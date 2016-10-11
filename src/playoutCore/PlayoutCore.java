package playoutCore;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.ListResponse;
import meltedBackend.telnetClient.MeltedTelnetClient;
import playoutCore.commands.PccpCommand;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.RedisStore;
import playoutCore.producerConsumer.CommandsExecutor;
import playoutCore.producerConsumer.CommandsListener;
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
        new PlayoutCore();
    }

    public PlayoutCore(){
        // General config
        Logger logger = Logger.getLogger(PlayoutCore.class.getName());
        ConfigurationManager cfg = new ConfigurationManager(logger);

        // Commands Queue
        ArrayBlockingQueue<PccpCommand> commandsQueue = new ArrayBlockingQueue(100, true);

        /**
         * Connect to Redis PubSub server.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to connect to Redis Pub/Sub server...");
        Jedis redisPubSubServer = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        try{
            redisPubSubServer.connect();
        }
        catch(JedisConnectionException e){
            //TODO handle reconnections
            logger.log(Level.SEVERE, "Playout Core - FATAL ERROR: Could not connect to the Redis server. Is it running?\nExiting...");
            System.exit(1);
        }
        
        
        /**
         * Connect to Redis Store server.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to connect to Redis store server...");
        Jedis redisStoreServer = new Jedis(cfg.getRedisHost(), cfg.getRedisPort(), cfg.getRedisReconnectionTimeout());
        DataStore store = new RedisStore(redisStoreServer);
        

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
        CommandsExecutor executor = new CommandsExecutor(melted, store, commandsQueue, logger);
        Thread executorThread = new Thread(executor);
        executorThread.start(); // TODO: handle reconnection


        /**
         * Start's the command listener thread.
         */
        logger.log(Level.INFO, "Playout Core - Attempt to start CommandsListener thread...");
        CommandsListener listener = new CommandsListener(redisPubSubServer, cfg.getRedisPccpChannel(), commandsQueue, logger);
        Thread listenerThread = new Thread(listener);
        try{
            listenerThread.start();
        }
        catch(JedisConnectionException e){
            // TODO handle reconnection
            logger.log(Level.SEVERE, "Playout Core - Connection to the Redis server lost. Is it running?");
            System.exit(1);
        }


        /**
         * General configurations.
         */
        //TODO esto está provisorio        
        MeltedCmdFactory f = new MeltedCmdFactory(melted);
        try {
            ListResponse r = ((ListResponse)f.getNewListCmd("U0").exec());
            int l = r.getPlaylistLength();
            redisStoreServer.set("U0", String.valueOf(l));
        } catch (MeltedCommandException ex) {
            Logger.getLogger(PlayoutCore.class.getName()).log(Level.SEVERE, null, ex);
        }
        

        logger.log(Level.INFO, "Playout Core - Ready...");
    }
}
