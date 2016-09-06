package eu.clarin.cmdi.vlo.importer;

import java.util.LinkedList;
import java.util.List;

public class OrganisationPostProcessor extends PostProcessorsWithVocabularyMap{
	 
    /**
     * Splits values for organisation facet at delimiter ';' and replaces
     * organisation name variants with their official name from a controlled
     * vocabulary
     *
     * @param value extracted organisation name/names
     * @return List of organisation names (splitted at semicolon) and variations
     * replaced with controlled vocabulary
     */
    @Override
    public List<String> process(String value) {
        String[] splitArray = normalizeInputString(value).split(";");
        List<String> result = new LinkedList<>();
        for (int i = 0; i < splitArray.length; i++) {        	
        	List<String> normalizedVals = normalize(splitArray[i]);
        	if(normalizedVals != null){
        		for(String normalizedVal: normalizedVals)
        			if(normalizedVal != null)//skip nulls
        				result.add(normalizedVal);
        	}
        }
        
        return result;
    }
    
	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getOrganisationNamesUrl();
	}
    
    private String normalizeInputString(String value) {
        return value.replaceAll("\\s+", " ");
    }
    
    private String normalizeVariant(String key) {
        return key.toLowerCase().replaceAll("-", " ");
    }
}