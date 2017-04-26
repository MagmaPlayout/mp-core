package playoutCore.pccp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.pccp.commands.PccpCLEARALL;
import playoutCore.pccp.commands.PccpPLAYNOW;
import playoutCore.pccp.commands.PccpPLPLAYNOW;
import playoutCore.pccp.commands.PccpPLSCHED;
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
    private final String fscpChannel;
    private final Scheduler scheduler;
    private final Logger logger;
    
    public PccpFactory(Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        this.publisher = publisher;
        this.fscpChannel = fscpChannel;
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

        public static PccpCommand convertCmdStrToObj(String opcode, 
                ArrayList<String> args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
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
        ArrayList<String> args = new ArrayList<>(Arrays.asList(Arrays.copyOfRange(explodedCmd, 1, explodedCmd.length)));

        PccpCommand cmd = Commands.convertCmdStrToObj(opcode, args, publisher, fscpChannel, scheduler, logger);
        return cmd;
    }
}
