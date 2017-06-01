package playoutCore.pccp;

import com.google.gson.JsonObject;
import java.time.Duration;
import playoutCore.dataStore.DataStore;
import playoutCore.dataStore.dataStructures.Clip;
import static playoutCore.dataStore.dataStructures.JsonClip.DURATION_KEY;
import static playoutCore.dataStore.dataStructures.JsonClip.FILTER_ID_KEY;
import static playoutCore.dataStore.dataStructures.JsonClip.FPS_KEY;
import static playoutCore.dataStore.dataStructures.JsonClip.FRAME_LEN_KEY;
import static playoutCore.dataStore.dataStructures.JsonClip.PATH_KEY;
import static playoutCore.dataStore.dataStructures.JsonClip.PLAYLIST_IDX_KEY;
import playoutCore.mvcp.MvcpCmdFactory;

/**
 *
 * @author rombus
 */
public abstract class PccpCommand {
    public JsonObject args;

    public PccpCommand(){
        
    }
    
    public PccpCommand(JsonObject args){
        this.args = args;
    }

    public void setArgs(JsonObject args){
        this.args = args;
    }

    public abstract boolean execute(MvcpCmdFactory meltedCmdFactory, DataStore store);

    public abstract JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory, DataStore store);

    /**
     * Receives a JsonObject that needs to have a single Clip representation.
     * For parsing multiple clips call this function inside a loop.
     *
     * @param args jsonObject to parse as a clip
     * @return 
     */
    protected Clip getClipFromJsonArg(JsonObject args){
        //TODO: handle exceptions of malformed jsonClip
        String path = args.getAsJsonPrimitive(PATH_KEY).toString();
        Duration duration = Duration.parse(args.getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));
        
        int filterId = Clip.NO_FILTER;
        if(args.getAsJsonPrimitive(FILTER_ID_KEY) != null){
            filterId = args.getAsJsonPrimitive(FILTER_ID_KEY).getAsInt();
        }
        int frameLen = args.getAsJsonPrimitive(FRAME_LEN_KEY).getAsInt();
        int fps = args.getAsJsonPrimitive(FPS_KEY).getAsInt();

        int plIdx = this.args.getAsJsonPrimitive(PLAYLIST_IDX_KEY).getAsInt();

        return new Clip(path, duration, frameLen, fps, filterId, plIdx);
    }
}
