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
public class PlSchedJobFactory implements JobFactory{
    private final Jedis publisher;
    private final String channel;
    private final Logger logger;
    private final MvcpCmdFactory factory;

    public PlSchedJobFactory(Jedis publisher, String channel, MvcpCmdFactory factory, Logger logger){
        this.publisher = publisher;
        this.channel = channel;
        this.logger = logger;
        this.factory = factory;
    }

    @Override
    public Job newJob(TriggerFiredBundle tfb, Scheduler schdlr) throws SchedulerException {
        return new PlSchedJob(publisher, channel, factory, logger);
    }
}
