package playoutCore.producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import org.quartz.Scheduler;
import playoutCore.melted.status.StatusCmdProcessor;
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
    private final StatusCmdProcessor statusProcessor;
    private final String mstaChannel;

    public CommandsListener(Jedis subscriber, Jedis publisher, String pccpChannel,
            String fscpChannel, String pcrChannel, String mstaChannel, Scheduler scheduler, ArrayBlockingQueue cmdQueue, 
            PccpFactory pccpFactory, StatusCmdProcessor statusProcessor, Logger logger){
        jedis = subscriber;
        this.pccpFactory = pccpFactory;
        this.logger = logger;
        this.pccpChannel = pccpChannel;
        this.cmdQueue = cmdQueue;
        this.mstaChannel = mstaChannel;
        this.statusProcessor = statusProcessor;
    }
    
    @Override
    public void run() {
        jedis.subscribe(new JedisPubSub() {
            @Override
            public void onMessage(String channel, String cmd) {
                PccpCommand pccpCmd = pccpFactory.getCommand(cmd);
                
                if(channel.equals(mstaChannel)){
                    System.out.println("ENGANCHO COMMAND");
                    statusProcessor.processCmd(cmd);
                } 
                else {
                    if(pccpCmd != null){
                        cmdQueue.add(pccpCmd);
                        logger.log(Level.INFO, "Playout Core - Queued command: {0}", cmd);
                    }
                    else {
                        logger.log(Level.WARNING, "Playout Core - Received an invalid command: {0}", cmd);
                    }
                }
            }
        }, pccpChannel);
    }
}
