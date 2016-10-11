package playoutCore.producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.commands.PccpCommand;
import playoutCore.commands.PccpFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * This class listens to the PCCP Redis channel for incomming commands and queues them for processing.
 * Runs in it's own thread.
 *
 * @author rombus
 */
public class CommandsListener implements Runnable{
    private final Logger logger;
    private final Jedis jedis;
    private final String pccpChannel;
    private final PccpFactory pccp;
    private final ArrayBlockingQueue<PccpCommand> cmdQueue;

    public CommandsListener(String host, int port, String pccpChannel, ArrayBlockingQueue cmdQueue, Logger logger){
        jedis = new Jedis(host, port);
        pccp = new PccpFactory();
        this.logger = logger;
        this.pccpChannel = pccpChannel;
        this.cmdQueue = cmdQueue;
    }

    public CommandsListener(Jedis redisPubSubServer, String pccpChannel, ArrayBlockingQueue cmdQueue, Logger logger){
        jedis = redisPubSubServer;
        pccp = new PccpFactory();
        this.logger = logger;
        this.pccpChannel = pccpChannel;
        this.cmdQueue = cmdQueue;
    }
    
    @Override
    public void run() {
        jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String cmd) {
                PccpCommand pccpCmd = pccp.getCommand(cmd);
                
                if(pccpCmd != null){
                    cmdQueue.add(pccpCmd);
                    logger.log(Level.INFO, "Playout Core - Queued command: {0}", cmd);
                }
                else {
                    logger.log(Level.WARNING, "Playout Core - Received an invalid command: {0}", cmd);
                }
            }
        }, pccpChannel);
    }
}
