package playoutCore.dataStore.dataStructures.rest;

import com.google.gson.annotations.Expose;
import com.google.gson.annotations.SerializedName;

/**
 *
 * @author rombus
 */
public class RestMedia {
    @SerializedName("path")
    @Expose
    public String path;
}
