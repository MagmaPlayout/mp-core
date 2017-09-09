package playoutCore.calendar.dataStore;

import java.io.IOException;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.logging.Logger;
import playoutCore.calendar.dataStructures.JsonOccurrence;
import playoutCore.calendar.dataStructures.Occurrence;
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
                
                occurrences.add(
                    new Occurrence(
                        startDateTime, endDateTime,
                        piece.getString(JsonOccurrence.PATH_KEY),
                        duration, piece.getInt(JsonOccurrence.FRAME_LEN_KEY),
                        piece.getInt(JsonOccurrence.FPS_KEY)
                    )
                );
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
}
