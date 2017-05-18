package playoutCore.dataStore;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.logging.Logger;
import playoutCore.dataStore.dataStructures.Clip;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;


/**
 * This class is responsible for getting the required information from the rest db store.
 * 
 * @author rombus
 */
// Unused? check in the future...
@Deprecated
public class RestStore implements DataStore {
    private static final String CLIPS_KEY = "clips";    // Json KEY that stores the list of clips
    private static final String PATH_KEY = "path";      // Json KEY that stores the path of the current clip
    private static final String FILTER_JSON_KEY = "idFilter";// Json KEY that stores the id of the filter of the current clip
    private static final String FILTER_LIST_KEY = "filterList"; // Json KEY that stores all the filters available
    private static final String FILTER_HTML_KEY = "htmlCode";   // Json KEY that stores the actual filter data
    private static final String DURATION_KEY = "duration";      // Json KEY that stores the duration of the clip
    private static final String FRAMELEN_KEY = "frames";        // Json KEY that stores the length of the clip in frames
    private static final String FPS_KEY = "fps";                // Json KEY that stores the fps of the clip


    private String baseUrl;
    private final Resty resty;
    private final Logger logger;

    public RestStore(String baseUrl, Logger logger){
        this.resty = new Resty();
        this.logger = logger;
        this.baseUrl = baseUrl;
    }

    
    @Override
    public ArrayList<Clip> getPlaylistClips(String playlistId) throws DataException{
        ArrayList<Clip> pathList = null;
//        HashMap<String, String> hm = (HashMap<String, String>)server.hgetAll("playlist:"+playlistId);
//        String jsonString = hm.get(CLIPS_KEY);
//
//        try{
//            JsonArray clips = new JsonParser().parse(jsonString).getAsJsonArray();
//
//            pathList = new ArrayList<>();
//            for(JsonElement clip: clips){
//                JsonObject curObj = clip.getAsJsonObject();
//
//                String path = curObj.getAsJsonPrimitive(PATH_KEY).toString();
//                String filterId = curObj.getAsJsonPrimitive(FILTER_JSON_KEY).toString().replace("\"", "");
//                Duration duration = Duration.parse(curObj.getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));
//                int frameLen = curObj.getAsJsonPrimitive(FRAMELEN_KEY).getAsInt();
//                int fps = curObj.getAsJsonPrimitive(FPS_KEY).getAsInt();
//
//                if(filterId.isEmpty()){
//                    pathList.add(new Clip(path, duration));
//                }
//                else {
//                    pathList.add(new Clip(path, duration, frameLen, fps, Integer.parseInt(filterId)));
//                }
//            }
//        }
//        catch(NullPointerException e){
//            throw new DataException("Playout Core - Could not fetch a required key from the clips data store.");
//        }
//        catch(IllegalStateException e){
//            throw new DataException("Playout Core - Clip data corrupted.");
//        }
        
        return pathList;
    }

    @Override
    public String getFilter(int filterId) {
//        String jsonString = server.lindex(FILTER_LIST_KEY, filterId);
        String filterString = null;

//        try{
//            JsonObject data = (JsonObject) new JsonParser().parse(jsonString);
//            filterString = data.get(FILTER_HTML_KEY).toString();
//        }
//        catch(NullPointerException e){
//            logger.log(Level.SEVERE, "RedisStore - Couldn't parse response.");
//        }

        return filterString;
    }

    @Override
    public Clip getClip(String id) {
        Clip result = null;
        
        try {
            JSONObject jsonRes = resty.json(baseUrl+"medias/"+id).toObject();
            boolean hasFilter = (jsonRes.get("filterId") != null);
            String path = (String) jsonRes.get("path");
            Duration duration = Duration.parse( ((String)jsonRes.get("duration")).replace("\"", ""));

            if(path == null || duration == null){
                // TODO: tirar una excepción controlada, no una runtime
                throw new RuntimeException("No path or duration");
            }


            if(!hasFilter){
//                result = new Clip(path, duration);
            }
            else {
                int frameLen = Integer.parseInt((String)jsonRes.get("frameLen"));
                int fps  = Integer.parseInt((String)jsonRes.get("fps"));
                int filterId  = Integer.parseInt((String)jsonRes.get("filterId"));

//                result = new Clip(path, duration, frameLen, fps, filterId);
            }
        } catch (IOException ex) {

        } catch (JSONException ex) {
            // Si la response del server no es json entra acá
        }

        return result;
    }
}
