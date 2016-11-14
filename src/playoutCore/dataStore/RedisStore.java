package playoutCore.dataStore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.dataStore.dataStructures.Clip;
import redis.clients.jedis.Jedis;

/**
 * This class is responsible for getting the required information from the redis store.
 * 
 * @author rombus
 */
public class RedisStore implements DataStore {
    private static final String CLIPS_KEY = "clips";    // Json KEY that stores the list of clips
    private static final String PATH_KEY = "path";      // Json KEY that stores the path of the current clip
    private static final String FILTER_JSON_KEY = "idFilter";// Json KEY that stores the id of the filter of the current clip
    private static final String FILTER_LIST_KEY = "filterList"; // Json KEY that stores all the filters available
    private static final String FILTER_HTML_KEY = "htmlCode";   // Json KEY that stores the actual filter data
    private static final String DURATION_KEY = "duration";      // Json KEY that stores the duration of the clip
    private static final String FRAMELEN_KEY = "frames";        // Json KEY that stores the length of the clip in frames
    private static final String FPS_KEY = "fps";                // Json KEY that stores the fps of the clip


    private final Jedis server;
    private final Logger logger;

    public RedisStore(Jedis server, Logger logger){
        this.server = server;
        this.logger = logger;
    }

    @Override
    public ArrayList<Clip> getPlaylistClips(String playlistId) throws DataException{
        ArrayList<Clip> pathList = null;
        HashMap<String, String> hm = (HashMap<String, String>)server.hgetAll("playlist:"+playlistId);
        String jsonString = hm.get(CLIPS_KEY);

        try{
            JsonArray clips = new JsonParser().parse(jsonString).getAsJsonArray();

            pathList = new ArrayList<>();
            for(JsonElement clip: clips){
                JsonObject curObj = clip.getAsJsonObject();

                String path = curObj.getAsJsonPrimitive(PATH_KEY).toString();
                String filterId = curObj.getAsJsonPrimitive(FILTER_JSON_KEY).toString().replace("\"", "");
                Duration duration = Duration.parse(curObj.getAsJsonPrimitive(DURATION_KEY).toString().replace("\"", ""));
                int frameLen = curObj.getAsJsonPrimitive(FRAMELEN_KEY).getAsInt();
                int fps = curObj.getAsJsonPrimitive(FPS_KEY).getAsInt();

                if(filterId.isEmpty()){
                    pathList.add(new Clip(path, duration));
                }
                else {
                    pathList.add(new Clip(path, duration, frameLen, fps, Integer.parseInt(filterId)));
                }
            }
        }
        catch(NullPointerException e){
            throw new DataException("Playout Core - Could not fetch a required key from the clips data store.");
        }
        
        return pathList;
    }

    @Override
    public String getFilter(int filterId) {
        String jsonString = server.lindex(FILTER_LIST_KEY, filterId);
        String filterString = null;

        try{
            JsonObject data = (JsonObject) new JsonParser().parse(jsonString);
            filterString = data.get(FILTER_HTML_KEY).toString();
        }
        catch(NullPointerException e){
            logger.log(Level.SEVERE, "RedisStore - Couldn't parse response.");
        }

        return filterString;
    }
}
