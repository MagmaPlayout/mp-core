package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmd;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Scheduler;
import playoutCore.dataStructures.Clip;
import static playoutCore.dataStructures.JsonClip.PIECE_KEY;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command moves the playing cursor to the specified clip, wiping all previous clips from the melted backend
 * 
 * @author rombus
 */
public class PccpREMOVE extends PccpCommand {
    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpREMOVE(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.fscpChannel = fscpChannel;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        String unit = "U0"; //TODO hardcoded unit
        
        try {
            MeltedCmd removeCmd;
            if(args != null){
                Clip clip = getClipFromJsonArg(args.getAsJsonObject(PIECE_KEY));
                removeCmd = factory.getRemove(unit, clip.playlistIdx);
            }
            else {
                removeCmd = factory.getRemove(unit);
            }
            
            GenericResponse r = removeCmd.exec();

            if(!r.cmdOk()){
                logger.log(Level.WARNING, "Playout Core - Could not remove clip. Melted error: "+r.getStatus()
                        + "Check the bash_timeout configuration key.");
                return false;
            }

            logger.log(Level.INFO, "Playout Core - Removed media");
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the REMOVE PCCP command. Possibly by a misconfigured melt path configuration");
            return false;
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
