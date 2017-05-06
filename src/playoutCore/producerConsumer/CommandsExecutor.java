package playoutCore.producerConsumer;

import com.google.gson.JsonObject;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.dataStore.DataStore;
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
    private final DataStore store;
    private final ArrayBlockingQueue<PccpCommand> commandQueue;
    private final Logger logger;
    private boolean keepRunning;

    public CommandsExecutor(MvcpCmdFactory factory, DataStore store, Jedis publisher,
            String fscpChannel, ArrayBlockingQueue<PccpCommand> commandQueue, Logger logger){
        this.meltedCmdFactory = factory;
        this.store = store;
        this.commandQueue = commandQueue;
        this.logger = logger;
        this.keepRunning = true;
    }
    
    @Override
    public void run() {
        while(keepRunning){
            try {
                PccpCommand cmd = commandQueue.take();  // blocking
                
                if(cmd instanceof PccpGETPL){
                    JsonObject response = cmd.executeForResponse(meltedCmdFactory, store);
                    //TODO mandarle al frontend la response
                }
                else {
                    cmd.execute(meltedCmdFactory, store);
                }
            } catch (InterruptedException e) {
                keepRunning = false;
                logger.log(Level.INFO, "Playout Core - Shutting down CommandsExecutor thread.");
            }
        }
    }
}
