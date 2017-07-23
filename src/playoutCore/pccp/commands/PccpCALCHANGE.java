package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.quartz.Scheduler;
import playoutCore.modeSwitcher.ModeManager;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 *  This command notifies mp-core that the calendar shcedule changed.
 * 
 * @author rombus
 */
public class PccpCALCHANGE extends PccpCommand {
    private final Logger logger;

    public PccpCALCHANGE(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger){
        super(args);
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        logger.log(Level.INFO, "Playout Core - notification of calendar change...");
        ModeManager.getInstance().notifyCalendarChange();
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }
}
