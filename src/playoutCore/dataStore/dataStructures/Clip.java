package playoutCore.dataStore.dataStructures;

import java.time.Duration;

/**
 * This class maps to a clip in a playlist position.
 *
 * @author rombus
 */
public class Clip {
    public static final int NO_FILTER = -1;
    public final String path;
    public final int filterId;
    public final Duration len;

    public Clip(String path, Duration len){
        this.path = path;
        this.len = len;
        this.filterId = NO_FILTER;
    }

    public Clip(String path, Duration len, int filterId){
        this.path = path;
        this.len = len;
        this.filterId = filterId;
    }
}
