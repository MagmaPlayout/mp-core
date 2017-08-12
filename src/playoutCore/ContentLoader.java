package playoutCore;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import playoutCore.calendar.SpacerGenerator;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;

/**
 * This class loads saved clips or default media if there are no saved ones.
 *
 * @author rombus
 */
public class ContentLoader {
    private final SpacerGenerator spacerGen;
    private CommandsExecutor executor;
    private PccpFactory pccpFactory;
    

    public ContentLoader(CommandsExecutor executor, PccpFactory pccpFactory){
        this.executor = executor;
        this.pccpFactory = pccpFactory;
        spacerGen = SpacerGenerator.getInstance();
    }

    /**
     * Reads the DB and loads all programmed medias if any (default one otherwise).
     *
     */
    public void loadSavedClips(){
        ArrayList<PccpCommand> commands = new ArrayList<>();
        PccpCommand playCmd = pccpFactory.getCommand("PLAY");

        // TODO: pedir a la BD los medias cargados
        // calcular la fecha actual y la del comienzo de la lista
        // para determinar que cargar y que no, incluyendo el número de frame

        // if( no hay medias en la bd) {
        Occurrence oc = spacerGen.generateImageSpacer(null, null, Duration.of(10, ChronoUnit.MINUTES));
        PccpCommand cmd = pccpFactory.getAPNDFromOccurrence(oc, 0);
        commands.add(cmd);
        // }

        
        commands.add(playCmd);
        executor.addPccpCmdsToExecute(commands);
        executor.tellMeltedProxyToTryNow();
    }
}
