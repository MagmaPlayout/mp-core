package playoutCore.dataStore;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.logging.Logger;
import playoutCore.dataStore.dataStructures.Clip;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;


/**
 * This class interacts with mp-playout-api rest module.
 * 
 * @author rombus
 */
public class CalendarApi implements MPPlayoutCalendarApi {
    private static final String OCCURRENCES_PATH = "occurrences/";
    private final String baseUrl;
    private final Resty resty;
    private final Logger logger;

    public CalendarApi(String baseUrl, Logger logger){
        this.resty = new Resty();
        this.logger = logger;
        this.baseUrl = baseUrl;
    }

    
     @Override
    public ArrayList<String> getAllOccurrences() {
        ArrayList<String> occurrences = new ArrayList<>();

        try {
            JSONObject jsonRes = resty.json(baseUrl+OCCURRENCES_PATH).toObject();
            JSONObject piece = jsonRes.getJSONObject("piece");

            ZonedDateTime startDateTime = ZonedDateTime.parse(jsonRes.getString("startDateTime"));
            Duration duration = Duration.parse(piece.getString("duration"));
            ZonedDateTime endDateTime = startDateTime.plus(duration.toMinutes(), ChronoUnit.MINUTES);
            
            String sourcePath = piece.getString("path");

            if(duration == null){
                // TODO: tirar una excepción controlada, no una runtime
                throw new RuntimeException("No path or duration");
            }

            
        } catch (IOException ex) {

        } catch (JSONException ex) {
            // Si la response del server no es json entra acá
            // TODO: handle
            ex.printStackTrace();
        }
        
        return occurrences;
    }

    
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
