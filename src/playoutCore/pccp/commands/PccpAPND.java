package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Scheduler;
import playoutCore.dataStore.dataStructures.Clip;
import static playoutCore.dataStore.dataStructures.JsonClip.PIECE_KEY;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command appends a clip into the playout server.
 * 
 * @author rombus
 */
public class PccpAPND extends PccpCommand {
    private final Logger logger;

    public PccpAPND(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        //TODO: validate args lenght, only accepts one clip, that is: only one json object.
        if(args == null){
            logger.log(Level.SEVERE, "Playout Core - No arguments found for APND PCCP command.");
            return false;
        }

        Clip clip = getClipFromJsonArg(args.getAsJsonObject(PIECE_KEY));

        //TODO hardcoded unit
        String unit = "U0";

        try {
            GenericResponse r = factory.getApnd(unit, clip).exec();

            if(!r.cmdOk()){
                logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                return false;
            }
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the PLAYNOW PCCP command. Possibly by a misconfigured melt path configuration");
            return false;
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
