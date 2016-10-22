package playoutCore.scheduler;

import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import redis.clients.jedis.Jedis;

/**
 *
 * @author rombus
 */
public class FilterJob implements Job{
    private final Jedis publisher;
    private final int filter;
    private final String channel;
    private final Logger logger;

    public FilterJob(Jedis publisher, String channel, int filter, Logger logger){
        this.publisher = publisher;
        this.filter = filter;
        this.channel = channel;
        this.logger = logger;
    }

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        publisher.publish(channel, "SETFILTER "+String.valueOf(filter)); // Tell the Filter Server to change it's filterId
        logger.log(Level.INFO, "Playout Core - Executing scheduled filter change.");
    }
}
