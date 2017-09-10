package playoutCore.calendar;

import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
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
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;
import playoutCore.scheduler.GotoSchedJob;

/**
 * This class executes the run() method when the calendar sends a "SAVE" trigger a.k.a. CALCHANGE command.
 * @author rombus
 */
public class CalendarMode implements Runnable{
    private static final String UNIT = "U0";
    private final Logger logger;
    private final PccpFactory pccpFactory;
    private final MvcpCmdFactory mvcpFactory;
    private final MPPlayoutCalendarApi api;
    private final SpacerGenerator spacerGen;
    private final CommandsExecutor cmdExecutor;
    private final Scheduler scheduler;

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
        ArrayList<PccpCommand> commands = new ArrayList<>(); // Here is where all the commands will be, the APND commands and any other needed
        ArrayList<Occurrence> occurrences = api.getAllOccurrences();    // This get's the playlist from the DB

        // Takes the occurrences list and adds the spacers in the right places (if needed) [[BUT it doesn't add anything before the first occurrence]]
        occurrences = spacerGen.generateNeededSpacers(occurrences);

        ZonedDateTime calendarStarts = occurrences.get(0).startDateTime;
        ZonedDateTime defaultMediasEnds = cmdExecutor.getCurClipEndTime().atZone(calendarStarts.getZone());

        //{ TODO: hago esto como bugfix temporal para acomodar el timezone que en la BD se está guardando mal. ISSUE: https://github.com/MagmaPlayout/mp-playout-api/issues/2
            calendarStarts = bugFix(calendarStarts);
            defaultMediasEnds = bugFix(defaultMediasEnds);
            System.out.println("START DATE TIME QUE TENGO EN LA OCCURRENCE: "+occurrences.get(0).startDateTime.toString());
        //} FIN TODO


        // +++++++++++++++++++++++++++++
        // TODO: implement modeSwitching
        // +++++++++++++++++++++++++++++
        boolean modeSwitch = false; // if switching from live mode to calendar mode this must be true
        int startingFrame = removeOldClips(occurrences, modeSwitch);

        
        boolean scheduleChange = false;
        if(defaultMediasEnds.isBefore(calendarStarts) || defaultMediasEnds.isEqual(calendarStarts)){ // TODO: agregar tolerancia
            logger.log(Level.INFO, "Playout Core - adding spacer before calendar playlist");
            // Creates a spacer from the end of the cur PL up to the first clip of the calendar
            Occurrence first = spacerGen.generateImageSpacer(calendarStarts, defaultMediasEnds);

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
                Date d = Date.from(calendarStarts.toInstant());
                SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
                logger.log(Level.INFO, "Playout Core - Scheduling goto at: {0}", d.toString());

                // Obtains the index of the media that will be scheduled to GOTO to
                ListResponse list = (ListResponse) mvcpFactory.getList(UNIT).exec();
                int lplclidx = list.getLastPlClipIndex();
                int firstCalClip = lplclidx +1; //+ occurrences.size();
                logger.log(Level.INFO, "DEBUG - list.getLastPlClipIndex: "+lplclidx+", firstCalClip: "+firstCalClip);


                try {
                    scheduler.scheduleJob(
                            newJob(GotoSchedJob.class)
                                    .usingJobData("clipToGoTo", firstCalClip)
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
            commands.add(pccpFactory.getAPNDFromOccurrence(cur, curPos));
            curPos++;
        }

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
        int framesPassed = -1;
        ZonedDateTime now = ZonedDateTime.now();

        for(Iterator<Occurrence> iterator = playList.iterator(); iterator.hasNext(); ){
            Occurrence cur = iterator.next();
            
            // TODO: hago esto como bugfix temporal para acomodar el timezone que en la BD se está guardando mal. ISSUE: https://github.com/MagmaPlayout/mp-playout-api/issues/2
            ZonedDateTime startDateTime = bugFix(cur.startDateTime);
            // fin bugfix;

            if(startDateTime.isBefore(now)){
                if(cur.endDateTime.isAfter(now)){
                    long secondsPassed = startDateTime.until(now, ChronoUnit.SECONDS);

                    if(tolerateInBetween){
                        float fps = cur.frameRate;
                        framesPassed = (int)(secondsPassed / fps); // This clip should be played starting on this frame

                        logger.log(Level.INFO, "CalendarMode - first clip will start playing at "+framesPassed+" frame (not 0). ");
                    }
                    else {
                        // generate spacer to fill the gap
                        Occurrence spacer = spacerGen.generateImageSpacer(null, null, Duration.of(secondsPassed, ChronoUnit.SECONDS));
                        
                    }


                    break; // We found the first clip of the playlist, so we're out of this loop
                }
                
                logger.log(Level.INFO, "CalendarMode - removing a clip that's in the past and can't be played.");
                iterator.remove();
            }
            else {
                // Occurrences are ordered so the first that's not before NOW means that we're done with the loop
                break;
            }
        }
        
        return framesPassed;
    }


    /**
     * TODO: hago esto como bugfix temporal para acomodar el timezone que en la BD se está guardando mal. ISSUE: https://github.com/MagmaPlayout/mp-playout-api/issues/2
     * @param zdt
     * @return
     */
    private ZonedDateTime bugFix(ZonedDateTime zdt){
        return zdt.plus(3, ChronoUnit.HOURS);
    }
}
