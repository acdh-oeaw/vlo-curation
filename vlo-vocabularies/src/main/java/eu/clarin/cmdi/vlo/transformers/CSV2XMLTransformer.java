package eu.clarin.cmdi.vlo.transformers;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.xml.bind.JAXBException;


/**
 * @author dostojic
 *
 */

public interface CSV2XMLTransformer {
	
	
	/**
	 * Transforms csv file to xml vocabulary used for metadata curation
	 * 
	 * @param csv - CSV as input stream to be transformed
	 * @param xml - Output stream where new xml will be stored
	 * @throws JAXBException 
	 * @throws IOException 
	 */
	public void csv2xml(InputStream csv, OutputStream xml) throws IOException, JAXBException;
	
	
	/**
	 * ransforms csv file to xml vocabulary used for metadata curation
	 * 
	 * @param csv - name of the csv file, file will be fetched from maps/csv path relative to this project. XML file will be created in the maps/uniform_maps with the same name as csv
	 * @throws JAXBException 
	 * @throws IOException 
	 */
	public void csv2xml(String csv) throws IOException, JAXBException;
	

}
