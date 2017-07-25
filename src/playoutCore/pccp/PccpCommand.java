package playoutCore.pccp;

import com.google.gson.JsonObject;
import java.time.Duration;
import playoutCore.dataStructures.Clip;
import static playoutCore.dataStructures.JsonClip.DURATION_KEY;
import static playoutCore.dataStructures.JsonClip.FILTER_ID_KEY;
import static playoutCore.dataStructures.JsonClip.FPS_KEY;
import static playoutCore.dataStructures.JsonClip.FRAME_LEN_KEY;
import static playoutCore.dataStructures.JsonClip.PATH_KEY;
import static playoutCore.dataStructures.JsonClip.PIECE_KEY;
import static playoutCore.dataStructures.JsonClip.PLAYLIST_IDX_KEY;
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

    public abstract boolean execute(MvcpCmdFactory meltedCmdFactory);

    public abstract JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory);

    /**
     * Receives a JsonObject that needs to have a single Clip representation.
     * For parsing multiple clips call this function inside a loop.
     *
     * @param args jsonObject to parse as a clip
     * @return 
     */
    protected Clip getClipFromJsonArg(JsonObject args){
        //try {
//            String path = args.getAsJsonPrimitive(PATH_KEY).toString();
//            Duration duration = Duration.parse(args.getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));
            JsonObject piece = args.getAsJsonObject(PIECE_KEY);
            String path = piece.getAsJsonPrimitive(PATH_KEY).toString();
            Duration duration = Duration.parse(piece.getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));

            //TODO: el filterId está deprecated???
            int filterId = Clip.NO_FILTER;
            if(piece.getAsJsonPrimitive(FILTER_ID_KEY) != null){
                filterId = piece.getAsJsonPrimitive(FILTER_ID_KEY).getAsInt();
            }

            // TODO: definir bien el JSON que vienen en args
            int frameLen = piece.getAsJsonPrimitive(FRAME_LEN_KEY).getAsInt();
            int fps = piece.getAsJsonPrimitive(FPS_KEY).getAsInt();

            int plIdx = this.args.getAsJsonPrimitive(PLAYLIST_IDX_KEY).getAsInt();

            return new Clip(path, duration, frameLen, fps, filterId, plIdx);
        //} catch(NullPointerException e){
            //TODO: cachear la null pointer
        //}
    }

    public Duration getLength(){
        return Duration.parse(args.getAsJsonObject(PIECE_KEY).getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));
    }
}
