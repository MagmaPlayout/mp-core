package playoutCore.producerConsumer;

import com.google.gson.JsonObject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.commands.PccpGETPL;
import redis.clients.jedis.Jedis;

/**
 * This class consumes the PccpCommand queue calling execute() on each object.
 * 
 * @author rombus
 */
public class CommandsExecutor implements Runnable {
    private final MvcpCmdFactory meltedCmdFactory;
    private final ArrayBlockingQueue<PccpCommand> commandQueue;
    private final Logger logger;
    private boolean keepRunning;
    private Jedis publisher;
    private String pcrChannel;

    public CommandsExecutor(MvcpCmdFactory factory, Jedis publisher, String pcrChannel,
            ArrayBlockingQueue<PccpCommand> commandQueue, Logger logger){
        this.meltedCmdFactory = factory;
        this.commandQueue = commandQueue;
        this.logger = logger;
        this.keepRunning = true;
        this.publisher = publisher;
        this.pcrChannel = pcrChannel;
    }
    
    @Override
    public void run() {
        while(keepRunning){
            try {
                PccpCommand cmd = commandQueue.take();  // blocking
                
                if(cmd instanceof PccpGETPL){
                    JsonObject response = cmd.executeForResponse(meltedCmdFactory);
                    publisher.publish(pcrChannel, response.toString());
                }
                else {
                    cmd.execute(meltedCmdFactory);
                }
            } catch (InterruptedException e) {
                keepRunning = false;
                logger.log(Level.INFO, "Playout Core - Shutting down CommandsExecutor thread.");
            }
        }
    }
}
