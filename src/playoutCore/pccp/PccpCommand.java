package playoutCore.pccp;

import java.util.ArrayList;
import playoutCore.dataStore.DataStore;
import playoutCore.mvcp.MvcpCmdFactory;

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

    public abstract boolean execute(MvcpCmdFactory meltedCmdFactory, DataStore store);
}
