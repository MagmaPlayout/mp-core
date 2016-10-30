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
public class FilterJob implements Job {

    private final Jedis publisher;
    private final String channel;
    private final Logger logger;

    public FilterJob(Jedis publisher, String channel, Logger logger) {
        this.publisher = publisher;
        this.channel = channel;
        this.logger = logger;
    }

    @Override
    public void execute(JobExecutionContext jec) throws JobExecutionException {
        int filter = jec.getJobDetail().getJobDataMap().getInt("filterId");

        publisher.publish(channel, "SETFILTER " + String.valueOf(filter)); // Tell the Filter Server to change it's filterId
        logger.log(Level.INFO, "Playout Core - Executing scheduled filter change to: {0}", String.valueOf(filter));

    }
}
