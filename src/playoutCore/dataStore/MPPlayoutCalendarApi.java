package playoutCore.dataStore;

import java.util.ArrayList;

/**
 * Interface to mp-playout-api module for use to manage the calendar feature.
 * 
 * @author rombus
 */
public interface MPPlayoutCalendarApi {
    ArrayList<String> getAllOccurrences();
    //TODO: en la api un método que sea delAllSpacers ?
}
