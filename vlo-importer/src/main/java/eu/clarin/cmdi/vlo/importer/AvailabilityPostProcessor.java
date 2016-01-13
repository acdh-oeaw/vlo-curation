package eu.clarin.cmdi.vlo.importer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 * @author teckart
 */
public class AvailabilityPostProcessor extends PostProcessorsWithVocabularyMap {
	
	private static final Integer MAX_LENGTH = 20;
    private static final String OTHER_VALUE = "Other";
    

    @Override
    public List<String> process(final String value) {
        List<String> resultList = new ArrayList<String>();
        
        resultList.add(normalize(value, value.length() > MAX_LENGTH? OTHER_VALUE : value.trim()));

        return resultList;
    }


	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getLicenseAvailabilityMapUrl();
	}
}
