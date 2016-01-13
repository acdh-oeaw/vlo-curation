package eu.clarin.cmdi.vlo.main;

import java.io.FileNotFoundException;
import java.io.IOException;

import javax.xml.bind.JAXBException;

import eu.clarin.cmdi.vlo.pojo.Constants;
import eu.clarin.cmdi.vlo.transformers.CSVTransformer;

public class TransformAllMaps {
	
	public static void main(String[] args) throws FileNotFoundException, JAXBException, IOException {
		
		new CSVTransformer().csv2xml(Constants.RESOURCE_TYPE);
		
		
//		CSVTransformer transformer = new CSVTransformer();
//		for(String map: Constants.maps)
//			transformer.xml2csv(map);
		
	}

}
