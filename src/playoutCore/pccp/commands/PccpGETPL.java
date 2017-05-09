package playoutCore.pccp.commands;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.ListResponse;
import playoutCore.dataStore.DataStore;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import redis.clients.jedis.Jedis;

/**
 * This command get's the playlist stored in Melted.
 * First it wipes the list, so you only get the current playing clip and those that will play next.
 * 
 * @author rombus
 */
public class PccpGETPL extends PccpCommand {
    private Jedis publisher;
    private String pcrChannel;

    public PccpGETPL(Jedis publisher, String pcrChannel){
        this.publisher = publisher;
        this.pcrChannel = pcrChannel;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory, DataStore store) {
        throw new UnsupportedOperationException("This command does not implement the execute method.");
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory factory, DataStore store) {
        JsonObject result = new JsonObject();
        String unit = "U0"; //TODO: hxc unit

        try {
            factory.getWipe(unit).exec();
            ListResponse response = (ListResponse)(factory.getList(unit).exec());
            String[] clipsPaths = response.getMeltedPlaylist();

            Gson gson = new Gson();
            result.add("opcode", new JsonPrimitive("GETPL"));
            result.add("result", new JsonPrimitive(gson.toJson(clipsPaths)));
            
            publisher.publish(pcrChannel, result.toString());
            //logger.log(Level.INFO, "A response was send to the PCR channel");
        } catch (MeltedCommandException ex) {
            Logger.getLogger(PccpGETPL.class.getName()).log(Level.SEVERE, null, ex);
            return result;
        }

        return result;
    }
}
