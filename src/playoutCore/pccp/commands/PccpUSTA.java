package playoutCore.pccp.commands;

import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import java.util.logging.Level;
import java.util.logging.Logger;
import meltedBackend.common.MeltedCommandException;
import meltedBackend.responseParser.responses.UstaResponse;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;

/**
 * This command returns information obtained by the USTA command (unit status).
 * 
 * @author rombus
 */
public class PccpUSTA extends PccpCommand {

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        throw new UnsupportedOperationException("This command does not implement the execute method.");
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory factory) {
        JsonObject result = new JsonObject();
        String unit = "U0"; //TODO: hxc unit

        try {
            UstaResponse response = (UstaResponse)(factory.getUsta(unit).exec());
            int curFrame = response.getPlayingClipFrame();
            int len = response.getPlayingClipLength();
            float fps = response.getPlayingClipFPS();

            
            result.add("curFrame", new JsonPrimitive(curFrame));
            result.add("len", new JsonPrimitive(len));
            result.add("fps", new JsonPrimitive(fps));

        } catch (MeltedCommandException ex) {
            Logger.getLogger(PccpUSTA.class.getName()).log(Level.SEVERE, null, ex);
            return result;
        }

        return result;
    }
}
