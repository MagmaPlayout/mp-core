package playoutCore.dataStore;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.util.ArrayList;
import redis.clients.jedis.Jedis;

/**
 * This class is responsible for getting the required information from the redis store.
 * 
 * @author rombus
 */
public class RedisStore implements DataStore {
    private final Jedis server;

    public RedisStore(Jedis server){
        this.server = server;
    }

    @Override
    public ArrayList<String> getPlaylistClips(String id) {
        String jsonString = server.get(id);
        JsonElement data = new JsonParser().parse(jsonString);
        JsonArray clips = data.getAsJsonObject().getAsJsonArray("clips");

        ArrayList<String> pathList = new ArrayList<>();
        for(JsonElement clip: clips){
            String path = clip.getAsJsonObject().getAsJsonPrimitive("path").toString();
            pathList.add(path);
        }
        
        return pathList;
    }

    @Override
    public int getPlaylistLength() {
        //TODO hardcoded unit;
        String UNIT = "U0";
        return Integer.parseInt(server.get(UNIT));
    }

    @Override
    public void incrementPlaylistLength() {
        //TODO hardcoded unit;
        String UNIT = "U0";
        server.incr(UNIT);
    }

    @Override
    public void resetPlaylist(){
        //TODO hardcoded unit;
        String UNIT = "U0";
        server.set(UNIT, "0");
    }
}
