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
    public final int frameLen;
    public final int fps;

    public Clip(String path, Duration len){
        this.path = path;
        this.len = len;
        this.frameLen = 0;  // If the clip has no filter, then I don't care about it's frameLen. I'm only using this to generate the .mlt of filtered media
        this.fps = 0;       // Same as frameLen, don't care about the fps if the clip is unfiltered.
        this.filterId = NO_FILTER;
    }

    public Clip(String path, Duration len, int frameLen, int fps, int filterId){
        this.path = path;
        this.len = len;
        this.filterId = filterId;
        this.frameLen = frameLen;
        this.fps = fps;
    }
}
