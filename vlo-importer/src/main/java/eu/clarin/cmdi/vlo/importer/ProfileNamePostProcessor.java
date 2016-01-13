package eu.clarin.cmdi.vlo.importer;

import java.lang.reflect.Array;
import java.util.Arrays;
import java.util.List;

public class ProfileNamePostProcessor extends PostProcessorsWithVocabularyMap{

	@Override
	public List<String> process(String value) {
		// doesnt do anything
		return Arrays.asList(value);
	}

	
	
	@Override
	public String getNormalizationMapURL() {
		return MetadataImporter.config.getProfileNameMapUrl();
	}

}
