package playoutCore.commands.pccp;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.commands.MeltedCmdFactory;
import meltedBackend.common.MeltedCommandException;
import playoutCore.commands.PccpCommand;
import playoutCore.dataStore.DataStore;

/**
 *  This command loads all clips of a given playlist into melted and moves the cursor to the first clip frame.
 * 
 * @author rombus
 */
public class PccpPLAYNOW extends PccpCommand {
    private static final int ID = 0;

    public PccpPLAYNOW(ArrayList<String> args){
        super(args);
    }

    @Override
    public boolean execute(MeltedCmdFactory factory, DataStore store) {
        String id = args.get(ID);
        ArrayList<String> clips = store.getPlaylistClips(id);
        boolean first = true;

        for(String clip: clips){
            //TODO hardcoded unit
            String unit = "U0";

            try {
                factory.getNewApndCmd(unit, clip).exec();
                store.incrementPlaylistLength();

                // Move cursor to the first added clip of this playlist
                if(first){
                    first = false;
                    int lastPlaylistPosition = store.getPlaylistLength();
                    factory.getNewGotoCmd(unit, 0, lastPlaylistPosition-1).exec();
                    factory.getNewPlayCmd(unit).exec();
                }
            } catch (MeltedCommandException ex) {
                //TODO handle errors
                Logger.getLogger(PccpPLAYNOW.class.getName()).log(Level.SEVERE, null, ex);
                return false;
            }
        }
        
        return true;
    }
}
