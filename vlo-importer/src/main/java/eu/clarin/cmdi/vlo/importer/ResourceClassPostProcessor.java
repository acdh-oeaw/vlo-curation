package eu.clarin.cmdi.vlo.importer;

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
        
        // replace DCMI URLs with DCMI type
        result = result.replaceFirst("http://purl.org/dc/dcmitype/", "");

        result.trim();
        
        // first letter should be upper case
        if(result.length() > 1) {
           result = StringUtils.capitalizeFirstLetter(value);
        }
		
        //return modified value in case that value is not in vocabulary
        return normalize(result, result);
    }

	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getResourceClassMapUrl();
	}
}
