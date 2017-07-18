package playoutCore.calendar;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.List;
import libconfig.ConfigurationManager;

/**
 *
 * @author rombus
 */
public class SpacerGenerator {
    private static final String SPACER_TEMPLATE_PATH = "../../templates/spacer.mpmlt";
    private static final String IMAGE_MLT_SERVICE = "pixbuf";
    private static final int IMAGE_FPS = 10;
    private final String defaultMediaPath;
    private static int ctr = 0;

    public SpacerGenerator(){
        defaultMediaPath = ConfigurationManager.getInstance().getDefaultMediaPath();
    }

    /**
     * Replaces the template objects with it's corresponding data.
     * 
     * @param line source for searching labels and replace them
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
     * Generates a spacer clip with a pixbuf mlt_service of the given duration.
     *
     * @param length duration of the spacer
     * @return path of the generated .mlt spacer
     */
    public String generateImageSpacer(Duration length){
        try {
            String path = "spacer"+ctr+".mlt"; //TODO: define name and path
            PrintWriter pw = new PrintWriter(path);
            int durationInFrames = (int)length.getSeconds() * IMAGE_FPS;            
            ctr++;

            List<String> lines = Files.readAllLines(Paths.get(SPACER_TEMPLATE_PATH));
            for(String line: lines){
                pw.println(processLine(line, defaultMediaPath, IMAGE_MLT_SERVICE, IMAGE_FPS, durationInFrames, durationInFrames));
            }
            pw.close();

            return path;
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
}
