package playoutCore.meltedProxy;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import playoutCore.calendar.SpacerGenerator;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.pccp.commands.PccpAPND;

/**
 * This class handles when to send APND commands to Melted.
 * You should not use it directly, use the CommandsExecutor class to handle the commands list.
 *
 * This class must be the bottleneck of all melted media additions.
 * The purpose of that is to not overload Melted's playlist and just
 * keep an approximated configured amount of time loaded.
 *
 * @author rombus
 */
public class MeltedProxy {
    public static boolean autoPilot = true; // Autopilot is when there are no clips to play and the MeltedProxy starts appending the default media over and over
    private final Logger logger;
    private final int plMaxDurationSeconds;
    private LocalDateTime plEndTimestamp;
    private final MvcpCmdFactory meltedCmdFactory;
    private final ConcurrentLinkedQueue<PccpAPND> commandsQueue;
    private final ScheduledExecutorService appenderWorker;
    private final Runnable appenderWorkerRunnable;
    private boolean appenderRunning = false;
    private String spacersPath;
    private ZonedDateTime startingTime;
    

    public MeltedProxy(int meltedPlaylistMaxDuration, MvcpCmdFactory meltedCmdFactory, PccpFactory pccpFactory, int appenderWorkerFreq, Logger logger){
        this.logger = logger;
        this.meltedCmdFactory = meltedCmdFactory;
        this.plMaxDurationSeconds = meltedPlaylistMaxDuration * 60; // meltedPlaylistMaxDuration is in minutes
        commandsQueue = new ConcurrentLinkedQueue();    // TODO: this list must be emptied when a CLEARALL command is issued
        spacersPath = ConfigurationManager.getInstance().getMltSpacersPath();

        // Creates the
        appenderWorker = Executors.newSingleThreadScheduledExecutor();
        appenderWorkerRunnable = new Runnable() {
            @Override
            public void run() {
                if(!appenderRunning){
                    appenderRunning = true;

                    logger.log(Level.INFO, "MeltedProxy - appenderWorkerRunnable running.");
                    try{
                        if(!commandsQueue.isEmpty()){
                            PccpAPND cmd = commandsQueue.peek(); // Get's the first element of the FIFO queue (doesn't remove it from the Q)
                            boolean executed = tryToExecute(cmd);
                            if(executed){
                                // If last command executed was a spacer it's because I'm on autopilot
                                autoPilot = cmd.args.getAsJsonObject().get("piece").getAsJsonObject().get("path").getAsString().startsWith(spacersPath);                                
                                commandsQueue.poll();            // Removes the first element from the FIFO queue
                                appenderWorkerRunnable.run();    // Tries again to see if another queued command can be executed
                            }
                        } else {
                            logger.log(Level.INFO, "MeltedProxy - Check to see if I can fit a default media.");
                            // See if I can fit in a default media
                            Occurrence oc = SpacerGenerator.getInstance().generateImageSpacer(null, null, Duration.of(3, ChronoUnit.MINUTES));
                            if(tryToExecute(pccpFactory.getAPNDFromOccurrence(oc, 0))){
                                autoPilot = true;
                            }
                        }
                    } catch(Exception e){
                        //TODO: handle
                        System.out.println("Exception on appenderWorkerRunnable");
                        e.printStackTrace();
                    } finally {
                        appenderRunning = false;
                    }
                }
            }
        };
        appenderWorker.scheduleAtFixedRate(appenderWorkerRunnable, 1, appenderWorkerFreq, TimeUnit.MINUTES);
    }

    /**
     * A way to force the execution of commandsQueue commands.
     */
    public void tryToExecuteNow(){
        appenderWorkerRunnable.run();
    }

    /**
     * Interface for "executing" commands.
     * In reality commands are only added to a queue here, and another process
     * executes them when needed.
     *
     * @param cmd
     */
    public void execute(PccpAPND cmd){
        commandsQueue.add(cmd);

        if(plEndTimestamp == null){ // If the list is empty, try to make the execution now
            appenderWorkerRunnable.run();
        }
    }

    /**
     * Call this method when melted's playlist changes by removing clips.
     */
    public void meltedPlChanged(){
        // TODO: ver como hacemos acá
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void meltedDisconnected(){
        // TODO: frenar el scheduleo por comandos, y armar el método contraparte de meltedConnected();
        throw new UnsupportedOperationException("Not supported yet.");
    }


    /**
     * This method is called repeatedly to execute the APND commands when needed.
     *
     * Commands are going to be executed only if the loaded play time of melted is less
     * than the specified in plMaxDurationMins.
     *
     * @param cmd first in the FIFO of queued commands
     * @return true if execution was succesful; false otherwise (not executed or executed with failure).
     */
    private boolean tryToExecute(PccpAPND cmd){
        boolean executed = false;

        LocalDateTime nowLDT = LocalDateTime.now();
        if (plEndTimestamp != null){
            logger.log(Level.INFO, "Seconds between now and plEndTimestamp:  "+String.valueOf(ChronoUnit.SECONDS.between(nowLDT, plEndTimestamp)));
        }

        if (plEndTimestamp == null){ // No medias loaded in melted
            plEndTimestamp = LocalDateTime.now();
            executed = doExecute(cmd);
        }
        else if ( ChronoUnit.SECONDS.between(nowLDT, plEndTimestamp) < plMaxDurationSeconds ){
            executed = doExecute(cmd);
        }
        else {
            //TODO: debug log
            logger.log(Level.INFO, "MeltedProxy: no need for APND.");
            logger.log(Level.INFO, "plEndTimestamp: "+plEndTimestamp.toString());
        }

        logger.log(Level.INFO, "MeltedProxy - tryToExecute()! ChronoUnit.SECONDS.between(nowLDT, plEndTimestamp) < plMaxDurationSeconds: ["+ChronoUnit.SECONDS.between(nowLDT, plEndTimestamp)+", "+plMaxDurationSeconds+"]");
        return executed;
    }

    /**
     * Returns the LocalDateTime in which the loaded melted's playlist will end.
     * @return
     */
    public LocalDateTime getLoadedPlDateTimeEnd() {
        return plEndTimestamp;
    }


    /**
     * Executes the PccpCommand and modifies the plEndTimestamp accordingly.
     * This method is called by tryToExecute(cmd);
     *
     * @param cmd
     * @return true if command succeded; false otherwise
     */
    private boolean doExecute(PccpCommand cmd){
        Duration length = cmd.getLength();

        boolean status = cmd.execute(meltedCmdFactory);
        if(status){
            // Add apended clip length to the plEndTimestamp
            //TODO: asumo que melted está en modo play
            if(startingTime != null){
                plEndTimestamp = startingTime.toLocalDateTime();
                startingTime = null;
            }
            plEndTimestamp = plEndTimestamp.plus(length);
            logger.log(Level.INFO, "MeltedProxy executed a APND command. Playlist will run until {0}", plEndTimestamp.toString());
            logger.log(Level.INFO, cmd.toString());
        }
        else {
            logger.log(Level.WARNING, "MeltedProxy tried to execute a APND command but failed!");
        }

        return status;
    }

    /**
     * Cleans commandsQueue and reset's plEndTimestamp.
     * This way the proxy is emptied.
     */
    public void cleanAll(){
        commandsQueue.clear();
        plEndTimestamp = null;
    }

    /**
     * Must use this when a schedule goto command is issued.
     * It's needed to calculate the plEndTimestamp correctly
     * 
     * @param startTime
     */
    public void setScheduledStartingTime(ZonedDateTime startTime){
        this.startingTime = startTime;
    }
}
