package playoutCore.pccp.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.modeSwitcher.ModeManager;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command notifies mp-core that the calendar shcedule changed.
 * 
 * @author rombus
 */
public class PccpSWITCHMODE extends PccpCommand {
    private static final int CALENDAR_MODE = 0;
    private static final int LIVE_MODE = 1;
    private static final String MODE_KEY = "mode";
    private static final String MEDIA_LIST = "mediaList";
    private final Logger logger;

    public PccpSWITCHMODE(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        int mode = args.getAsJsonPrimitive(MODE_KEY).getAsInt();
        logger.log(Level.INFO, "Playout Core - A switch mode action to "+ ((mode==CALENDAR_MODE)?"CALENDAR":"LIVE") +"has been requested.");

        switch(mode){
            case CALENDAR_MODE:
                ModeManager.getInstance().changeToCalendarMode();
                break;
            case LIVE_MODE:
                ArrayList<Occurrence> occurrences = new ArrayList<>();
                JsonArray mediaList = args.getAsJsonArray(MEDIA_LIST);

                for(JsonElement media: mediaList){
                    //TODO: load occurrences list with the specified medias on the SWITCHMODE PCCP command
                }

                ModeManager.getInstance().changeToLiveMode(occurrences);
                break;
        }

        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
