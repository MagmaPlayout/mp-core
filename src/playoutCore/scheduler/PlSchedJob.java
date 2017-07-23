package playoutCore.scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import playoutCore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import redis.clients.jedis.Jedis;

/**
 *
 * @author rombus
 */
public class PlSchedJob implements Job{
    private final Jedis publisher;
    private final String channel;
    private final Logger logger;
    private final MvcpCmdFactory factory;

    public PlSchedJob(Jedis publisher, String channel, MvcpCmdFactory factory, Logger logger){
        this.publisher = publisher;
        this.channel = channel;
        this.logger = logger;
        this.factory = factory;
    }

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        //TODO hardcoded unit
        String unit = "U0";

        JobDataMap data = jec.getJobDetail().getJobDataMap();
        int firstClipId = data.getInt("firstClipId");
        int filter = data.getInt("filterId");

        try {
            GenericResponse r = factory.getGoto(unit, 0, firstClipId).exec();
            if(!r.cmdOk()){
                logger.log(Level.SEVERE, "Playout Core - Scheduler - Could not move playing cursor to {0} position. Error {1}", new Object[]{firstClipId, r.getStatus()});
                return;
            }

            r = factory.getPlay(unit).exec();
            if(!r.cmdOk()){
                logger.log(Level.SEVERE, "Playout Core - Scheduler - Cannot play appended clip. Error {0}", r.getStatus());
                return;
            }

            logger.log(Level.INFO, "Playout Core - Scheduler - Playing schedulled playlist.");
        } catch (MeltedCommandException ex) {
            logger.log(Level.INFO, "Playout Core - error while executing the PLSCHED commands.");
            return;
        }

        if(filter != Clip.NO_FILTER){
            publisher.publish(channel, "SETFILTER "+String.valueOf(filter)); // Tell the Filter Server to change it's filterId
            logger.log(Level.INFO, "Playout Core - Setting filter server filter id:{0}", filter);
        }
    }
}