package playoutCore.pccp.commands;

import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import playoutCore.dataStore.DataStore;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;

/**
 *  This command cleans Melted's playlists, removing all clips from it.
 * 
 * @author rombus
 */
public class PccpCLEARALL extends PccpCommand {
    private static final int ID = 0;

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        //TODO hardcoded unit
        String unit = "U0";

        try {
            factory.getStop(unit).exec();
            factory.getClean(unit).exec();
            factory.getRemove(unit).exec();
            Logger.getLogger(PccpCLEARALL.class.getName()).log(Level.INFO, "Playout Core - Melted clips removed.");
        } catch (MeltedCommandException ex) {
            //TODO handle errors
            Logger.getLogger(PccpCLEARALL.class.getName()).log(Level.SEVERE, null, ex);
            return false;
        }
        
        return true;
    }
}
