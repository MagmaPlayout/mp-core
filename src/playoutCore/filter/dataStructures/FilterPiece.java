package playoutCore.filter.dataStructures;

import java.util.ArrayList;

/**
 * This class represents a Piece that has (one or many) associated filters with their respective arguments.
 * 
 * @author rombus
 */
public class FilterPiece {
    public int pieceId;
    public String mltPath;
    private ArrayList<Filter> filters;
    
    public FilterPiece(){
        this.filters = new ArrayList<>();
    }
    
    public void addFilter(Filter filter){
        this.filters.add(filter);
    }
    
    public ArrayList<Filter> getFilters(){
        return this.filters;
    }
}
