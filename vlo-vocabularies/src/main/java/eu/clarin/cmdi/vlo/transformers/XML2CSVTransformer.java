package eu.clarin.cmdi.vlo.transformers;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;

/**
 * @author dostojic
 *
 */

public interface XML2CSVTransformer {

	
	/**
	 * Transforms xml vocabulary to csv used by metedata curators
	 * 
	 * @param xml - input stream from xml vocabulary which will be transfored to csv
	 * @param csv - out stream where the new csv will be saved
	 * @throws IOException 
	 * @throws JAXBException 
	 */
	public void xml2csv(InputStream xml, OutputStream csv) throws JAXBException, IOException;
	
	/**
	 * * Transforms xml vocabulary to csv used by metedata curators
	 * 
	 * @param map - name of the xml file. File will be fetched from mmaps/uniform_maps path relative to this project. CSV file will be created in the maps/csv with the same name as xml map
	 * @throws IOException 
	 * @throws JAXBException 
	 * @throws FileNotFoundException 
	 */
	public void xml2csv(String map) throws FileNotFoundException, JAXBException, IOException;
}
