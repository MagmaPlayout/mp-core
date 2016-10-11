package playoutCore.commands;

import java.util.ArrayList;
import java.util.Arrays;
import playoutCore.commands.pccp.PccpCLEARALL;
import playoutCore.commands.pccp.PccpPLAYNOW;
import playoutCore.commands.pccp.PccpPSCHED;

/**
 * Playout Core Command Protocol.
 * Utility class that enumerates the available commands,
 * validates command strings and creates PccpCommand objects.
 *
 * @author rombus
 */
public class PccpFactory {
    /**
     * Supported commands
     */
    private enum Commands {
        PLAYNOW,    // Plays given playlist as soon as it can. PLAYNOW <playlist id>
        CLEARALL,   // Removes everything from the playlist. No arguments.
        PSCHED,     // Schedules a given playlist. PSCHED <name> <timestamp>

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

        public static PccpCommand convertCmdStrToObj(String opcode, ArrayList<String> args){
            Commands oc = getEnumFromString(opcode);

            //TODO object pool for PccpCommands
            if(oc != null){
                switch(oc){
                    case PSCHED:
                        return new PccpPSCHED(args);
                    case PLAYNOW:
                        return new PccpPLAYNOW(args);
                    case CLEARALL:
                        return new PccpCLEARALL();
                }
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

        PccpCommand cmd = Commands.convertCmdStrToObj(opcode, args);
        return cmd;
    }
}
