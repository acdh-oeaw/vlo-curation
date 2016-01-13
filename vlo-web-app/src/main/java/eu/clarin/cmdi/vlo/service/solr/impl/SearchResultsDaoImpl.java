package eu.clarin.cmdi.vlo.service.solr.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.response.FacetField;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.cmdi.vlo.config.VloConfig;
import eu.clarin.cmdi.vlo.service.solr.SearchResultsDao;

public class SearchResultsDaoImpl extends SolrDaoImpl implements SearchResultsDao {

    private final static Logger logger = LoggerFactory.getLogger(SearchResultsDaoImpl.class);

    public SearchResultsDaoImpl(SolrServer solrServer, VloConfig config) {
        super(solrServer, config);
    }

    
    @Override
    public List<FacetField> getFacets(SolrQuery... queries) {
    	List<FacetField> result = null;
    	
    	    	
    	for(SolrQuery query: queries){
    		final List<FacetField> response = fireQuery(sanitise(query)).getFacetFields();
    		
    		if(result == null){
    			result = response;
    			continue;
    		}
    		
    		List<FacetField> temp = new ArrayList<FacetField>(result);
    		
    		//merge result lists
    		for(FacetField facetNew: response){
    			boolean facetExists = false;
    			Integer ind = null;
    			FacetField mergedFacet = null;
    			for(FacetField facetOld: temp){
    				if(facetNew.getName().equals(facetOld.getName())){
    					//facet exists, merge values
    					facetExists = true;
    					mergedFacet = copyFacetField(facetOld);
    	        		ind = response.indexOf(facetNew);
    	        		for(FacetField.Count facetValNew: facetNew.getValues()){
    						boolean valueExists = false;
        	        		for(FacetField.Count facetValOld: facetOld.getValues()){
    	        				if(facetValNew.getName().equals(facetValOld.getName())){
    	        					valueExists = true;
    	        					break;
    	        				}
    	        			}
    	        			//import missing values into the facet from the list and set count to zero
    	        			if(!valueExists){
    	        				logger.debug("Missing value {} is added with count 0", facetValNew.getName());
    	        				mergedFacet.add(facetValNew.getName(), 0);
    	        			}
    					}
    	        		
    	        		result.remove(ind);
        				result.add(ind, mergedFacet);
    	        		
    				}    				
    			}
    			//if facet doesn't already exist in result list, add it
    			if(!facetExists){
    				result.add(facetNew);
    			}    			
    		}
    	}
    	
    	return result;
    }
    
    
    private FacetField copyFacetField(FacetField from){
    	FacetField newFacet = new FacetField(from.getName(), from.getGap(), from.getEnd());
    	for(FacetField.Count val: from.getValues())
    		newFacet.add(val.getName(), val.getCount());
    	
    	return newFacet;
    }
    

    @Override
    public SolrDocumentList getDocuments(SolrQuery query) {
        QueryResponse queryResponse = fireQuery(query);
        final SolrDocumentList documents = queryResponse.getResults();
        logger.debug("Found {} documents", documents.getNumFound());
        return documents;
    }
    
}
