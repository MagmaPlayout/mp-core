package playoutCore.calendar;

import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Date;
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
            calendarStarts = calendarStarts.plus(3, ChronoUnit.HOURS);
            defaultMediasEnds = defaultMediasEnds.plus(3, ChronoUnit.HOURS);
            System.out.println("START DATE TIME QUE TENGO EN LA OCCURRENCE: "+occurrences.get(0).startDateTime.toString());
        //} FIN TODO


        /**
         * TODO (acá):
         * Filtrar la lista de "occurrences" para que los clips que estén al principio con horario anterior a NOW desaparezcan
         * o se mueva al frame exacto, según si estás en calendar mode o venís de un switch de modo.
         */

        
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


        // TODO: (steps)
        // clear melted's playlist
        // take into account the actual time and the startDateTime of the first occurrence. Generate a spacer with that info and add it first of all
        // add every other occurrence

        cleanProxyAndMeltedLists(); // Also cancels any scheduled goto
        
        if(scheduleChange){
            try{
                // Prepares a scheduled GOTO command that will go to the first calendar clip
                Date d = Date.from(calendarStarts.toInstant());
                SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
                logger.log(Level.INFO, "Playout Core - Scheduling goto at: {0}", d.toString());


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
}
