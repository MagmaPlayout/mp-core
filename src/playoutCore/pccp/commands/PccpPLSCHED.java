package playoutCore.pccp.commands;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import meltedBackend.responseParser.responses.ListResponse;
import static org.quartz.JobBuilder.newJob;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import static org.quartz.TriggerBuilder.newTrigger;
import playoutCore.dataStore.DataException;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.scheduler.FilterJob;
import playoutCore.scheduler.PlSchedJob;

/**
 *
 * @author rombus
 */
public class PccpPLSCHED extends PccpCommand {
    private static final int ID = 0;
    private static final int TIME = 1;
    private final Logger logger;
    private final Scheduler scheduler;

    public PccpPLSCHED(ArrayList<String> args, Scheduler scheduler, Logger logger){
        super(args);
        this.scheduler = scheduler;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        String id = args.get(ID);
        String timeStamp = args.get(TIME);
        ArrayList<Clip> clips;

        try {
            clips = store.getPlaylistClips(id);
        } catch (DataException e) {
            logger.log(Level.SEVERE, e.getMessage());
            return false;
        }

        //TODO hardcoded unit
        String unit = "U0";
        int lastClipId = 0;

        try {
            lastClipId = ((ListResponse)factory.getList(unit).exec()).getPlaylistLength();
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured while executing the LIST melted command. Cannot get playlist lenght!");
            return false;
        }
        
        timeStamp = timeStamp.split("\\.")[0]; // Removes .000Z of the end that's not compatible with LocalDateTime
        LocalDateTime start = LocalDateTime.parse(timeStamp);
        Duration playlistLength = Duration.ZERO;
        boolean first = true;
        for(Clip clip: clips){
            try {
                GenericResponse r = factory.getApnd(unit, clip).exec();

                if(!r.cmdOk()){
                    //TODO make this like a transaction, if one fails then remove the loaded clips of this failed playlist
                    logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
                            + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                    return false;
                }

                // Schedulles the movement of the cursor to the first added clip of this playlist
                if(first){
                    first = false;
                    int filter = Clip.NO_FILTER;
                    if(clip.filterId != Clip.NO_FILTER){
                        filter = clip.filterId;
                    }

                    //TODO HARDCODED TIMEZONE
                    Date d = Date.from(start.plusHours(3).toInstant(ZoneOffset.UTC));
                    SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();

                    try {
                        scheduler.scheduleJob(newJob(PlSchedJob.class)
                                .usingJobData("filterId", filter)
                                .usingJobData("firstClipId", lastClipId)
                                .build(), trigger);

                        logger.log(Level.INFO, "Playout Core - Playlist scheduled at {0}", d.toString());
                    } catch (SchedulerException ex) {
                        logger.log(Level.SEVERE, "Playout Core - Error while shcedulling playlist.");
                        return false;
                    }
                }
                else if(clip.filterId != Clip.NO_FILTER){
                    // TODO hardcoded timezone compensation
                    Date d = Date.from(start.plus(playlistLength).plusHours(3).toInstant(ZoneOffset.UTC));
                    SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
                    logger.log(Level.INFO, "Playout Core - Scheduling filter change at: {0}", d.toString());

                    try {
                        scheduler.scheduleJob(newJob(FilterJob.class).usingJobData("filterId", clip.filterId).build(), trigger);
                    } catch (SchedulerException ex) {
                        Logger.getLogger(MvcpCmdFactory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                playlistLength = playlistLength.plus(clip.len);
            } catch (MeltedCommandException ex) {
                logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the PLAYNOW PCCP command. Possibly by a misconfigured melt path configuration");
                return false;
            }
        }
        logger.log(Level.INFO, "Playout Core - Playlist length: {0}", playlistLength.toString());

        return true;
    }
}
