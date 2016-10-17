package playoutCore.pccp.commands;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.ListResponse;
import static org.quartz.JobBuilder.newJob;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.SimpleTrigger;
import static org.quartz.TriggerBuilder.newTrigger;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.scheduler.FilterJob;
import playoutCore.scheduler.SchedulerJobFactory;
import redis.clients.jedis.Jedis;

/**
 *  This command loads all clips of a given playlist into melted and moves the cursor to the first clip frame.
 * 
 * @author rombus
 */
public class PccpPLAYNOW extends PccpCommand {
    private static final int ID = 0;
    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;
    private LocalDateTime curPlaylistTime;

    public PccpPLAYNOW(ArrayList<String> args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.fscpChannel = fscpChannel;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        String id = args.get(ID);
        ArrayList<Clip> clips = store.getPlaylistClips(id);
        
        // Couldn't parse clips, nothing more to do here
        if(clips == null){
            return false;
        }

        //TODO hardcoded unit
        String unit = "U0";
        int lastClipId = 0;
        
        try {
            lastClipId = ((ListResponse)factory.getList(unit).exec()).getPlaylistLength()-1;
        } catch (MeltedCommandException ex) {
            //TODO handle error. Coudln't get playlistLength
            Logger.getLogger(PccpPLAYNOW.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }

        LocalDateTime start = null;
        Duration playlistLength = Duration.ZERO;
        boolean first = true;
        for(Clip clip: clips){
            try {
                factory.getApnd(unit, clip, first).exec();
                
                // Move cursor to the first added clip of this playlist
                if(first){
                    first = false;                    
                    factory.getGoto(unit, 0, lastClipId).exec();
                    factory.getPlay(unit).exec();
                    start = LocalDateTime.now();
                    
                    if(clip.filterId != Clip.NO_FILTER){
                        publisher.publish(fscpChannel, "SETFILTER "+String.valueOf(clip.filterId)); // Tell the Filter Server to change it's filterId
                        logger.log(Level.INFO, "Playout Core - Setting filter server filter id:{0}", clip.filterId);
                    }
                }
                else if(clip.filterId != Clip.NO_FILTER){
                    // TODO schedule filterId change
                    Date d = Date.from(start.plus(playlistLength).plusHours(3).toInstant(ZoneOffset.UTC));
                    SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
                    logger.log(Level.INFO, "Playout Core - Scheduling filter change at: {0}", d.toString());

                    try {
                        scheduler.setJobFactory(new SchedulerJobFactory(publisher, fscpChannel, clip.filterId, logger));
                        scheduler.scheduleJob(newJob(FilterJob.class).build(), trigger);
                    } catch (SchedulerException ex) {
                        Logger.getLogger(MvcpCmdFactory.class.getName()).log(Level.SEVERE, null, ex);
                    }
                }
                playlistLength = playlistLength.plus(clip.len);
            } catch (MeltedCommandException ex) {
                //TODO handle errors
                Logger.getLogger(PccpPLAYNOW.class.getName()).log(Level.SEVERE, " PUDRO LA TORTA ");

                return false;
            }
        }
        
        return true;
    }
}
