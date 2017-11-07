package playoutCore.calendar;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import playoutCore.PlayoutCore;
import playoutCore.calendar.dataStructures.Occurrence;

/**
 * This class is responsible for creating the .mlt files that will act as melted "spacers" (filling empty time with a default media).
 * It also has methods for detecting empty spaces between occurrences.
 * 
 * @author rombus
 */
public class SpacerGenerator {
    private static SpacerGenerator instance = new SpacerGenerator();
    private static String spacer_template_path;
    private static final String IMAGE_MLT_SERVICE = "pixbuf";
    private final int imageFPS;
    private final String defaultMediaPath, spacersPath;
    private static int ctr = 0;
    private final Logger logger;
    private final ConfigurationManager cfgMgr;

    public static SpacerGenerator getInstance(){
        return instance;
    }

    private SpacerGenerator(){
        //TODO: poner en la config
        spacer_template_path = Paths.get(".").toAbsolutePath().normalize().toString();
        if(spacer_template_path.endsWith("dist")){
            spacer_template_path +="/../templates/spacer.mpmlt";
        }
        else {
            spacer_template_path +="/templates/spacer.mpmlt";
        }
        
        cfgMgr = ConfigurationManager.getInstance();
        defaultMediaPath = cfgMgr.getDefaultMediaPath();
        spacersPath = cfgMgr.getMltSpacersPath(); // TODO: create default path on installation
        logger = Logger.getLogger(PlayoutCore.class.getName());
        imageFPS = ConfigurationManager.getInstance().getMediasFPS();
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

        deleteOldSpacers(); // Clean spacers dir from old spacers
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
                    len++;
                    i++; // Make the loop skip the added spacer in it's next iteration
                }
            }
        }
        
        return occurrences;
    }


    /**
     * Generates a spacer clip with a pixbuf mlt_service of the given duration.
     *
     * @param startDateTime can be null
     * @param endDateTime can be null
     * @param length duration of the spacer
     * @return path of the generated .mlt spacer or null if duration is 0
     */
    public Occurrence generateImageSpacer(ZonedDateTime startDateTime, ZonedDateTime endDateTime, Duration length){
        // If duration is 0 then return null
        if(!(length.get(ChronoUnit.SECONDS) > 0)){
            logger.log(Level.INFO, "SpacerGenerator - The length to generate a spacer is 0. No spacer will be generated.");
            return null;
        }

        try {
            String path = spacersPath+"spacer"+ctr+".mlt"; //TODO: define name and path
            PrintWriter pw = new PrintWriter(path);
            int durationInFrames = (int)length.getSeconds() * imageFPS;
            ctr++;

            List<String> lines = Files.readAllLines(Paths.get(spacer_template_path));
            for(String line: lines){
                pw.println(processLine(line, defaultMediaPath, IMAGE_MLT_SERVICE, imageFPS, durationInFrames, durationInFrames));
            }
            pw.flush();
            pw.close();

            return new Occurrence(startDateTime, endDateTime, path, length, durationInFrames, imageFPS);
        } catch (IOException ex) {
            //TODO: handle, fatal exception??
            ex.printStackTrace();
        }

        logger.log(Level.INFO, "SpacerGenerator - No spacer will be generated.");
        return null;
    }

    /**
     * Generates a spacer clip with the duration between startDateTime and endDateTime.
     * 
     * @param calendarStarts calendarStarts
     * @param curMediaEndTime   curMediaEndTime
     * @return
     */
    public Occurrence generateImageSpacer(ZonedDateTime calendarStarts, ZonedDateTime curMediaEndTime){
        return generateImageSpacer(calendarStarts, curMediaEndTime, Duration.of(ChronoUnit.SECONDS.between(curMediaEndTime, calendarStarts), ChronoUnit.SECONDS));
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

    /**
     * Deletes all spacers that are older than the playlist max length.
     * It's called every time that a CALCHANGE command is issued.
     */
    private void deleteOldSpacers() {
        long olderThan = LocalDateTime.now().minus(cfgMgr.getMeltedPlaylistMaxDuration(), ChronoUnit.MINUTES).toEpochSecond(ZoneOffset.UTC);
        Path path = Paths.get(spacersPath);
        try {
            Files.list(path).filter(
                (curFile) -> {
                    try {
                        return Files.getLastModifiedTime(curFile).to(TimeUnit.SECONDS) < olderThan;
                    } catch (IOException ex) {
                        return false;
                    }
                }
            ).forEach(curFile -> {
                try {
                    Files.delete(curFile);
                } catch (IOException ex) {
                    logger.log(Level.WARNING, "SpacerGenerator - Couldn't delete old spacer: "+curFile.getFileName());
                }
            });
        } catch (IOException ex) {
            logger.log(Level.SEVERE, "SpacerGenerator - Couldn't list spacers path: "+spacersPath);
        }
    }

    private int getLastSpacerIdx(){
        // TODO: este método devuelve cual es el siguiente ID a usar,
        // va a servir cuando se levante el sistema después de un shutdown y los spacers estén guardados en la BD
        return 1;
    }
}
