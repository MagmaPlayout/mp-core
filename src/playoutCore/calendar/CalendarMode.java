package playoutCore.calendar;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.ListIterator;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.ListResponse;
import static org.quartz.JobBuilder.newJob;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import static org.quartz.TriggerBuilder.newTrigger;
import playoutCore.calendar.dataStore.MPPlayoutCalendarApi;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;
import playoutCore.scheduler.GotoSchedJob;

/**
 * This class executes the run() method when the calendar sends a CALCHANGE PCCP command.
 * @author rombus
 */
public class CalendarMode implements Runnable{
    private static final int NO_MODE_SWITCHING = -1; // Flag that indicates that a mode switching operation is not taking place
    private static final String UNIT = "U0";
    private final Logger logger;
    private final PccpFactory pccpFactory;
    private final MvcpCmdFactory mvcpFactory;
    private final MPPlayoutCalendarApi api;
    private final SpacerGenerator spacerGen;
    private final CommandsExecutor cmdExecutor;
    private final Scheduler scheduler;
    private boolean commingFromLiveMode = false;

    public CalendarMode(MPPlayoutCalendarApi api, MvcpCmdFactory mvcpFactory, PccpFactory pccpFactory, CommandsExecutor cmdExecutor, Scheduler scheduler, Logger logger) {
        this.logger = logger;
        this.api = api;
        this.pccpFactory = pccpFactory;
        this.mvcpFactory = mvcpFactory;
        this.cmdExecutor = cmdExecutor;
        this.scheduler = scheduler;
        spacerGen = SpacerGenerator.getInstance();
    }

    @Override
    public void run() {
        cmdExecutor.blockMelted(true);
        ArrayList<PccpCommand> commands = new ArrayList<>(); // Here is where all the commands will be, the APND commands and any other needed
        ArrayList<Occurrence> occurrences = api.getAllOccurrences();    // This get's the playlist from the DB

        int startingFrame = removeOldClips(occurrences, commingFromLiveMode);
        if(commingFromLiveMode) commingFromLiveMode = false;

        // Takes the occurrences list and adds the spacers in the right places (if needed) [[BUT it doesn't add anything before the first occurrence]]
        occurrences = spacerGen.generateNeededSpacers(occurrences);
        ZonedDateTime calendarStarts;
        if(occurrences.size() > 0){
            calendarStarts = occurrences.get(0).startDateTime;

            // If it's a modeswitch then calculate starting time with starting frame
            calendarStarts = (startingFrame == NO_MODE_SWITCHING)? calendarStarts: calendarStarts.plus(startingFrame/occurrences.get(0).frameRate, ChronoUnit.SECONDS);
        }
        else {
            calendarStarts = ZonedDateTime.now();
        }

        ZonedDateTime curMediaEndTime = cmdExecutor.getCurClipEndTime().atZone(ZoneId.systemDefault());
        boolean scheduleChange = false;
        
        logger.log(Level.INFO, "Playout Core --- "
                +" Clip: "+ cmdExecutor.getCurClipPath()
                +", curMediaEndTime: "+ curMediaEndTime.toString()
                +", curMediaStartTime: "+ cmdExecutor.getCurClipStartTime().toString()
                +"; calendarStarts: "+calendarStarts.toString()
        );
        
        if(curMediaEndTime.isBefore(calendarStarts) || curMediaEndTime.isEqual(calendarStarts.plus(1,ChronoUnit.SECONDS))){
            logger.log(Level.INFO, "Playout Core - adding spacer before calendar playlist");
            // Creates a spacer from the end of the cur PL up to the first clip of the calendar
            Occurrence first = spacerGen.generateImageSpacer(calendarStarts, curMediaEndTime);

            if(first != null){
                // adds the spacer as the first occurrence to the occurrences that will be added
                occurrences.add(0, first);
            }
        }
        else {
            scheduleChange = true; // Mark this flag so that I call cleanProxyAndMeltedLists() before scheduling anything
        }

        cleanProxyAndMeltedLists(); // Also cancels any scheduled goto

        if(scheduleChange){
            try{
                // Prepares a scheduled GOTO command that will go to the first calendar clip
                Date d = Date.from(calendarStarts.plus(2000,ChronoUnit.MILLIS).toInstant()); //TODO: le pongo un changui acá para workaroundear un bug. hacer bien
                SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
                logger.log(Level.INFO, "Playout Core - Scheduling goto at: {0}", d.toString());

                // Obtains the index of the media that will be scheduled to GOTO to
                ListResponse list = (ListResponse) mvcpFactory.getList(UNIT).exec();
                int lplclidx = list.getLastPlClipIndex();
                int firstCalClip = lplclidx +1; //+ occurrences.size();
                startingFrame = (startingFrame == NO_MODE_SWITCHING)? 0:startingFrame; // If I don't want to goto a specific frame then I got to the first frame i.e. 0
                logger.log(Level.INFO, "DEBUG - list.getLastPlClipIndex: "+lplclidx+", firstCalClip: "+firstCalClip+ ", frame: "+startingFrame);

                try {
                    scheduler.scheduleJob(
                            newJob(GotoSchedJob.class)
                                    .usingJobData("clipToGoTo", firstCalClip)
                                    .usingJobData("frameToGoTo", startingFrame)
                                    .withIdentity("calSchedJob")
                                    .build()
                            , trigger
                    );
                } catch (SchedulerException ex) {
                    logger.log(Level.SEVERE, "Playout Core - An exception occured while trying to execute a scheduled GOTO.");
                }
            }catch (MeltedCommandException e){
                logger.log(Level.SEVERE, "Playout Core - An exception occured while trying to execute a LIST MVCP command.");
            }
        }

        int curPos = 1;
        for(Occurrence cur:occurrences){
            commands.add(pccpFactory.getAPNDFromOccurrence(cur, curPos++));
        }

        cmdExecutor.blockMelted(false);
        cmdExecutor.addPccpCmdsToExecute(commands);

        logger.log(Level.INFO, "Playout Core - CalendarMode thread finished");
    }


