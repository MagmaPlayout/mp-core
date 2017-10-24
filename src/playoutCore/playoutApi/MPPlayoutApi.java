package playoutCore.playoutApi;

import java.util.ArrayList;
import playoutCore.calendar.dataStructures.Occurrence;

/**
 * Interface to mp-playout-api rest module.
 * Used to manage calendar and filter features.
 * 
 * @author rombus
 */
public interface MPPlayoutApi {
    ArrayList<Occurrence> getAllOccurrences();
}
