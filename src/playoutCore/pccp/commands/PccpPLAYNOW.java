package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.GenericResponse;
import meltedBackend.responseParser.responses.ListResponse;
import org.quartz.Scheduler;
import playoutCore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command loads a clip into melted and moves the cursor to it's first frame.
 * 
 * @author rombus
 */
public class PccpPLAYNOW extends PccpCommand {
    private final Logger logger;
    private final Jedis publisher;
    private final String fscpChannel;
    private final Scheduler scheduler;

    public PccpPLAYNOW(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.publisher = publisher;
        this.scheduler = scheduler;
        this.fscpChannel = fscpChannel;
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        //TODO: validate args lenght, only accepts one clip, that is only one json object.
        if(args == null){
            logger.log(Level.SEVERE, "Playout Core - No arguments found for PLAYNOW PCCP command.");
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

        LocalDateTime start = null;
        Duration playlistLength = Duration.ZERO;
        try {
            GenericResponse r = factory.getApnd(unit, clip).exec();

            if(!r.cmdOk()){
                logger.log(Level.WARNING, "Playout Core - Could not append clip {0} Melted error: {1}. "
                        + "Check the bash_timeout configuration key.", new Object[]{clip.path, r.getStatus()});
                return false;
            }

            // Move cursor to the first added clip of this playlist
            factory.getGoto(unit, 0, lastClipId).exec();
            factory.getPlay(unit).exec();
            start = LocalDateTime.now();

            if(clip.filterId != Clip.NO_FILTER){
                //TODO remove this publish from here and centralize it on a FSCP class.
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
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
