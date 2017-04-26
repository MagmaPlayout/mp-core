package playoutCore.dataStore;

import java.util.ArrayList;
import playoutCore.dataStore.dataStructures.Clip;

/**
 *
 * @author rombus
 */
public interface DataStore {
    Clip getClip(String id);
    ArrayList<Clip> getPlaylistClips(String id) throws DataException;
    String getFilter(int id);
}
