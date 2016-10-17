package playoutCore.mvcp;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmd;
import meltedBackend.commands.MeltedCmdApnd;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedClient;
import meltedBackend.common.MeltedCommandException;
import org.quartz.Scheduler;
import playoutCore.ConfigurationManager;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;

/**
 * This is yet another indirection layer to add core logic to some MVCP commands at MagmaPlayout level.
 *
 * @author rombus
 */
public class MvcpCmdFactory {
    private final MeltedCmdFactory factory;
    private final DataStore store;
    private final Logger logger;
    private final String melt, url;
    private final int bashTimeout;

    public MvcpCmdFactory(MeltedClient melted, DataStore store,  Scheduler scheduler, Logger logger){
        factory = new MeltedCmdFactory(melted);
        this.store = store;
        this.logger = logger;

        ConfigurationManager cfg = ConfigurationManager.getInstance();
        melt = cfg.getMeltPath();
        url = cfg.getFilterServerHost();
        bashTimeout = cfg.getMeltXmlTimeout();
    }

    public MeltedCmdApnd getApnd(String unit, Clip clip, boolean firstInPlaylist) throws MeltedCommandException{
        String path = clip.path;

        // If the clip has a filter I create an mlt xml to load into melted
        if(clip.filterId != Clip.NO_FILTER){
            String filter = store.getFilter(clip.filterId);
            path = createMltFile(clip.path);
        }

        return factory.getNewApndCmd(unit, path);
    }

    private String createMltFile(String clip) throws MeltedCommandException{
        clip = clip.replace("\"", "");
        String xmlPath = clip+"-"+LocalDateTime.now().toEpochSecond(ZoneOffset.UTC)+".xml";
        String cmdString = melt+"/melt "+clip+" -filter webvfx:"+url+" -consumer xml:"+xmlPath;
        
        try{
            Process cmd = Runtime.getRuntime().exec(cmdString);

            try {
                cmd.waitFor(bashTimeout, TimeUnit.MILLISECONDS);
                logger.log(Level.INFO, "Playout Core - Created .xml file for clip {0}", clip);
            } catch (InterruptedException ex) {
                logger.log(Level.WARNING,"Playout Core - Killing XML generation with melted thread.");
            }
        }catch (IOException e){
            logger.log(Level.WARNING, "Playout Core - Couldn''t create an MLT file for clip {0} with the command {1}", new Object[]{clip, cmdString});
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

    public MeltedCmd getClean(String unit){
        return factory.getNewCleanCmd(unit);
    }
}
