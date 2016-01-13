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
package eu.clarin.cmdi.vlo.service.solr;

import eu.clarin.cmdi.vlo.pojo.QueryFacetsSelection;
import java.util.List;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.response.FacetField;

/**
 * A service that retrieves facet fields based on pre-formulated SOLR query
 *
 * @author twagoo
 */
public interface FacetFieldsService {

    /**
     *
     * @param selection query and selected facet values
     * @param facets facets to include
     * @param valueLimit limits the number of values to retrieve per facet,
     * negative for unlimited (see {@link SolrQuery#getFacetLimit() })
     * @return facet field objects representing the state of all present facets
     */
    List<FacetField> getFacetFields(QueryFacetsSelection selection, List<String> facets, int valueLimit);

    /**
     *
     * @param facets facets to include
     * @return the total number of facets
     */
    long getFacetFieldCount(List<String> facets);
    
    
    /**
    *
    * @param facet
    * @return the total number of documents that don't have specified value for this facet
    */
    
	long getNullCount(String facet, QueryFacetsSelection selection);
}
