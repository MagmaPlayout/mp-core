package playoutCore.modeSwitcher;

import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import playoutCore.calendar.CalendarMode;
import playoutCore.calendar.dataStore.CalendarApi;
import playoutCore.pccp.PccpFactory;
import playoutCore.producerConsumer.CommandsExecutor;

/**
 * This class is responsible for changing between live mode and calendar mode.
 * 
 * @author rombus
 */
public class ModeManager {
    private static ModeManager instance;
    private final CalendarMode calendarMode;
    private final Logger logger;

    /**
     * This class manages the different modes of MP (live mode and calendar mode at the moment).
     * You need to call init(ms) before calling this in order to setup the static instance.
     * 
     * @param cmdFactory
     * @param cmdExecutor
     * @param logger 
     */
    public ModeManager(PccpFactory cmdFactory, CommandsExecutor cmdExecutor, Logger logger){
        this.logger = logger;
        calendarMode = new CalendarMode(
            new CalendarApi(ConfigurationManager.getInstance().getRestBaseUrl(),
            logger),
            cmdFactory,
            cmdExecutor,
            logger
        );
    }

    /**
     * Makes a static reference to the ModeManager specified.
     *
     * @param mm will be the singleton instance.
     */
    public void init(ModeManager mm){
        ModeManager.instance = mm;
    }
    
    public static ModeManager getInstance(){
        if(instance == null){
            throw new RuntimeException("An attempt was done to call getInstance() on ModeSwitcher without calling init(mm) first.");
        }
        return instance;
    }
    
    /**
     * Call this method when a change on the calendar playlist has been done.
     */
    public void notifyCalendarChange(){
        Thread t = new Thread(calendarMode);
        t.start();
    }

    public void changeToCalendarMode(){
        // TODO: implement
    }

    public void changeToLiveMode(){
        // TODO: implement
    }
}
