package playoutCore.calendar.dataStructures;

import java.time.Duration;
import java.time.ZonedDateTime;

/**
 * Represents an instance of a piece in melted's playlist.
 * 
 * @author rombus
 */
public class Occurrence {
    public ZonedDateTime startDateTime;
    public ZonedDateTime endDateTime;
    public String path;
    public Duration len;
    public int frameCount;  // Length in frames
    public int frameRate;   // FPS

    public Occurrence(){}

    public Occurrence(ZonedDateTime startDateTime, ZonedDateTime endDateTime, String path, Duration len, int frameLen, int fps) {
        this.startDateTime = startDateTime;
        this.endDateTime = endDateTime;
        this.path = path;
        this.len = len;
        this.frameCount = frameLen;
        this.frameRate = fps;
    }
}
