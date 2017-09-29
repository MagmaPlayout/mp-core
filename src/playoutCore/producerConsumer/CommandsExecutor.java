package playoutCore.producerConsumer;

import com.google.gson.JsonObject;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.meltedProxy.MeltedProxy;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
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
    private final PccpFactory pccpFactory;

    public CommandsExecutor(MvcpCmdFactory factory, PccpFactory pccpFactory, Jedis publisher, String pcrChannel,
            ArrayBlockingQueue<PccpCommand> commandQueue, int meltedPlaylistMaxDuration, int appenderWorkerFrq, Logger logger){
        this.meltedCmdFactory = factory;
        this.commandQueue = commandQueue;
        this.logger = logger;
        this.keepRunning = true;
        this.publisher = publisher;
        this.pcrChannel = pcrChannel; // Responses channel
        this.pccpFactory = pccpFactory;
        
        meltedProxy = new MeltedProxy(meltedPlaylistMaxDuration, factory, pccpFactory, appenderWorkerFrq, logger);
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
        // I make sure the MeltedProxy worker won't do anything until I finish loading the PccpCommands onto the commandQueue by blocking it
        interruptMeltedProxyWorker();
        meltedProxy.blockQueue(true);
        for(PccpCommand cmd: cmds){
            commandQueue.add(cmd);
        }
        // Once I finish blocking the queue I tryToExecute all it's commands right away
        meltedProxy.blockQueue(false);
        meltedProxy.tryToExecuteNow();
    }

    public void interruptMeltedProxyWorker(){
        meltedProxy.interruptAppenderThread();
    }

    /**
     * This method provides a way to add PccpCommands to the queue internally, that is, without using the PCCP redis channel.
     * The startTime argument allows to calculate the plEndTimestamp from that point on (used when scheduling goto commands)
     *
     * @param cmds List of commands to queue
     * @param startTime starting time of the PL that will be added
     */
    public void addPccpCmdsToExecute(ArrayList<PccpCommand> cmds, ZonedDateTime startTime){
        meltedProxy.setScheduledStartingTime(startTime);
        for(PccpCommand cmd: cmds){
            commandQueue.add(cmd);
        }
    }

    /**
     * This method provides a way to add a single PccpCommands to the queue internally, that is, without using the PCCP redis channel.
     *
     * @param cmd command to queue
     */
    public void addPccpCmdToExecute(PccpCommand cmd){
        commandQueue.add(cmd);
    }

    public void tellMeltedProxyToTryNow(){
        meltedProxy.tryToExecuteNow();
    }

    public boolean blockMelted(boolean doBlock){
        // If melted is already blocked return false
        if(doBlock == true){
            if(!meltedProxy.isMeltedBloqued()){
                meltedProxy.blockMelted(doBlock);
                return true;
            }
            else {
                return false;
            }
        }

        meltedProxy.blockMelted(doBlock);
        return true;
    }

    public LocalDateTime getLoadedPlDateTimeEnd(){
        return meltedProxy.getLoadedPlDateTimeEnd();
    }

    /**
     * Cleans MeltedProxy's list and Melted's playlist.
     */
    public void cleanProxyAndMeltedLists(){
        meltedProxy.cleanAll();
        pccpFactory.getCommand("CLEAN").execute(meltedCmdFactory);
    }

    /**
     * Returns the calculated end time for the current clip.
     * @return
     */
    public LocalDateTime getCurClipEndTime(){
        JsonObject response = pccpFactory.getCommand("USTA").executeForResponse(meltedCmdFactory);
        LocalDateTime now = LocalDateTime.now();
        int len = response.get("len").getAsInt();
        int curFrame = response.get("curFrame").getAsInt();
        float fps = response.get("fps").getAsFloat();

        int remainingFrames = len - curFrame;
        int remainingSeconds = (int)Math.ceil(remainingFrames / fps);

        return now.plus(remainingSeconds, ChronoUnit.SECONDS);
    }

    /**
     * Returns the local time in which the current clip started playing.
     * @return
     */
    public LocalDateTime getCurClipStartTime(){
        JsonObject response = pccpFactory.getCommand("USTA").executeForResponse(meltedCmdFactory);
        LocalDateTime now = LocalDateTime.now();
        int curFrame = response.get("curFrame").getAsInt();
        float fps = response.get("fps").getAsFloat();

        int elapsedSeconds = (int)Math.ceil(curFrame / fps);

        return now.minus(elapsedSeconds, ChronoUnit.SECONDS);
    }

    /**
     * Returns the path of the current playing clip.
     * Used for debugging
     * @return
     */
    public String getCurClipPath(){
        return pccpFactory.getCommand("USTA").executeForResponse(meltedCmdFactory).get("path").getAsString();
    }
}
