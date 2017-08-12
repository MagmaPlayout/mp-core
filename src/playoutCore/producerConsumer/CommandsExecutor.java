package playoutCore.producerConsumer;

import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.meltedProxy.MeltedProxy;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.commands.PccpAPND;
import playoutCore.pccp.commands.PccpGETPL;
import redis.clients.jedis.Jedis;

/**
 * This class consumes the PccpCommand queue calling execute() on each object.
 * Also handles the MeltedProxy for not overloading melted's playlist.
 * 
 * @author rombus
 */
public class CommandsExecutor implements Runnable {
    private final MvcpCmdFactory meltedCmdFactory;
    private final ArrayBlockingQueue<PccpCommand> commandQueue;
    private final Logger logger;
    private boolean keepRunning;
    private final Jedis publisher;
    private final String pcrChannel;
    private final MeltedProxy meltedProxy;

    public CommandsExecutor(MvcpCmdFactory factory, Jedis publisher, String pcrChannel,
            ArrayBlockingQueue<PccpCommand> commandQueue, int meltedPlaylistMaxDuration, int appenderWorkerFrq, Logger logger){
        this.meltedCmdFactory = factory;
        this.commandQueue = commandQueue;
        this.logger = logger;
        this.keepRunning = true;
        this.publisher = publisher;
        this.pcrChannel = pcrChannel; // Responses channel
        
        meltedProxy = new MeltedProxy(meltedPlaylistMaxDuration, factory, appenderWorkerFrq, logger);
    }
    
    @Override
    public void run() {
        while(keepRunning){
            try {
                PccpCommand cmd = commandQueue.take();  // blocking

                if(cmd instanceof PccpAPND){
                    // PccpAPND commands are the only PCCP commands that add medias to melted.
                    // In the future, any other command added that adds medias to melted
                    // should be taken into account here.
                    // MeltedProxy makes sure that melted's playlist doesn't get overloaded
                    meltedProxy.execute((PccpAPND)cmd);
                }
                else {
                    if(cmd instanceof PccpGETPL){ //TODO: make this distinction more abstract
                        JsonObject response = cmd.executeForResponse(meltedCmdFactory);
                        publisher.publish(pcrChannel, response.toString());
                    }
                    else {
                        cmd.execute(meltedCmdFactory);
                    }
                }
            } catch (InterruptedException e) {
                keepRunning = false;
                logger.log(Level.INFO, "Playout Core - Shutting down CommandsExecutor thread.");
            }
        }
    }

    /**
     * This method provides a way to add PccpCommands to the queue internally, that is, without using the PCCP redis channel.
     * 
     * @param cmds List of commands to queue
     */
    public void addPccpCmdsToExecute(ArrayList<PccpCommand> cmds){
        for(PccpCommand cmd: cmds){
            commandQueue.add(cmd);
        }
    }

    public void tellMeltedProxyToTryNow(){
        meltedProxy.tryToExecuteNow();
    }
}
