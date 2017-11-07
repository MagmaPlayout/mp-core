package playoutCore.modeSwitcher;

import java.util.ArrayList;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.calendar.CalendarMode;
import playoutCore.dataStructures.Clip;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpFactory;
import playoutCore.playoutApi.PlayoutApi;
import playoutCore.producerConsumer.CommandsExecutor;

/**
 * This class is responsible for changing between live mode and calendar mode.
 * 
 * @author rombus
 */
public class ModeManager {
    private enum Mode {LIVE_MODE, CALENDAR_MODE};
    private static Mode curMode;
    private static ModeManager instance;
    private final CalendarMode calendarMode;
    private final Logger logger;

    /**
     * This class manages the different modes of MP (live mode and calendar mode at the moment).
     * You need to call init(ms) before calling this in order to setup the static instance.
     * 
     * @param mvcpFactory
     * @param cmdFactory
     * @param cmdExecutor
     * @param scheduler
     * @param logger 
     */
    public ModeManager(MvcpCmdFactory mvcpFactory, PccpFactory cmdFactory, CommandsExecutor cmdExecutor, Scheduler scheduler, Logger logger){
        this.logger = logger;
        calendarMode = new CalendarMode(
            PlayoutApi.getInstance(),
            mvcpFactory,
            cmdFactory,
            cmdExecutor,
            scheduler,
            logger
        );
        curMode = Mode.CALENDAR_MODE; // Defaults to calendar mode
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
        if(curMode == Mode.CALENDAR_MODE){
            Thread t = new Thread(calendarMode);
            t.start();
        }
    }

    public void changeToCalendarMode(){
        curMode = Mode.CALENDAR_MODE;
        calendarMode.setTolerateInBetween(true);
        notifyCalendarChange();
    }

    public void changeToLiveMode(ArrayList<Clip> clips){
        curMode = Mode.LIVE_MODE;
        calendarMode.switchToLiveMode(clips);
    }
}
