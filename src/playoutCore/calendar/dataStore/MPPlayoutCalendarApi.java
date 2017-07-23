package playoutCore.calendar.dataStore;

import java.util.ArrayList;
import playoutCore.calendar.dataStructures.Occurrence;

/**
 * Interface to mp-playout-api module for use to manage the calendar feature.
 * 
 * @author rombus
 */
public interface MPPlayoutCalendarApi {
    ArrayList<Occurrence> getAllOccurrences();
    //TODO: en la api un método que sea delAllSpacers ?
}
