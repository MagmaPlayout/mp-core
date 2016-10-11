package playoutCore.commands.pccp;

import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedCommandException;
import playoutCore.commands.PccpCommand;
import playoutCore.dataStore.DataStore;

/**
 *  This command cleans Melted's playlists, removing all clips from it.
 * 
 * @author rombus
 */
public class PccpCLEARALL extends PccpCommand {
    private static final int ID = 0;

    @Override
    public boolean execute(MeltedCmdFactory factory, DataStore store) {
        String unit = "U0";

        try {
            factory.getNewStopCmd(unit).exec();
            factory.getNewCleanCmd(unit).exec();
            factory.getNewRemoveCmd(unit).exec();
            store.resetPlaylist();
        } catch (MeltedCommandException ex) {
            //TODO handle errors
            Logger.getLogger(PccpCLEARALL.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
}
