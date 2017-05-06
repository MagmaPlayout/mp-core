package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.dataStore.DataStore;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command loads all clips of a given playlist into melted and moves the cursor to the first loaded clip frame.
 * 
 * @author rombus
 */
public class PccpPLPLAYNOW extends PccpCommand {
    private static final int ID = 0;
    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpPLPLAYNOW(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.fscpChannel = fscpChannel;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        logger.log(Level.SEVERE, "Playout Core - PLPLAYNOW command not implemented yet!");
        return false;
        // TODO: call getClipFromJsonArg(xx) inside a loop 
//        String id = args.get(ID);
//        ArrayList<Clip> clips;
//
//        try {
//            clips = store.getPlaylistClips(id);
//        } catch (DataException e) {
//            logger.log(Level.SEVERE, e.getMessage());
//            return false;
//        }
//
//        //TODO hardcoded unit
//        String unit = "U0";
//        int lastClipId = 0;
//
//        try {
//            lastClipId = ((ListResponse)factory.getList(unit).exec()).getPlaylistLength()-1;
//        } catch (MeltedCommandException ex) {
//            logger.log(Level.SEVERE, "Playout Core - An error occured while executing the LIST melted command. Cannot get playlist lenght!");
//            return false;
//        }
//
//        LocalDateTime start = null;
//        Duration playlistLength = Duration.ZERO;
//        boolean first = true;
//        for(Clip clip: clips){
//            try {
//                GenericResponse r = factory.getApnd(unit, clip).exec();
//
//                if(!r.cmdOk()){
//                    logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
//                            + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
//                    return false;
//                }
//
//                // Move cursor to the first added clip of this playlist
//                if(first){
//                    first = false;
//                    factory.getGoto(unit, 0, lastClipId).exec();
//                    factory.getPlay(unit).exec();
//                    start = LocalDateTime.now();
//
//                    if(clip.filterId != Clip.NO_FILTER){
//                        //TODO remove this publish from here and centralize it on a FSCP class.
//                        publisher.publish(fscpChannel, "SETFILTER "+String.valueOf(clip.filterId)); // Tell the Filter Server to change it's filterId
//                        logger.log(Level.INFO, "Playout Core - Setting filter server filter id:{0}", clip.filterId);
//                    }
//                }
//                else if(clip.filterId != Clip.NO_FILTER){
//                    // TODO hardcoded timezone compensation
//                    logger.log(Level.SEVERE, "TODO - HARDCODED TIMEZONE INFORMATION!!!");
//                    Date d = Date.from(start.plus(playlistLength).plusHours(3).plusSeconds(1).toInstant(ZoneOffset.UTC));
//                    SimpleTrigger trigger = (SimpleTrigger) newTrigger().startAt(d).build();
//                    logger.log(Level.INFO, "Playout Core - Scheduling filter change at: {0}", d.toString());
//
//                    try {
//                        scheduler.scheduleJob(newJob(FilterJob.class).usingJobData("filterId", clip.filterId).build(), trigger);
//                    } catch (SchedulerException ex) {
//                        Logger.getLogger(MvcpCmdFactory.class.getName()).log(Level.SEVERE, null, ex);
//                    }
//                }
//                playlistLength = playlistLength.plus(clip.len);
//                logger.log(Level.INFO, "Playout Core - Playlist length: {0}", playlistLength.toString());
//            } catch (MeltedCommandException ex) {
//                logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the PLAYNOW PCCP command. Possibly by a misconfigured melt path configuration");
//                return false;
//            }
//        }
//
//        return true;
    }
}
