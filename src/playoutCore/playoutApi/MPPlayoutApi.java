package playoutCore.playoutApi;

import java.util.ArrayList;
import playoutCore.calendar.dataStructures.Occurrence;
import playoutCore.filter.dataStructures.Filter;

/**
 * Interface to mp-playout-api rest module.
 * Used to manage calendar and filter features.
 * 
 * @author rombus
 */
public interface MPPlayoutApi {
    ArrayList<Occurrence> getAllOccurrences();
    Filter getFilterArguments(int filterId);
}
