package playoutCore.filter.dataStructures;

import java.util.LinkedHashMap;

/**
 * A filter model for the mp-playout-api
 * 
 * @author rombus
 */
public class Filter {
    public int filterId;
    public LinkedHashMap<String, String> keyValues;

    public Filter(){
        keyValues = new LinkedHashMap<>(16, 0.75f, false);
    }

    public void addKeyValue(String key, String value){
        keyValues.put(key, value);
    }
}
