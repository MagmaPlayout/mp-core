package playoutCore.playoutApi;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import playoutCore.calendar.dataStructures.JsonOccurrence;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.filter.dataStructures.Filter;
import playoutCore.filter.dataStructures.JsonFilteredPiece;
import us.monoid.json.JSONArray;
import us.monoid.json.JSONException;
import us.monoid.json.JSONObject;
import us.monoid.web.Resty;


/**
 * This class interacts with mp-playout-api rest module.
 * It's used by the calendar package.
 * 
 * @author rombus
 */
public class PlayoutApi implements MPPlayoutApi {
    private static final String OCCURRENCES_PATH = "occurrences/";
    @Deprecated private final String FILTER_ARGS_PATH = "filterArgs/";
    
    private static PlayoutApi instance;
    private final String baseUrl;
    private final Resty resty;
    private final Logger logger;

    public static void init(String baseUrl, Logger logger){
        instance = new PlayoutApi(baseUrl, logger);
    }
    
    public static PlayoutApi getInstance(){
        if(instance == null){
            throw new RuntimeException("PlayoutApi getInstance() - You need to call init() on PlayoutApi before calling getInstance()!");
        }
        
        return instance;
    }

    private PlayoutApi(String baseUrl, Logger logger){
        this.resty = new Resty();
        this.logger = logger;
        this.baseUrl = baseUrl;
    }

    /**
     * Makes a rest get request to the OCCURRENCE_PATH of mp-playout-api to get all the configured occurrences.
     * With that info it creates a list of Occurrence objects.
     * 
     * @return the list of occurrence objects.
     */
    @Override
    public ArrayList<Occurrence> getAllOccurrences() {
        ArrayList<Occurrence> occurrences = new ArrayList<>();

        try {
            JSONArray jsonOccurrences = resty.json(baseUrl+OCCURRENCES_PATH).array();
            int len = jsonOccurrences.length();
            ZonedDateTime now = ZonedDateTime.now();

            // Iterate over every occurrence creating Occurrence objects
            for(int i=0; i<len; i++){
                JSONObject curOccurrence = jsonOccurrences.getJSONObject(i);
                JSONObject piece = curOccurrence.getJSONObject(JsonOccurrence.PIECE_KEY);
                
                ZonedDateTime startDateTime = ZonedDateTime.parse(curOccurrence.getString(JsonOccurrence.START_DATE_TIME_KEY));
                Duration duration = Duration.parse(piece.getString(JsonOccurrence.DURATION_KEY));
                ZonedDateTime endDateTime = startDateTime.plus(duration.toMillis(), ChronoUnit.MILLIS);


                if(duration == null){
                    // TODO: tirar una excepción controlada, no una runtime y ver mejor como handlear esta situacion
                    throw new RuntimeException("No path or duration");
                }

                // If the media ended after now, there's no point on adding it to the occurrences list
                if(endDateTime.isAfter(now)){
                    occurrences.add(
                        new Occurrence(
                            startDateTime, endDateTime,
                            piece.getString(JsonOccurrence.PATH_KEY),
                            duration, piece.getInt(JsonOccurrence.FRAME_LEN_KEY),
                            piece.getInt(JsonOccurrence.FPS_KEY)
                        )
                    );
                }
            }
            
        } catch (IOException ex) {
            // TODO: handle
            // MALFORMED URL (por ej)
            ex.printStackTrace();
        } catch (JSONException ex) {
            // Si la response del server no es json entra acá
            // TODO: handle
            ex.printStackTrace();
        }
        
        return occurrences;
    }

    
    /**
     * Makes a rest get request to the FILTER_ARGS_PATH of mp-playout-api to get all the key value filter arguments.
     *
     * @param filterName the value for the "mlt_service" key
     * @param filterArgsId filterArgsId to search arguments
     * @return the filter containing a Map with the key value arguments.
     */
    @Override
    @Deprecated
    public Filter getFilterArguments(String filterName, int filterArgsId) {
        Filter filter = new Filter();
        filter.addKeyValue("mlt_service", filterName); // The most important argument, defines what filter is it.

        try {
            JSONArray jsonOccurrences = resty.json(baseUrl+FILTER_ARGS_PATH+filterArgsId).array();
            int len = jsonOccurrences.length();

            // Iterate over every occurrence creating Occurrence objects
            for(int i=0; i<len; i++){
                JSONObject curArg = jsonOccurrences.getJSONObject(i);
                String key = curArg.getString(JsonFilteredPiece.KEY);
                String value = curArg.getString(JsonFilteredPiece.VALUE);

                filter.addKeyValue(key, value);
            }
        } catch (IOException ex) {
            //TODO: handle
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        } catch (JSONException ex) {
            //TODO: handle
            Logger.getLogger(PlayoutApi.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return filter;
    }
}
