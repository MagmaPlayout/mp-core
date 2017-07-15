package playoutCore.producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPubSub;

/**
 * This class listens to the PCCP Redis channel for incomming commands and queues them for processing.
 * Runs on it's own thread.
 *
 * @author rombus
 */
public class CommandsListener implements Runnable{
    private final Logger logger;
    private final Jedis jedis;
    private final String pccpChannel;
    private final PccpFactory pccpFactory;
    private final ArrayBlockingQueue<PccpCommand> cmdQueue;

    public CommandsListener(Jedis subscriber, Jedis publisher, String pccpChannel,
            String fscpChannel, String pcrChannel, Scheduler scheduler, ArrayBlockingQueue cmdQueue, Logger logger){
        jedis = subscriber;
        pccpFactory = new PccpFactory(publisher, fscpChannel, pcrChannel, scheduler, logger);
        this.logger = logger;
        this.pccpChannel = pccpChannel;
        this.cmdQueue = cmdQueue;
    }
    
    @Override
    public void run() {
        jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String cmd) {
                PccpCommand pccpCmd = pccpFactory.getCommand(cmd);
                
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
