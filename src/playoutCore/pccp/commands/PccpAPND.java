package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import meltedBackend.responseParser.responses.ListResponse;
import org.quartz.Scheduler;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
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
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpAPND(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
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
            logger.log(Level.SEVERE, "Playout Core - No arguments found for APND PCCP command.");
            return false;
        }

        Clip clip = getClipFromJsonArg(args);

        //TODO hardcoded unit
        String unit = "U0";
        int lastClipId = 0;
        
        try {
            lastClipId = ((ListResponse)factory.getList(unit).exec()).getPlaylistLength()-1;
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured while executing the LIST melted command. Cannot get playlist lenght!");
            return false;
        }

        Duration playlistLength = Duration.ZERO;
        try {
            GenericResponse r = factory.getApnd(unit, clip).exec();

            if(!r.cmdOk()){
                logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                return false;
            }


            if(clip.filterId != Clip.NO_FILTER){
                //TODO: schedule filter if any
                publisher.publish(fscpChannel, "SETFILTER "+String.valueOf(clip.filterId)); // Tell the Filter Server to change it's filterId
                logger.log(Level.INFO, "Playout Core - Setting filter server filter id:{0}", clip.filterId);
            }

            playlistLength = playlistLength.plus(clip.len);
            logger.log(Level.INFO, "Playout Core - Playlist length: {0}", playlistLength.toString());
        } catch (MeltedCommandException ex) {
            logger.log(Level.SEVERE, "Playout Core - An error occured during the execution of the PLAYNOW PCCP command. Possibly by a misconfigured melt path configuration");
            return false;
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory, DataStore store) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
