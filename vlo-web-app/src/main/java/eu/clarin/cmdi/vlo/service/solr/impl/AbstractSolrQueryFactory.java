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
package eu.clarin.cmdi.vlo.service.solr.impl;

import eu.clarin.cmdi.vlo.pojo.FacetSelection;
import eu.clarin.cmdi.vlo.pojo.FacetSelectionValueQualifier;
import eu.clarin.cmdi.vlo.pojo.QueryFacetsSelection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.util.ClientUtils;

/**
 *
 * @author twagoo
 */
public abstract class AbstractSolrQueryFactory {

    protected static final String SOLR_SEARCH_ALL = "*:*";

    protected final void addQueryFacetParameters(final SolrQuery query, QueryFacetsSelection queryFacetsSelections) {
        final String queryString = queryFacetsSelections.getQuery();
        if (queryString == null) {
            query.setQuery(SOLR_SEARCH_ALL);
        } else {
            // escape query content and wrap in quotes to make literal query
            query.setQuery(queryString);
        }
        final Map<String, FacetSelection> selections = queryFacetsSelections.getSelection();
        if (selections != null) {
            final List<String> encodedQueries = new ArrayList(selections.size()); // assuming every facet has one selection, most common scenario
            for (Map.Entry<String, FacetSelection> selectionEntry : selections.entrySet()) {
                final String facetName = selectionEntry.getKey();
                final FacetSelection selection = selectionEntry.getValue();
                if (selection != null) {
                    switch (selection.getSelectionType()) {
                        case NOT_EMPTY:
                            //TODO: test
                            encodedQueries.add(String.format("%s:[* TO *]", facetName));
                            break;
                        case AND:
                            for (String value : selection.getValues()) {
                                if (selection.getQualifier(value) == FacetSelectionValueQualifier.NOT) {
                                    encodedQueries.add(createNegativeFilterQuery(facetName, value));
                                } else {
                                    encodedQueries.add(createFilterQuery(facetName, value));
                                }
                            }
                            break;
                        case OR:
                            // notice that OR ignores qualifiers, so it does not support e.g. (A OR (NOT B))
                            encodedQueries.add(createFacetOrQuery(facetName, selection.getValues()));
                            // replace facet field with version prefixed with exclude statement 
                            // (see <http://wiki.apache.org/solr/SimpleFacetParameters#Multi-Select_Faceting_and_LocalParams>)
                            query.removeFacetField(facetName);
                            query.addFacetField(String.format("{!ex=%1$s}%1$s", facetName));
                            break;
                        default:
                            throw new UnsupportedOperationException("Unsupported selection type: " + selection.getSelectionType());
                    }
                }
            }
            query.setFilterQueries(encodedQueries.toArray(new String[encodedQueries.size()]));
        }
    }

    private String createFilterQuery(String facetName, String value) {
        // escape value and wrap in quotes to make literal query
        return createFilterQuery("%s:\"%s\"", facetName, value);
    }

    private String createNegativeFilterQuery(String facetName, String value) {
        // escape value and wrap in quotes to make literal query, prepend negator
        return createFilterQuery("-%s:\"%s\"", facetName, value);
    }

    private String createFilterQuery(String _formatString, String facetName, String value) {
        return String.format(_formatString, facetName, ClientUtils.escapeQueryChars(value));
    }

    /**
     * Creates an OR filter query over the provided facet/value pairs (a query
     * that requests all records matching ANY of the facet/value pairs)
     *
     * @param facetValues map with facet/value pairs that should be matched
     * @return
     */
    protected final String createFilterOrQuery(Map<String, String> facetValues) {
        // escape value and wrap in quotes to make literal query
        final StringBuilder queryBuilder = new StringBuilder();
        final Iterator<Map.Entry<String, String>> iterator = facetValues.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<String, String> facetValue = iterator.next();
            queryBuilder.append(createFilterQuery(facetValue.getKey(), facetValue.getValue()));
            if (iterator.hasNext()) {
                queryBuilder.append(" OR ");
            }
        }
        return queryBuilder.toString();
    }

    /**
     * Creates an OR filter query for a single facet for a number of values
     *
     * @param facetName facet that should be matched
     * @param values allowed values
     * @return
     */
    private String createFacetOrQuery(String facetName, Collection<String> values) {
        // escape value and wrap in quotes to make literal query
        // prefix field name with tag statement (see <http://wiki.apache.org/solr/SimpleFacetParameters#Multi-Select_Faceting_and_LocalParams>)
        final StringBuilder queryBuilder = new StringBuilder(String.format("{!tag=%1$s}%1$s", facetName)).append(":(");
        // loop over values
        final Iterator<String> iterator = values.iterator();
        while (iterator.hasNext()) {
            final String value = iterator.next();

            queryBuilder.append(ClientUtils.escapeQueryChars(value));

            // add 'OR' connector except for last token
            if (iterator.hasNext()) {
                queryBuilder.append(" OR ");
            }
        }
        queryBuilder.append(")");
        return queryBuilder.toString();
    }

}
