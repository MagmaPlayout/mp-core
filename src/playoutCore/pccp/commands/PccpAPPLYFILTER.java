package playoutCore.pccp.commands;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import libconfig.ConfigurationManager;
import org.quartz.Scheduler;
import playoutCore.filter.dataStructures.Filter;
import playoutCore.filter.dataStructures.FilterPiece;
import playoutCore.filter.dataStructures.JsonFilteredPiece;
import playoutCore.mvcp.MvcpCmdFactory;
import playoutCore.pccp.PccpCommand;
import playoutCore.playoutApi.PlayoutApi;
import redis.clients.jedis.Jedis;

/**
 * This command creates a new .mlt file with the specified filter loaded.
 *
 * @author rombus
 */
public class PccpAPPLYFILTER extends PccpCommand {
    private final Logger logger;

    public PccpAPPLYFILTER(JsonObject args, Jedis publisher, String fscpChannel, Scheduler scheduler, Logger logger) {
        super(args);
        this.logger = logger;
    }

    @Override
    public boolean execute(MvcpCmdFactory factory) {
        try {
            logger.log(Level.INFO, "PccpCREATEPIECE - A create piece command has been requested.");
            PlayoutApi api = PlayoutApi.getInstance();
            String fromPath = args.getAsJsonPrimitive(JsonFilteredPiece.FROM).toString().replace("\"", "");
            
            // Parse piece JSON object
            JsonObject pieceObj = args.getAsJsonObject(JsonFilteredPiece.PIECE_OBJ);
            FilterPiece piece = new FilterPiece();
            piece.pieceId = pieceObj.getAsJsonPrimitive(JsonFilteredPiece.ID).getAsInt();
            piece.mltPath = pieceObj.getAsJsonPrimitive(JsonFilteredPiece.MLT_PATH).getAsString();
            piece = parseFilters(piece, pieceObj);

            // Reads fromPath and writes into toPath adding the filtersXmlString in the tracktor tag
            File outputFile = new File(piece.mltPath);
            outputFile.createNewFile();

            PrintWriter pw = new PrintWriter(outputFile);
            List<String> lines = Files.readAllLines(Paths.get(fromPath));
            boolean insideTractorTag = false;
            for(String line: lines){
                // Set's the title attribute to be the pieceId
                if(line.contains("<mlt ")){
                    line = line.replaceFirst("title=[\"][\\S]*[\"]", "title=\""+String.valueOf(piece.pieceId)+"\"");
                }

                // If you're inside the tractor tag, then it's time to print the filters
                if(insideTractorTag){
                    insideTractorTag = false;
                    
                    // Loads all the filters data
                    ArrayList<Filter> filters = piece.getFilters();
                    for(Filter filter: filters){
                        StringBuffer filtersXmlString = getFiltersXmlString(filter);
                        pw.println(filtersXmlString);    
                    }
                }

                if(line.contains("<tractor ")){
                    insideTractorTag = true;
                }
                
                pw.println(line);
            }

            pw.flush();
            pw.close();
            
        } catch (FileNotFoundException ex) {
            Logger.getLogger(PccpAPPLYFILTER.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(PccpAPPLYFILTER.class.getName()).log(Level.SEVERE, null, ex);
        }
        
        return true;
    }

    @Override
    public JsonObject executeForResponse(MvcpCmdFactory meltedCmdFactory) {
        throw new UnsupportedOperationException("This command does not implement the executeForResponse method.");
    }

    
    /**
     * Creates a StringBuffer with the XML of all the filters needed.
     * This xml needs to be inside the tracktor tag.
     * 
     * @param filter
     * @return
     */
    private StringBuffer getFiltersXmlString(Filter filter) {
        LinkedHashMap<String, String> arguments = filter.keyValues;
        StringBuffer filters = new StringBuffer();
        int filterCount = 0;

        for (Map.Entry<String, String> pair : arguments.entrySet()) {
            String key = pair.getKey();
            String value = pair.getValue();

            // If it's a mlt_service then it needs it's own filter tag
            if (key.equals("mlt_service")) {
                if(filterCount++ > 0){
                    // Close previous filter tag
                    filters.append("\t\t</filter>\n");
                }
                
                filters.append("\t\t<filter id=\"filter").append(filterCount).append("\">\n");
            }

            filters.append("\t\t\t<property name=\"").append(key).append("\">").append(value).append("</property>\n");
        }
        filters.append("\t\t</filter>\n");

        return filters;
    }
    
    
    /**
     * Parses the piece JSON object into a FilterPiece object.
     * 
     * @param piece
     * @param pieceObj
     * @return 
     */
    private FilterPiece parseFilters(FilterPiece piece, JsonObject pieceObj) {
        //TODO reemplazar todos los JSONArray y JSONObject por JsonArray y JsonObject, así uso gson
        // Parse Filters
        TreeMap<Integer, Filter> orderedFilters = new TreeMap<>(); // Temp Map for storing filters while loading their arguments
        
        JsonArray filtersArr = pieceObj.getAsJsonArray(JsonFilteredPiece.FILTER_CONFIGS); // Array<FilterConfigModel>
        // Iterate over all the filterArgs and group them by filterIndex into the "orderedFilters" variable
        for(JsonElement filterElemen:filtersArr){
            JsonObject filterObj = filterElemen.getAsJsonObject();
            
            // Adds this filterArg to the orderedFilters list
            int idx = filterObj.get(JsonFilteredPiece.FILTER_INDEX).getAsInt();
            Filter curFilter = orderedFilters.get(idx);
            if(curFilter == null){
                // If this filter idx is not loaded on orderedFilters then I create it and put it there
                curFilter = new Filter();
                orderedFilters.put(idx, curFilter);
            }

            // Set's the curFilter key and value
            JsonObject filterArgsModel = filterObj.getAsJsonObject(JsonFilteredPiece.FILTER_ARG);
            String key = filterArgsModel.get(JsonFilteredPiece.KEY).getAsString();
            String value = filterObj.get(JsonFilteredPiece.VALUE).getAsString();
            curFilter.addKeyValue(key, value);
            
            // TODO: provisorio.
            // Esto es un hardcodeo para demostrar que el backend soporta filtros con argumentos
            // pero como falta la GUI para definirlos, los hardcodeo acá.
            if(value.equals("watermark")){
                curFilter.addKeyValue("resource","/opt/magma/watermark.png");
                curFilter.addKeyValue("factory","loader");
                curFilter.addKeyValue("composite.valign","bottom");
                curFilter.addKeyValue("composite.halign","right");
            }
            else if(value.equals("dynamictext")){
                curFilter.addKeyValue("argument","#localtime %H:%M#");
                curFilter.addKeyValue("geometry","0%/0%:100%x100%:100");
                curFilter.addKeyValue("family","freemono");
                curFilter.addKeyValue("size","22");
                curFilter.addKeyValue("weight","400");
                curFilter.addKeyValue("style" ,"normal");
                curFilter.addKeyValue("fgcolour","#FFFFFF");
                curFilter.addKeyValue("bgcolour","#00FFFFFF");
                curFilter.addKeyValue("olcolour","0x00000000");
                curFilter.addKeyValue("pad","0");
                curFilter.addKeyValue("halign","left");
                curFilter.addKeyValue("valign","bottom");
                curFilter.addKeyValue("outline","0");
                curFilter.addKeyValue("track","0");
            }
            
        }
        
        // Loads every Filter object into the piece
        Iterator it = orderedFilters.entrySet().iterator();
        while(it.hasNext()){
            Map.Entry<Integer, Filter> pair = (Map.Entry)it.next();
            piece.addFilter(pair.getValue());
        }
        
        return piece;
    }
}
