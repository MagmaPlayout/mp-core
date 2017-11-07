package playoutCore;

import java.util.ArrayList;
import playoutCore.calendar.SpacerGenerator;
import playoutCore.modeSwitcher.ModeManager;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;

/**
 * This class loads saved clips or default media if there are no saved ones.
 * It's executed when the program is started to set up the initial content.
 * It's never called again.
 * TODO: load saved clips from BD (if any)
 *
 * @author rombus
 */
public class ContentLoader {
    private final SpacerGenerator spacerGen;
    private final CommandsExecutor executor;
    private final PccpFactory pccpFactory;
    

    public ContentLoader(CommandsExecutor executor, PccpFactory pccpFactory){
        this.executor = executor;
        this.pccpFactory = pccpFactory;
        spacerGen = SpacerGenerator.getInstance();
    }

    /**
     * Reads the DB and loads all programmed medias if any (default one otherwise).
     * It also sends the PLAY command so that melted starts playling and never should stop.
     *
     */
    public void loadSavedClips(){
        ModeManager.getInstance().changeToCalendarMode();

        ArrayList<PccpCommand> commands = new ArrayList<>();
        commands.add(pccpFactory.getCommand("PLAY"));
        executor.addPccpCmdsToExecute(commands);
        executor.tellMeltedProxyToTryNow();
    }
}
