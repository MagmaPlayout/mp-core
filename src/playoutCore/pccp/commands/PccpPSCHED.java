package playoutCore.pccp.commands;

import java.util.ArrayList;
import playoutCore.pccp.PccpCommand;
import playoutCore.dataStore.DataStore;
import playoutCore.mvcp.MvcpCmdFactory;

/**
 *
 * @author rombus
 */
public class PccpPSCHED extends PccpCommand {

    public PccpPSCHED(ArrayList<String> args){
        super(args);
    }

    @Override
    public boolean execute(MvcpCmdFactory melted, DataStore store) {
        //TODO: implement PSCHED cmd
        // Pedirle a redis la playlist <id>
        // Obtener el path de todos los videos que forman parte de la playlist
        // enviar comandos APND a melted con cada video. Guardar el frame del primer video
        // Schedulear un comando para la fechahora <timestamp> que mueva el cursor a este punto.
        return false;
    }
}
