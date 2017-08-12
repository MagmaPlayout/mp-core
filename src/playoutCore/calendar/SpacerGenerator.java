package playoutCore.calendar;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import libconfig.ConfigurationManager;
import playoutCore.calendar.dataStructures.Occurrence;

/**
 * This class is responsible for creating the .mlt files that will act as melted "spacers" (filling empty time with a default media).
 * It also has methods for detecting empty spaces between occurrences.
 * 
 * @author rombus
 */
public class SpacerGenerator {
    private static SpacerGenerator instance = new SpacerGenerator();
    private static final String SPACER_TEMPLATE_PATH = "templates/spacer.mpmlt";
    private static final String IMAGE_MLT_SERVICE = "pixbuf";
    private static final int IMAGE_FPS = 10;
    private final String defaultMediaPath, spacersPath;
    private static int ctr = 0;

    public static SpacerGenerator getInstance(){
        return instance;
    }

    private SpacerGenerator(){
        ConfigurationManager cfgMgr = ConfigurationManager.getInstance();
        defaultMediaPath = cfgMgr.getDefaultMediaPath();
        spacersPath = cfgMgr.getMltSpacersPath(); // TODO: create default path on installation
    }


    /**
     * Analizes the ordered list of occurrences to check for empty spaces between them.
     * If it detects an empty space a "spacer" will be generated.
     * 
     * TODO: here are some assumptions made about how two consecutive occurrences will be separated
     * in time by 1 second. If that's not the case it should be changed here.
     * 
     * @param occurrences ordered list of occurrences (ordered by startDateTime ascending)
     * @return the same list with needed spacers added to it
     */
    public ArrayList<Occurrence> generateNeededSpacers(ArrayList<Occurrence> occurrences){
        int len = occurrences.size();
        
        for(int i=0; i+1<len; i++){
            Occurrence cur = occurrences.get(i);
            Occurrence next = occurrences.get(i+1);

            long emptySeconds = ChronoUnit.SECONDS.between(cur.endDateTime, next.startDateTime);
            //TODO: I assume here that two consecutive occurrences will be 1 second appart.
            if(emptySeconds > 1){
                // TODO: I'm hardcoding here an Image Spacer, you may want a Media Spacer...
                Occurrence spacer = generateImageSpacer(
                        // endDate+1s indicates the next video of current
                        // startDate-1s indicates the previous video of next
                        // duration-2 indicates the new duration of an occurrence between (cur.endDate+1s) and (next.startDate-1s)
                        cur.endDateTime.plus(1, ChronoUnit.SECONDS),
                        next.startDateTime.minus(1, ChronoUnit.SECONDS),
                        Duration.of(emptySeconds-2, ChronoUnit.SECONDS)
                );

                if(spacer != null){
                    occurrences.add(i+1, spacer); // Adds the spacer between cur and next
                    i++; // Make the loop skip the added spacer in it's next iteration
                }
            }
        }
        
        return occurrences;
    }


    /**
     * Generates a spacer clip with a pixbuf mlt_service of the given duration.
     *
     * @param startDateTime
     * @param endDateTime
     * @param length duration of the spacer
     * @return path of the generated .mlt spacer
     */
    public Occurrence generateImageSpacer(ZonedDateTime startDateTime, ZonedDateTime endDateTime, Duration length){
        try {
            String path = spacersPath+"spacer"+ctr+".mlt"; //TODO: define name and path
            PrintWriter pw = new PrintWriter(path);
            int durationInFrames = (int)length.getSeconds() * IMAGE_FPS;            
            ctr++;

            List<String> lines = Files.readAllLines(Paths.get(SPACER_TEMPLATE_PATH));
            for(String line: lines){
                pw.println(processLine(line, defaultMediaPath, IMAGE_MLT_SERVICE, IMAGE_FPS, durationInFrames, durationInFrames));
            }
            pw.flush();
            pw.close();

            return new Occurrence(startDateTime, endDateTime, path, length, durationInFrames, IMAGE_FPS);
        } catch (IOException ex) {
            //TODO: handle, fatal exception??
            ex.printStackTrace();
        }
        return null;
    }

    public String generateVideoSpacer(){
        // TODO: Implement
        return "";
    }

    /**
     * Replaces the template objects, in a line, with it's corresponding data.
     * This methdo needs to be called in a loop with each line of the template mlt file.
     *
     * @param line source for searching labels and replace them (comes from the template .mlt file)
     * @param path  resource path
     * @param mltService pixbuf or avformat
     * @param fps   frames per second
     * @param framesLen duration of resource in frames
     * @param frameOut  las frame to reproduce in mlt playlist tag
     * @return
     */
    private String processLine(String line, String path, String mltService, int fps, int framesLen, int frameOut){
        line = line.replace("{pathname}", path);
        line = line.replace("{fps}", String.valueOf(fps));
        line = line.replace("{length-1}", String.valueOf(framesLen-1));
        line = line.replace("{length}", String.valueOf(framesLen));
        line = line.replace("{frame_out}", String.valueOf(frameOut));
        line = line.replace("{mlt_service}", mltService);

        return line;
    }
}
