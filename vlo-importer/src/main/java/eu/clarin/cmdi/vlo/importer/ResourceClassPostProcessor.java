package eu.clarin.cmdi.vlo.importer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import eu.clarin.cmdi.vlo.Plural2Singular;
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
        
        // replace DCMI URLs with DCMI type
        result = result.replaceFirst("http://purl.org/dc/dcmitype/", "");

        result.trim();
        
        // first letter should be upper case
        if(result.length() > 1) {
           result = StringUtils.capitalizeFirstLetter(value);
        }
		
        result = normalize(value);
            
		return Arrays.asList(result);
    }

	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getResourceClassMapUrl();
	}
}
