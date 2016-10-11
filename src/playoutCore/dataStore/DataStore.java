package playoutCore.dataStore;

import java.util.ArrayList;

/**
 *
 * @author rombus
 */
public interface DataStore {
    ArrayList<String> getPlaylistClips(String id);
    int getPlaylistLength();
    void incrementPlaylistLength();
    void resetPlaylist();
    
    /*
    TODO: complete with stuff.
    */
}
