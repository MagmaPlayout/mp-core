package playoutCore;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.telnetClient.MeltedTelnetClient;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.impl.StdSchedulerFactory;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.producerConsumer.CommandsExecutor;
import playoutCore.producerConsumer.CommandsListener;
import playoutCore.scheduler.SchedulerJobFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.exceptions.JedisConnectionException;

/**
 *
 * @author rombus
 */
public class PlayoutCore {
    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        PlayoutCore pc = new PlayoutCore();
        pc.run();
    }

    private void run(){
        // General config
        Logger logger = Logger.getLogger(PlayoutCore.class.getName());
        ConfigurationManager cfg = ConfigurationManager.getInstance();
        cfg.init(logger);

        // Commands Queue
        ArrayBlockingQueue<PccpCommand> commandsQueue = new ArrayBlockingQueue(100, true);

        /**
         * Connect to Redis PubSub server.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to connect to Redis Pub/Sub server...");
        Jedis redisPCCPSubscriber = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        Jedis redisPublisher = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        try{
            redisPCCPSubscriber.connect();
            redisPublisher.connect();
        }
        catch(JedisConnectionException e){
            //TODO handle reconnections
            logger.log(Level.SEVERE, "Playout Core - FATAL ERROR: Could not connect to the Redis server. Is it running?\nExiting...");
            System.exit(1);
        }
        

        /**
         * Creates a meltedTelnetClient instance, exits if it can't establish a connection.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to connect to Melted server...");
        MeltedTelnetClient melted = new MeltedTelnetClient();
        boolean connected = melted.connect(cfg.getMeltedHost(), cfg.getMeltedPort(), logger);
        // Handling reconnections
        while(!connected){
            connected = melted.reconnect(cfg.getMeltedReconnectionTries(), cfg.getMeltedReconnectionTimeout());
        }
        if(!connected){
            logger.log(Level.SEVERE, "Playout Core - FATAL ERROR: Could not connect to the Melted server. Is it running?\nExiting...");
            System.exit(1);
        }

        
        /**
         * Start's the command executor thread.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to start CommandsExecutor thread...");
        MvcpCmdFactory factory = new MvcpCmdFactory(melted, logger);
        CommandsExecutor executor = new CommandsExecutor(factory, redisPublisher, cfg.getRedisFscpChannel(), commandsQueue, logger);
        
        Thread executorThread = new Thread(executor);
        executorThread.start(); // TODO: handle reconnection


        /**
         * Start's the command listener thread.
         */
        Scheduler scheduler = null;
        try {
            scheduler = new StdSchedulerFactory().getScheduler();
            scheduler.setJobFactory(new SchedulerJobFactory(redisPublisher, cfg.getRedisFscpChannel(), factory, logger));
            scheduler.start();
        } catch (SchedulerException ex) {
            Logger.getLogger(PlayoutCore.class.getName()).log(Level.SEVERE, null, ex);
        }

        logger.log(Level.INFO, "Playout Core - Attempt to start CommandsListener thread...");
        CommandsListener listener = new CommandsListener(redisPCCPSubscriber, redisPublisher,
                cfg.getRedisPccpChannel(), cfg.getRedisFscpChannel(), cfg.getRedisPcrChannel(), scheduler, commandsQueue, logger);

        Thread listenerThread = new Thread(listener);
        try{
            listenerThread.start();
        }
        catch(JedisConnectionException e){
            // TODO handle reconnection
            logger.log(Level.SEVERE, "Playout Core - Connection to the Redis server lost. Is it running?");
            System.exit(1);
        }

        logger.log(Level.INFO, "Playout Core - Ready...\n\n");
    }
}
