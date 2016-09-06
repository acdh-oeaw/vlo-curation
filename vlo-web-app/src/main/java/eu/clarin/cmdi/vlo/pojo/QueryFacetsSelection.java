/*
 * Copyright (C) 2014 CLARIN
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package eu.clarin.cmdi.vlo.pojo;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.google.common.collect.Maps;

/**
 * Represents a query and any number of selected values for zero or more facets
 *
 * @author twagoo
 */
public class QueryFacetsSelection implements Serializable {

    private String queryString;
    private final Map<String, FacetSelection> selection;
    
    
    //multi selection for single facet
    private String excludedFacet = null;

    /**
     * Creates an empty selection (no string, no facet values)
     */
    public QueryFacetsSelection() {
        this(null, Maps.<String, FacetSelection>newHashMap());
    }

    /**
     * Creates a selection with an empty facet selection
     *
     * @param query query string
     */
    public QueryFacetsSelection(String query) {
        this(query, Maps.<String, FacetSelection>newHashMap());
    }

    /**
     * Creates a selection without a query
     *
     * @param selection facet values selection map
     */
    public QueryFacetsSelection(Map<String, FacetSelection> selection) {
        this(null, selection);
    }

    /**
     * Creates a selection with a textual query and facet value selection
     *
     * @param query textual query (can be null)
     * @param selection facet values selection map (can be null)
     */
    public QueryFacetsSelection(String query, Map<String, FacetSelection> selection) {
        this.queryString = query;
        if (selection == null) {
            this.selection = new HashMap<String, FacetSelection>();
        } else {
            this.selection = selection;
        }
    }

    /**
     *
     * @return a facet -> values map representing the current selection
     */
    public Map<String, FacetSelection> getSelection() {
        return selection;
    }

    /**
     *
     * @return the facets present in the current selection
     */
    public Collection<String> getFacets() {
        return selection.keySet();
    }

    /**
     *
     * @param facet facet to get values for
     * @return the selected values for the specified facet. Can be null.
     */
    public FacetSelection getSelectionValues(String facet) {
        return selection.get(facet);
    }

    /**
     *
     * @return the current textual query, may be null in case of no query
     */
    public String getQuery() {
        return queryString;
    }

    public void setQuery(String queryString) {
        this.queryString = queryString;
    }

    
    public void selectValues(String facet, FacetSelection values) {
    	// allow multi value selection for single facet only if user has selected one value
        if (values == null || values.isEmpty()) {
            selection.remove(facet);
            
            switch(selection.size()){
            case 0:
            	excludedFacet = null;
            	break;
            case 1:
            	excludedFacet = selection.keySet().iterator().next(); //set it to the only element
            	break;
            }
        } else {
            if (values instanceof Serializable) {
            	selection.put (facet, values);
            } else {
            	selection.put (facet, values);
            }
            
            if(selection.size() == 1)
            	excludedFacet = facet;
            else
            	excludedFacet = null;
        }
    }
    
    public void addNewFacetValue(String facet, Collection<String> values){
    	FacetSelection curSel = selection.get(facet);
    	if(curSel != null){
    		curSel.mergeValues(values);
    	}else{
    		curSel = new FacetSelection(values);    		
    	}
    		
    	selectValues(facet, curSel);    	
    }
    
    public void removeFacetValue(String facet, Collection<String> valuestoBeRemoved){
    	FacetSelection curSel = selection.get(facet);
    	if (curSel != null){
    		curSel.removeValues(valuestoBeRemoved);    		 		
    	}
    	//to remove facet from map if does not have any value
    	selectValues(facet, curSel);
    }

    @Override
    public String toString() {
        return String.format("[QueryFacetSelection queryString = %s, selection = %s]", queryString, selection);
    }

    public QueryFacetsSelection getCopy() {
        final Map<String, FacetSelection> selectionClone = new HashMap<String, FacetSelection>(selection.size());
        for (Entry<String, FacetSelection> entry : selection.entrySet()) {
            selectionClone.put(entry.getKey(), entry.getValue().getCopy());
        }
        return new QueryFacetsSelection(queryString, selectionClone);
    }

	public String getExcludedFacet() {
		return excludedFacet;
	}

	public void setExcludedFacet(String excludedFacet) {
		this.excludedFacet = excludedFacet;
	}
    
    

}