package playoutCore.scheduler;

import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.spi.JobFactory;
import org.quartz.spi.TriggerFiredBundle;
import redis.clients.jedis.Jedis;

/**
 *
 * @author rombus
 */
public class SchedulerJobFactory implements JobFactory{
    private final Jedis publisher;
    private final String channel;
    private final int filter;
    private final Logger logger;

    public SchedulerJobFactory(Jedis publisher, String channel, int filter, Logger logger){
        this.publisher = publisher;
        this.channel = channel;
        this.logger = logger;
        this.filter = filter;
    }

    @Override
    public Job newJob(TriggerFiredBundle tfb, Scheduler schdlr) throws SchedulerException {
        return new FilterJob(publisher, channel, filter, logger);
    }
}
