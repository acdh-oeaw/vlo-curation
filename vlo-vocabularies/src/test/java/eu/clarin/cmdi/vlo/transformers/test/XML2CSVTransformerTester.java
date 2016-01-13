package eu.clarin.cmdi.vlo.transformers.test; 

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import javax.xml.bind.JAXBException;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.cmdi.vlo.pojo.Constants;
import eu.clarin.cmdi.vlo.pojo.CrossMapping;
import eu.clarin.cmdi.vlo.pojo.Mapping;
import eu.clarin.cmdi.vlo.pojo.Variant;
import eu.clarin.cmdi.vlo.pojo.VariantsMap;
import eu.clarin.cmdi.vlo.transformers.CSVTransformer;
import eu.clarin.cmdi.vlo.transformers.VariantsMapMarshaller;

public class XML2CSVTransformerTester {
	
	private final static Logger _logger = LoggerFactory.getLogger(XML2CSVTransformerTester.class);
		
	private CSVTransformer transformers;
	
	@Before
	public void init() throws JAXBException, IOException{
		
		//Remove all csv files
		for(File csv: new File(Constants.TEST_CSV_PATH).listFiles())
			csv.delete();
		
		//Remove all xml files from xml
			for(File csv: new File(Constants.TEST_XML_PATH).listFiles())
				csv.delete();
	}
	

	/**
	 * transforms all vocabulary maps used in production and compares number of rows in xml and csv 
	 * @throws JAXBException
	 * @throws IOException
	 */
	
	@Test
	public void transformAndTestAll() throws JAXBException, IOException{
		for(String map: Constants.maps){
			
			transformers = new CSVTransformer();
			xml2csv(map);
			_logger.debug("---------------------------------------------------------");
			csv2xml(map);
			_logger.debug("---------------------------------------------------------");

			Assert.assertTrue("number of normalized values must be identical in xml and csv!", transformers.getXmlNormValsCnt() == transformers.getCsvNormValsCnt());
			Assert.assertTrue("number of rows in CSV (excluding headers)must be identical as numbers of variants in xml with exception when variant is missing, which counts as 1 variant.", transformers.getXmlRowCnt() == transformers.getCsvRowCnt());
			
		}		
	}
	
	@Test
	public void crossMapping() throws JAXBException, IOException{
		transformers = new CSVTransformer();
		
		xml2csv(Constants.CROSSMAPPING);
		csv2xml(Constants.CROSSMAPPING);
		
		Map<String, Integer> crossMapCount = new HashMap<String, Integer>();
		
		InputStream xml = new FileInputStream(new File(Constants.TEST_XML_PATH + File.separator + Constants.CROSSMAPPING + ".xml"));
		VariantsMap vm = VariantsMapMarshaller.unmarshal(xml);
		for(Mapping m: vm.getMappings()){
			_logger.info("mapping for {} has {} variants", m.getValue(), m.getVariants().size());
			for(Variant v: m.getVariants()){
				_logger.info("Variant {} has {} cross-mappings", v.getValue(), v.getCrossMappings() != null? v.getCrossMappings().size() : 0);
				crossMapCount.put(v.getValue(), v.getCrossMappings().size());
				for(CrossMapping cm: v.getCrossMappings()){
					_logger.info("Crossmapping {}:{}", cm.getFacet(), cm.getValue());
				}
			}
		}
		
		Assert.assertTrue("Varinat Software sohuld have 2 crossmappings", crossMapCount.get("Software") == 2);
		Assert.assertTrue("Varinat Tool sohuld have 3 crossmappings", crossMapCount.get("Tool") == 3);
		
	}
	
	
	private void xml2csv(String map) throws JAXBException, IOException{
		InputStream xml = new FileInputStream(new File(Constants.TEST_MAPS_PATH + File.separator + map + ".xml"));
		OutputStream csv = new FileOutputStream(new File(Constants.TEST_CSV_PATH + File.separator + map + ".csv"));
		
		_logger.info("converting {} map from xml to csv", map);

		transformers.xml2csv(xml, csv);
		
		xml.close();
		csv.close();
	}
	
	private void csv2xml(String map) throws JAXBException, IOException{
		InputStream csv = new FileInputStream(new File(Constants.TEST_CSV_PATH + File.separator + map + ".csv"));
		OutputStream xml = new FileOutputStream(new File(Constants.TEST_XML_PATH + File.separator + map + ".xml"));
		
		_logger.info("converting {} map from csv to xml", map);
		transformers.csv2xml(csv, xml);

		csv.close();
		xml.close();			
	}
	
}
