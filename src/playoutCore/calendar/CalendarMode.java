package playoutCore.calendar;

import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.calendar.dataStore.MPPlayoutCalendarApi;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.pccp.PccpCommand;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;

/**
 *
 * @author rombus
 */
public class CalendarMode implements Runnable{
    private final Logger logger;
    private final PccpFactory cmdFactory;
    private final MPPlayoutCalendarApi api;
    private final SpacerGenerator spacerGen;
    private final CommandsExecutor cmdExecutor;

    public CalendarMode(MPPlayoutCalendarApi api, PccpFactory cmdFactory, CommandsExecutor cmdExecutor, Logger logger) {
        this.logger = logger;
        this.api = api;
        this.cmdFactory = cmdFactory;
        this.cmdExecutor = cmdExecutor;
        spacerGen = SpacerGenerator.getInstance();
    }

    @Override
    public void run() {
        ArrayList<Occurrence> occurrences = api.getAllOccurrences();
        occurrences = spacerGen.generateNeededSpacers(occurrences);   // Takes the occurrences list and adds the spacers in the right places (if needed)

        // TODO: (steps)
        // clear melted's playlist
        // take into account the actual time and the startDateTime of the first occurrence. Generate a spacer with that info and add it first of all
        // add every other occurrence

        ArrayList<PccpCommand> commands = new ArrayList<>();
        int curPos = 1;
        for(Occurrence cur:occurrences){
            commands.add(cmdFactory.getAPNDFromOccurrence(cur, curPos));
            curPos++;
        }

        cmdExecutor.addPccpCmdsToExecute(commands);

        logger.log(Level.INFO, "Playout Core - CalendarMode thread finished");
    }
}
