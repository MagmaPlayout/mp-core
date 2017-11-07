package playoutCore.mvcp;

import java.util.logging.Logger;
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

    public MvcpCmdFactory(MeltedClient melted, Logger logger){
        factory = new MeltedCmdFactory(melted);
        this.logger = logger;
    }

    public MeltedCmdApnd getApnd(String unit, Clip clip) throws MeltedCommandException{
        String path = clip.path;

        return factory.getNewApndCmd(unit, path);
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

    public MeltedCmd getUsta(String unit){
        return factory.getNewUstaCmd(unit);
    }
}
