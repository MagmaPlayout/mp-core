package playoutCore.commands;

import java.util.ArrayList;
import meltedBackend.commands.MeltedCmdFactory;
import playoutCore.dataStore.DataStore;

/**
 *
 * @author rombus
 */
public abstract class PccpCommand {
    public ArrayList<String> args;


    public PccpCommand(){
        this.args = new ArrayList<>();
    }

    public PccpCommand(ArrayList<String> args){
        this.args = args;
    }

    public void setArgs(ArrayList<String> args){
        this.args = args;
    }

    public abstract boolean execute(MeltedCmdFactory meltedCmdFactory, DataStore store);
}
