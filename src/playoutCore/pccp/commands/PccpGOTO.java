package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import org.quartz.Scheduler;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
import static playoutCore.dataStore.dataStructures.JsonClip.MEDIA_KEY;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command moves the playing cursor to the specified clip, wiping all previous clips from the melted backend
 * 
 * @author rombus
 */
public class PccpGOTO extends PccpCommand {
    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpGOTO(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
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
            logger.log(Level.SEVERE, "Playout Core - No arguments found for GOTO PCCP command.");
            return false;
        }
        Clip clip = getClipFromJsonArg(args.getAsJsonObject(MEDIA_KEY));

        //TODO hardcoded unit
        String unit = "U0";
        
        try {
            GenericResponse r = factory.getApnd(unit, clip).exec();

            if(!r.cmdOk()){
                logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                return false;
            }

            // Move cursor to the specified clip
            factory.getGoto(unit, 0, clip.playlistIdx).exec();
            factory.getPlay(unit).exec();
            factory.getWipe(unit).exec();

            if(clip.filterId != Clip.NO_FILTER){
                //TODO remove this publish from here and centralize it on a FSCP class.
                publisher.publish(fscpChannel, "SETFILTER "+String.valueOf(clip.filterId)); // Tell the Filter Server to change it's filterId
                logger.log(Level.INFO, "Playout Core - Setting filter server filter id:{0}", clip.filterId);
            }
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the GOTO PCCP command. Possibly by a misconfigured melt path configuration");
            return false;
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory, DataStore store) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
