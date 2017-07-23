package playoutCore.calendar;

import java.util.ArrayList;
import playoutCore.dataStore.MPPlayoutCalendarApi;

/**
 *
 * @author rombus
 */
public class CalendarMode implements Runnable{
    private MPPlayoutCalendarApi api;

    public CalendarMode(MPPlayoutCalendarApi api) {
        this.api = api;
    }



    @Override
    public void run() {
        ArrayList<String> oc = api.getAllOccurrences();

        // Ask data to playout-api
        // for all data
        //      genSpacers();
        // send clearAll();
        // for all data
        //      send APND
    }
}
