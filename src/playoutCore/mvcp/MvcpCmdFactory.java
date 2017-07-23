package playoutCore.mvcp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import meltedBackend.commands.MeltedCmd;
import meltedBackend.commands.MeltedCmdApnd;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedClient;
import meltedBackend.common.MeltedCommandException;
import playoutCore.dataStructures.Clip;

/**
 * This is yet another indirection layer to add core logic to some MVCP commands at MagmaPlayout level.
 *
 * @author rombus
 */
public class MvcpCmdFactory {
    private final MeltedCmdFactory factory;
    private final Logger logger;
    private final String melt, url;
    private final int bashTimeout;

    public MvcpCmdFactory(MeltedClient melted, Logger logger){
        factory = new MeltedCmdFactory(melted);
        this.logger = logger;

        ConfigurationManager cfg = ConfigurationManager.getInstance();
        melt = cfg.getMeltPath();
        url = cfg.getFilterServerHost();
        bashTimeout = cfg.getMeltXmlTimeout();
    }

    public MeltedCmdApnd getApnd(String unit, Clip clip) throws MeltedCommandException{
        String path = clip.path;

        // If the clip has a filter I create an mlt xml to load into melted
        if(clip.filterId != Clip.NO_FILTER){
            path = createMltFile(clip.path, clip.frameLen, clip.fps);
        }

        return factory.getNewApndCmd(unit, path);
    }

    private String createMltFile(String clip, int frameLen, int fps) throws MeltedCommandException{
        clip = clip.replace("\"", "");
        String xmlPath = clip+"-"+LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)+".mlt";
        String cmdString = melt+" "+clip+" out="+frameLen+" length="+frameLen+" -filter webvfx:http://"+url+" -consumer xml:"+xmlPath+" frame_rate_num="+fps;
        
        try{
            Process cmd = Runtime.getRuntime().exec(cmdString);

            try {
                logger.log(Level.INFO, "Playout Core - Waiting for the .mlt generation to end...");
                cmd.waitFor(bashTimeout, TimeUnit.MILLISECONDS);
                logger.log(Level.INFO, "Playout Core - Created .mlt file for clip {0}", clip);
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING,"Playout Core - Killing .mlt generation with melted thread.");
            }
        }catch (IOException e){
            logger.log(Level.WARNING, "Playout Core - Could not create an MLT file for clip {0} with the command {1}", new Object[]{clip, cmdString});
            throw new MeltedCommandException("Playout Core - aborted command.");
        }
        
        // TODO testear la integridad del xml!!!
        return xmlPath;
    }


    public MeltedCmd getList(String unit){
        return factory.getNewListCmd(unit);
    }

    public MeltedCmd getGoto(String unit, int framePosition, int clipId){
        return factory.getNewGotoCmd(unit, framePosition, clipId);
    }

    public MeltedCmd getPlay(String unit){
        return factory.getNewPlayCmd(unit);
    }

    public MeltedCmd getStop(String unit){
        return factory.getNewStopCmd(unit);
    }

    public MeltedCmd getRemove(String unit){
        return factory.getNewRemoveCmd(unit);
    }

    public MeltedCmd getRemove(String unit, int playlistIndex){
        return factory.getNewRemoveCmd(unit, playlistIndex);
    }

    public MeltedCmd getClean(String unit){
        return factory.getNewCleanCmd(unit);
    }

    public MeltedCmd getWipe(String unit){
        return factory.getNewWipeCmd(unit);
    }

    public MeltedCmd getInsert(String unit, String path, int playlistIndex){
        return factory.getNewInsertCmd(unit, path, playlistIndex);
    }
}
