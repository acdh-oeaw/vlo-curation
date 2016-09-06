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

import com.google.common.collect.Lists;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents the selection for a single facet
 *
 * @author twagoo
 */
public class FacetSelection implements Serializable {

    private FacetSelectionType selectionType;
    private final Collection<String> values;

    /**
     * Creates an {@link FacetSelectionType#AND} selection for the specified
     * values
     *
     * @param values
     */
    public FacetSelection(Collection<String> values) {
        this(FacetSelectionType.AND, values);
    }

    /**
     * Creates an empty selection with the specified type
     *
     * @param type
     */
    public FacetSelection(FacetSelectionType type) {
        this(type, Lists.<String>newArrayList());
    }

    public FacetSelection(FacetSelectionType selectionType, Collection<String> values) {
        this.selectionType = selectionType; //values.size() > 1? FacetSelectionType.OR : FacetSelectionType.AND;
        // always store as array list, which is modifiable and serialisable
        if (values instanceof ArrayList) {
            this.values = values;
        } else {
            // copy to new list
            this.values = Lists.newArrayList(values);
        }
    }

    /**
     *
     * @return type of selection
     */
    public FacetSelectionType getSelectionType() {
        return selectionType;
    }

    /**
     *
     * @return values subject to selection type
     */
    public Collection<String> getValues() {
        return values;
    }
    
    public void mergeValues(Collection<String> values){
    	if(values == null || values.isEmpty())
    		return;
    	for(String val: values)
    		if(!this.values.contains(val))
    			this.values.add(val);
    	
    	//change type of selection to OR if more values are selected
    	if(this.values.size() > 1)
    		this.selectionType = FacetSelectionType.OR;
    	
    }
    
    public void removeValues(Collection<String> valuesToBeRemoved){
    	if(valuesToBeRemoved != null){
	    	for(String val: valuesToBeRemoved){
	    		this.values.remove(val);
	    	}
    	}
    	
    	if(this.values.size() <= 1)
    		this.selectionType = FacetSelectionType.AND;
    }
    

    public FacetSelection getCopy() {
        return new FacetSelection(selectionType, values);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 79 * hash + (this.selectionType != null ? this.selectionType.hashCode() : 0);
        hash = 79 * hash + (this.values != null ? this.values.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final FacetSelection other = (FacetSelection) obj;
        if (this.selectionType != other.selectionType) {
            return false;
        }
        if (this.values != other.values && (this.values == null || !this.values.equals(other.values))) {
            return false;
        }
        return true;
    }

    /**
     *
     * @return whether this instance represents an actual selection
     */
    public boolean isEmpty() {
        return selectionType != FacetSelectionType.NOT_EMPTY // 'not empty' does not require any value, other types do
                && (values == null || values.isEmpty());
    }

    @Override
    public String toString() {
        return String.format("%s: %s", selectionType, values);
    }

}
