package eu.clarin.cmdi.vlo.importer;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import eu.clarin.cmdi.vlo.StringUtils;

public class ResourceClassPostProcessor extends PostProcessorsWithVocabularyMap {
	
	/**
     * Postprocess ResourceClass values
     * @param value extracted ResourcClass information
     * @return Value with some normalisation
     */
	
    @Override
    public List<String> process(String value) {
        String result = value;
        
        result.trim();
        
        // replace DCMI URLs with DCMI type
        result = result.replaceFirst("http://purl.org/dc/dcmitype/", "");
        
        String normalised = normalize(result);
        List<String> normalisedValues = new LinkedList<>();
        
        //all values are trimmed and first letter is capitalised
        if(normalised != null){
        	Arrays.stream(normalised.split(";")).forEach(nVal -> normalisedValues.add(StringUtils.capitalizeFirstLetter(nVal.trim())));
        }else{
        	normalisedValues.add(StringUtils.capitalizeFirstLetter(result));
        }
        
        return normalisedValues;
    }

	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getResourceClassMapUrl();
	}
	
}
