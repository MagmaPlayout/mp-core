package playoutCore.scheduler;

import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import playoutCore.mvcp.MvcpCmdFactory;
import redis.clients.jedis.Jedis;

/**
 *
 * @author rombus
 */
public class SchedulerJobFactory implements JobFactory{
    private static final String FILTER_JOB = FilterJob.class.getName();
    private static final String PLSCHED_JOB = PlSchedJob.class.getName();
    
    private final Jedis publisher;
    private final String fscpChannel;
    private final Logger logger;
    private final MvcpCmdFactory factory;


    public SchedulerJobFactory(Jedis publisher, String channel, MvcpCmdFactory factory, Logger logger){
        this.publisher = publisher;
        this.fscpChannel = channel;
        this.logger = logger;
        this.factory = factory;
    }

    @Override
    public Job newJob(TriggerFiredBundle tfb, Scheduler schdlr) throws SchedulerException {
        String jobClass = tfb.getJobDetail().getJobClass().getName();

        if(jobClass.equals(FILTER_JOB)){
            return new FilterJob(publisher, fscpChannel, logger);
        }
        else if(jobClass.equals(PLSCHED_JOB)){
            return new PlSchedJob(publisher, fscpChannel, factory, logger);
        }

        throw new SchedulerException("Playout Core - Could not create "+jobClass+" schedule job.");
    }
}
