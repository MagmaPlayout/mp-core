package playoutCore.scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import playoutCore.mvcp.MvcpCmdFactory;
import redis.clients.jedis.Jedis;

/**
 *
 * @author rombus
 */
public class GotoSchedJob implements Job{
    private final Jedis publisher;
    private final String channel;
    private final Logger logger;
    private final MvcpCmdFactory factory;

    public GotoSchedJob(Jedis publisher, String channel, MvcpCmdFactory factory, Logger logger){
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
        int firstClipId = data.getInt("clipToGoTo");

        try {
            GenericResponse r = factory.getGoto(unit, 0, firstClipId).exec();
            if(!r.cmdOk()){
                logger.log(Level.SEVERE, "Playout Core - Scheduler - Could not move playing cursor to {0} position. Error {1}", new Object[]{firstClipId, r.getStatus()});
                return;
            }

            logger.log(Level.INFO, "Playout Core - Scheduler - Playing schedulled playlist.");
        } catch (MeltedCommandException ex) {
            logger.log(Level.INFO, "Playout Core - error while executing the PLSCHED commands.");
            return;
        }
    }
}