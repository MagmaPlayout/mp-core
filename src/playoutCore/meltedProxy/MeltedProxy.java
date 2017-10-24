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
import playoutCore.calendar.SpacerGenerator;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;

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
    private final Logger logger;
    private final int plMaxDurationSeconds;
    private LocalDateTime plEndTimestamp;
    private final MvcpCmdFactory meltedCmdFactory;
    private final ConcurrentLinkedQueue<PccpCommand> commandsQueue;
    private final ScheduledExecutorService periodicAppenderWorker;
    private final ScheduledExecutorService appenderWorker;
    private final Runnable appenderWorkerRunnable;
    private static boolean appenderRunning = false;
    private ZonedDateTime startingTime;
    public static boolean activeSequenceTransaction = false;

    public MeltedProxy(int meltedPlaylistMaxDuration, MvcpCmdFactory meltedCmdFactory, PccpFactory pccpFactory, int appenderWorkerFreq, Logger logger){
        this.logger = logger;
        this.meltedCmdFactory = meltedCmdFactory;
        this.plMaxDurationSeconds = meltedPlaylistMaxDuration * 60; // meltedPlaylistMaxDuration is in minutes
        commandsQueue = new ConcurrentLinkedQueue();    // TODO: this list must be emptied when a CLEARALL command is issued

        // Creates the
        periodicAppenderWorker = Executors.newSingleThreadScheduledExecutor();
        appenderWorker = Executors.newScheduledThreadPool(5);
        appenderWorkerRunnable = new Runnable() {
            @Override
            public void run() {
                boolean tryAgain = false;

                if(!appenderRunning){
                    appenderRunning = true;
                    try{
                        if(!commandsQueue.isEmpty()){
                            PccpCommand cmd = commandsQueue.peek(); // Get's the first element of the FIFO queue (doesn't remove it from the Q)

                            switch(cmd.sequenceModifier){
                                case START_SEQ:
                                    logger.log(Level.INFO, "MeltedProxy - implicitly STARTING sequence transaction");
                                    activeSequenceTransaction = true;
                                    break;
                                case END_SEQ:
                                    logger.log(Level.INFO, "MeltedProxy - implicitly ENDING sequence transaction");
                                    activeSequenceTransaction = false;
                                    break;
                            }

                            boolean executed = tryToExecute(cmd);
                            if(executed){
                                logger.log(Level.INFO, "  MeltedProxy - Appended a clip.");// Now will see if another one can be appended as well...");
                                commandsQueue.poll();   // Removes the first element from the FIFO queue
                                tryAgain = true;
                            }

                        } else if (!activeSequenceTransaction){
                            logger.log(Level.INFO, "  MeltedProxy - Check to see if I can fit a default media.");
                            // See if I can fit in a default media
                            Occurrence oc = SpacerGenerator.getInstance().generateImageSpacer(null, null, Duration.of(30, ChronoUnit.MINUTES)); //TODO make this length configurable
                            if(tryToExecute(pccpFactory.getAPNDFromOccurrence(oc, 0))){
                                tryAgain = true;
                            }
                        }
                    } catch(Exception e){
                        //TODO: handle
                        System.out.println("Exception on appenderWorkerRunnable");
                        e.printStackTrace();
                    } finally {
                        appenderRunning = false;
                    }
                } else {
                    logger.log(Level.INFO, "  MeltedProxy - Tried to run but already running!!");
                }

                // Runs again to see if another clip can be added (real or default) or if a sequence transaction is not ended
                if(tryAgain || activeSequenceTransaction){
                    logger.log(Level.INFO, "  MeltedProxy - Trying the execution again! check if you see duplicates here.");
                    appenderWorker.schedule(appenderWorkerRunnable, 50, TimeUnit.MILLISECONDS); // Run again in a little while
                    //TODO: kill appenderWorker scheduledAtFixedRate
                    // appenderWorker.shutdown();
                } else {
                    //TODO: reschedule atfixedRate the appender worker
                    // appenderWorker.scheduleAtFixedRate(appenderWorkerRunnable, 1, appenderWorkerFreq, TimeUnit.MINUTES);
                }
            }
        };

        // Makes the appenderWorker to run each appenderWorkerFreq minutes and by this assuring that melted always has something to play
        periodicAppenderWorker.scheduleAtFixedRate(appenderWorkerRunnable, 1, appenderWorkerFreq, TimeUnit.MINUTES);
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
    public void execute(PccpCommand cmd){
        commandsQueue.add(cmd);

        if(plEndTimestamp == null){ // If the list is empty, try to make the execution now
            appenderWorkerRunnable.run();
        }
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
    private boolean tryToExecute(PccpCommand cmd){
        boolean executed = false;

        LocalDateTime nowLDT = LocalDateTime.now();

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

    public void startSequenceTransaction(){
        logger.log(Level.INFO, "MeltedProxy - explicitly STARTING sequence transaction");
        MeltedProxy.activeSequenceTransaction = true;
    }
    public void endSequenceTransaction(){
        logger.log(Level.INFO, "MeltedProxy - explicitly ENDING a sequence transaction");
        MeltedProxy.activeSequenceTransaction = false;
    }

    /**
     * Stops the execution of the appenderWorkerThread and resets the blockQueue and blockMelted flags.
     * This is called when the CalendarMode wants to run.
     */
    public void interruptAppenderThread(){
//        periodicAppenderWorker.shutdownNow();
    }
}
