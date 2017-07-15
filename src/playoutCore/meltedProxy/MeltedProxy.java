package playoutCore.meltedProxy;

import java.time.Duration;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;

/**
 * This class handles when to send APND commands to Melted.
 * The purpose of that is to not overload Melted's playlist and just
 * keep an approximated configured amount of time loaded.
 * 
 * @author rombus
 */
public class MeltedProxy {
    private final Logger logger;
    private final int playlistMaxDurationMins;
    private Duration plEndTimestamp;

    public MeltedProxy(int meltedPlaylistMaxDuration, Logger logger){
        this.logger = logger;
        this.playlistMaxDurationMins = meltedPlaylistMaxDuration;
    }

    /**
     * Call this method when melted's playlist changes by removing clips.
     */
    public void meltedPlChanged(){
        // actualizar el plEndTimestamp.
    }

    public void execute(PccpCommand cmd, MvcpCmdFactory meltedCmdFactory){
        /*
            if(plEndTimestamp == 0){ // First time here
                ejecutar apnd
                plEndTimestamp = now + cmd.duration;
            } else if ( (plEndTimestamp - now) < playlistMaxDurationMins  ){
                ejecutar apnd
            } else if ( tengo 1 solo clip cargado) {
                ejecutar apnd
            } else {
                dejar la ejecución para más adelante
            }
        */
        // TODO: no estoy procesando nada todavía
        logger.log(Level.INFO, "Playout Core - Melted Proxy, executing command ...");
        cmd.execute(meltedCmdFactory);
    }
}