    /**
     * Cleans all upcoming clips.
     */
    private void cleanProxyAndMeltedLists(){
        try {
            // Cancel scheduler
            scheduler.deleteJob(JobKey.jobKey("calSchedJob"));

            // Empty MeltedProxy and Melted lists
            cmdExecutor.cleanProxyAndMeltedLists();
        } catch (SchedulerException ex) {
            Logger.getLogger(CalendarMode.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    /**
     * Removes all clips that have a starting time before now.
     * If you are switching from live mode to calendar mode you should send the tolerateInBetween flag as true.
     * That way if a clip has a starting time before now, but an ending time after now you can move to the specific
     * frame of that clip.
     *
     * @param playList the playlist taken from the calendar
     * @param tolerateInBetween if true it wont remove clips that have an ending time after now. If false it will remove all clips that have a starting time before now
     * @return the frame to move to if tolerateInBetween is true, otherwhise -1
     */
    private int removeOldClips(ArrayList<Occurrence> playList, boolean tolerateInBetween){
        int framesPassed = NO_MODE_SWITCHING;

        for(ListIterator<Occurrence> iterator = playList.listIterator(); iterator.hasNext(); ){
            Occurrence cur = iterator.next();
            ZonedDateTime startDateTime = cur.startDateTime;
            ZonedDateTime endDateTime = cur.endDateTime;

            ZonedDateTime now = ZonedDateTime.now();
            if(startDateTime.isBefore(now)){
                if(endDateTime.isAfter(now)){
                    long secondsPassed = startDateTime.until(now, ChronoUnit.SECONDS);
                    float fps = cur.frameRate;

                    if(tolerateInBetween){
                        framesPassed = (int) Math.round(secondsPassed * fps); // This clip should be played starting on this frame
                        logger.log(Level.INFO, "CalendarMode - first clip will start playing at "+framesPassed+" frame (not 0). ");
                    }
                    else {
                        iterator.remove();
                        framesPassed = NO_MODE_SWITCHING; // If tolerateInBetween is false then I'm not on a mode switch
                    }

                    break; // We found the first clip of the playlist, so we're out of this loop
                }
                
                logger.log(Level.INFO, "CalendarMode - removing a clip that''s in the past and can''t be played. Path: {0}, startDateTime: {1}", new Object[]{cur.path, cur.startDateTime});
                iterator.remove();
            }
            else {
                // Occurrences are ordered so the first that's not before NOW means that we're done with the loop
                break;
            }
        }
        
        return framesPassed;
    }

    public void switchToLiveMode(ArrayList<Clip> clips){
        cmdExecutor.blockMelted(true);
        cleanProxyAndMeltedLists();
        commingFromLiveMode = true; // I set this flag in advance here

        ArrayList<PccpCommand> commands = new ArrayList<>();
        for(Clip cur:clips){
            commands.add(pccpFactory.getAPNDFromClip(cur));
        }

        
        if(!commands.isEmpty()){
            cmdExecutor.addPccpCmdsToExecute(commands);
        }
        cmdExecutor.blockMelted(false);
    }
}
