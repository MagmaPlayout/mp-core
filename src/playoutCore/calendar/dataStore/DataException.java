package playoutCore.calendar.dataStore;

/**
 * This exception is thrown whenever an error occurs on the DataStore.
 * 
 * @author rombus
 */
public class DataException extends RuntimeException{
    public DataException(String msg){
        super(msg);
    }
}
