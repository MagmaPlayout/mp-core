package playoutCore.pccp;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.pccp.commands.PccpAPND;
import playoutCore.pccp.commands.PccpCLEARALL;
import playoutCore.pccp.commands.PccpGETPL;
import playoutCore.pccp.commands.PccpGOTO;
import playoutCore.pccp.commands.PccpMOVE;
import playoutCore.pccp.commands.PccpPLAYNOW;
import playoutCore.pccp.commands.PccpPLPLAYNOW;
import playoutCore.pccp.commands.PccpPLSCHED;
import playoutCore.pccp.commands.PccpREMOVE;
import redis.clients.jedis.Jedis;

/**
 * Playout Core Command Protocol.
 * Utility class that enumerates the available commands,
 * validates command strings and creates PccpCommand objects.
 *
 * @author rombus
 */
public class PccpFactory {
    private final Jedis publisher;
    private final String fscpChannel, pcrChannel;
    private final Scheduler scheduler;
    private final Logger logger;

    public PccpFactory(Jedis publisher, String fscpChannel, String pcrChannel, Scheduler scheduler, Logger logger){
        this.publisher = publisher;
        this.fscpChannel = fscpChannel;
        this.pcrChannel = pcrChannel;
        this.scheduler = scheduler;
        this.logger = logger;
    }


    /**
     * Supported commands
     */
    private enum Commands {
        PLAYNOW,    // Plays given clip as soon as it can. PLAYNOW <clip id>
        PLPLAYNOW,  // Plays given playlist as soon as it can. PLAYNOW <playlist id>
        CLEARALL,   // Removes everything from the playlist. No arguments.
        PLSCHED,    // Schedules a given playlist. PLSCHED <playlist id> <timestamp>
        GETPL,      // Returns the playlist loaded in melted plus the clips that will be added to melted in schedule
        GETTIMERS,  // Returns a JSON with the 3 timers (uptime, clip time, playlist remaining time)
        APND,       // Appends a clip to the playout's playlist
        REMOVE,     // Removes a given playlist index
        MOVE,       // Moves a media from 1 playlist index to another
        GOTO,       // Moves the playing cursor to the specified index

        STANDBY,    // Plays the stand by, "technical difficulties" media. . 1 argument: which standby to play
        PREM,       // Playlist Removed. 1 argument: playlist name/id
        PMOD,       // Playlist Modified. 1 argument: playlist name/id
        CREM;       // Clip Removed. 1 argument: clip name


        public static Commands getEnumFromString(String opcode){
            for(Commands cmd: Commands.values()){
                if(opcode.equals(cmd.name())){
                    return cmd;
                }
            }
            return null;
        }

        public static PccpCommand convertCmdStrToObj(String opcode, JsonObject args, Jedis publisher,
                String fscpChannel, String pcrChannel, Scheduler scheduler, Logger logger){
            Commands oc = getEnumFromString(opcode);

            //TODO object pool for PccpCommands
            if(oc != null){
                PccpCommand cmd = null;
                switch(oc){
                    case PLSCHED:
                        cmd = new PccpPLSCHED(args, scheduler, logger);
                        break;
                    case PLAYNOW:
                        cmd = new PccpPLAYNOW(args, publisher, fscpChannel, scheduler, logger);
                        break;
                    case PLPLAYNOW:
                        cmd = new PccpPLPLAYNOW(args, publisher, fscpChannel, scheduler, logger);
                        break;
                    case CLEARALL:
                        cmd = new PccpCLEARALL();
                        break;
                    case GETPL:
                        cmd = new PccpGETPL();
                        break;
                    case APND:
                        cmd = new PccpAPND(args, publisher, fscpChannel, scheduler, logger);
                        break;
                    case GETTIMERS:
                        System.out.println("GETTIMERS NOT IMPLEMENTED YET!!!");
                        break;
                    case REMOVE:
                        cmd = new PccpREMOVE(args, publisher, fscpChannel, scheduler, logger);
                        break;
                    case MOVE:
                        cmd = new PccpMOVE(args, publisher, fscpChannel, scheduler, logger);
                        break;
                    case GOTO:
                        cmd = new PccpGOTO(args, publisher, fscpChannel, scheduler, logger);
                        break;
                }

                return cmd;
            }

            return null;
        }
    };

    /**
     * Converts a command string into a PccpCommand object
     *
     * @param commandString
     * @return
     */
    public PccpCommand getCommand(String commandString){
        String[] explodedCmd = commandString.split(" ");
        String opcode = explodedCmd[0];
        JsonObject args;

        if(explodedCmd.length < 2){
            args = null; // GETPL needs no args
        }
        else try {
            String argString = commandString.substring(opcode.length()); // Removes opcode from commandString
            args = new JsonParser().parse(argString).getAsJsonObject();
        }catch(IllegalStateException e){ // args is not a json object
            return null;
        }
        
        //TODO: handle args being null on commands that NEED args.
        PccpCommand cmd = Commands.convertCmdStrToObj(opcode, args, publisher, fscpChannel, pcrChannel, scheduler, logger);
        return cmd;
    }
}
