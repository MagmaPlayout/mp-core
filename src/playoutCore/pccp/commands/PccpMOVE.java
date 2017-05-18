package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Scheduler;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command inserts the specified media in the specified playlist index (newPos) and then removes the previous position (oldPos)
 * 
 * @author rombus
 */
public class PccpMOVE extends PccpCommand {
    private static final String MEDIA_KEY = "media";
    private static final String OLD_POS_KEY = "oldPos";
    private static final String NEW_POS_KEY = "newPos";

    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpMOVE(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.fscpChannel = fscpChannel;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        //TODO: validate args lenght, only accepts one clip, that is only one json object.
        if(args == null){
            logger.log(Level.SEVERE, "Playout Core - No arguments found for MOVE PCCP command.");
            return false;
        }

        //TODO hardcoded unit
        String unit = "U0";
        Clip clip = getClipFromJsonArg(args.getAsJsonObject(MEDIA_KEY));
        int oldPos =  args.getAsJsonPrimitive(OLD_POS_KEY).getAsInt();
        int newPos =  args.getAsJsonPrimitive(NEW_POS_KEY).getAsInt();

        try {
            if(newPos < oldPos) {
                oldPos += 1; // The list will move 1 up, as I will insert a new clip in newPos
            } else {
                newPos +=1;
            }

            GenericResponse r;
            r = factory.getInsert(unit, clip.path, newPos).exec();
            if(!r.cmdOk()){
                logger.log(Level.SEVERE, "Playout Core - Could not insert clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key."
                        , new Object[]{clip.path, r.getStatus()});
                return false;
            }

            r = factory.getRemove(unit, oldPos).exec();
            if(!r.cmdOk()){
                logger.log(Level.SEVERE, "Playout Core - Could not remove clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                return false;
            }


            if(clip.filterId != Clip.NO_FILTER){
                // TODO: implement filters
            }
            logger.log(Level.INFO, "Playout Core - Moved clip {0} to index {1}. ", new Object[]{clip.path, newPos});
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the MOVE PCCP command. Possibly by a misconfigured melt path configuration");
            return false;
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory, DataStore store) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
