package playoutCore.producerConsumer;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedClient;
import playoutCore.commands.PccpCommand;
import playoutCore.dataStore.DataStore;

/**
 * This class consumes the PccpCommand queue calling execute() on each object.
 * 
 * @author rombus
 */
public class CommandsExecutor implements Runnable {
    private final MeltedCmdFactory meltedCmdFactory;
    private final DataStore store;
    private final ArrayBlockingQueue<PccpCommand> commandQueue;
    private final Logger logger;
    private boolean keepRunning;

    public CommandsExecutor(MeltedClient melted, DataStore store, ArrayBlockingQueue<PccpCommand> commandQueue, Logger logger){
        this.meltedCmdFactory = new MeltedCmdFactory(melted);
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
                cmd.execute(meltedCmdFactory, store);
            } catch (InterruptedException e) {
                keepRunning = false;
                logger.log(Level.INFO, "Playout Core - Shutting down CommandsExecutor thread.");
            }
        }
    }
}
