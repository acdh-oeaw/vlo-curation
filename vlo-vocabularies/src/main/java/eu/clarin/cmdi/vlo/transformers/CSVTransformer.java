package eu.clarin.cmdi.vlo.transformers;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.xml.bind.JAXBException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.clarin.cmdi.vlo.pojo.Constants;
import eu.clarin.cmdi.vlo.pojo.CrossMapping;
import eu.clarin.cmdi.vlo.pojo.Mapping;
import eu.clarin.cmdi.vlo.pojo.Variant;
import eu.clarin.cmdi.vlo.pojo.VariantsMap;

/**
 * @author dostojic
 *
 */

public class CSVTransformer implements CSV2XMLTransformer, XML2CSVTransformer {
	
	private final static Logger _logger = LoggerFactory.getLogger(CSVTransformer.class);
	
	//for tests
	private int csvRowCnt = 0;
	private int csvNormValsCnt = 0;
	
	private int xmlRowCnt = 0; //count of variants or just NormalizedVal if variants are missing
	private int xmlNormValsCnt = 0;	
		
	public synchronized void xml2csv(InputStream xml, OutputStream csv) throws JAXBException, IOException{
		_logger.info("starting with conversion from xml to csv ..."); 
		
		csv.write(xml2csvTransformer(VariantsMapMarshaller.unmarshal(xml)).getBytes());
		csv.flush();
	}	
	
	public synchronized void xml2csv(String map) throws FileNotFoundException, JAXBException, IOException{
		_logger.info("reading from file {}{}.xml.", Constants.MAPS_PATH, map);
		InputStream input = new FileInputStream(Constants.MAPS_PATH + map + ".xml");
		_logger.info("output will be saved into {}{}.txt.", Constants.CSV_PATH, map);
		OutputStream output = new FileOutputStream(Constants.CSV_PATH + map + ".txt");
		
		xml2csv(input, output);
		
		_logger.debug("closing streams ...");
		input.close();
		output.close();
		_logger.debug("streams are closed.");
		
	}
	
	public synchronized void csv2xml(InputStream csv, OutputStream xml) throws IOException, JAXBException{
		_logger.info("starting with conversion from csv to xml.");
		
		InputStreamReader isr = new InputStreamReader(csv);
		BufferedReader br = new BufferedReader(isr);
		VariantsMap vm = csv2xmlTransformer(br);
		VariantsMapMarshaller.marshal(vm, xml);
		
		_logger.debug("closing streams ...");
		br.close();
		isr.close();
		_logger.debug("streams are closed.");
	}
	
	
	public synchronized void csv2xml(String map) throws IOException, JAXBException{
		_logger.info("reading from file {}{}.txt.", Constants.CSV_PATH, map);
		InputStream input = new FileInputStream(Constants.CSV_PATH + map + ".txt");
		_logger.info("output will be saved into {}{}.xml.", Constants.MAPS_PATH, map);
	    OutputStream output = new FileOutputStream(Constants.MAPS_PATH + map + ".xml");
	    
	    csv2xml(input, output);
	    
		input.close();
		output.close();	    
	}
		    
 
    private String xml2csvTransformer(VariantsMap map) {
		List<List<String>> values = new ArrayList<List<String>>();
		boolean regExpresionExists = false;
		
		List<String> columnNames = new ArrayList<String>();
		columnNames.add(map.getField());
		columnNames.add(Constants.REGULAR_EXPRESSION);
		columnNames.add(Constants.COUNT);
		columnNames.add(Constants.NORMALIZED_VALUE);
		
		for(Mapping mapping: map.getMappings()){
			xmlNormValsCnt++;
			
			_logger.debug("new mapping was found, normalized value is {}", mapping.getValue());
			if(mapping.getVariants().isEmpty()){
				xmlRowCnt++;
				_logger.warn("Mapping for {} doesn't have any variants!", mapping.getValue());
				String[] rowArray = new String[columnNames.size()];
				List<String> row = new ArrayList<String>(Arrays.asList(rowArray));
				//add normalized val
				row.set(3, mapping.getValue());
				values.add(row);
			}
			else{
				for(Variant var: mapping.getVariants()){
					xmlRowCnt++;
					_logger.debug("new varinat was found, variant value is {}. ", var.getValue());
					//create new row and init with nulls
					String[] rowArray = new String[columnNames.size()];
					List<String> row = new ArrayList<String>(Arrays.asList(rowArray));
					//add orig val
					row.set(0, var.getValue());
					//add regEx if it's true
					if(var.isRegExp()){
						row.set(1, "true");
						regExpresionExists = true;
					}
					//add normalized val
					row.set(3, mapping.getValue());
					
					//add cross mappings
					if(var.getCrossMappings() != null){
						for(CrossMapping cm: var.getCrossMappings()){
							_logger.debug("new cross-mapping was found {}:{} ", cm.getFacet(), cm.getValue());
							int ind = columnNames.indexOf(cm.getFacet());
							if(ind > -1)//if exists
								row.set(ind, cm.getValue());
							else{//first occurrence? add the facet name and new val at the end of the list
								columnNames.add(cm.getFacet());
								row.add(cm.getValue());
							}
						}
					}
					values.add(row);
				}
			}
			
			_logger.info("Mapping for {} has {} variants.", mapping.getValue(), mapping.getVariants().size());
		}
				
		StringBuilder sb = new StringBuilder();
		//add column names in the first row
		for(String columnName: columnNames){
			
			if(!regExpresionExists && columnName.equals(Constants.REGULAR_EXPRESSION))//skip regEx column if there are no regular expressions in this map
				continue;
			sb.append(columnName.toUpperCase()).append("\t");
		}
		_logger.info("CSV file will contain following headers {}", sb.toString());
		sb.append("\n");
			
		//add values
		_logger.info("CSV has {} rows.", values.size());
		for(List<String> row: values){
			for(int i = 0; i < columnNames.size(); i++){
				if(!regExpresionExists && i == 1)//skip regEx column if there are no regular expressions in this map
					continue;					
				sb.append(i >= row.size() || row.get(i) == null ? "" : row.get(i)).append("\t");
			}
			sb.append("\n");
		}
		
		return sb.toString();
		
	}
    
    private VariantsMap csv2xmlTransformer(BufferedReader br) throws IOException{
    	boolean regExpresionExists = false;
    	boolean remarksExists = false;
    	VariantsMap map = new VariantsMap();
    	map.setMappings(new ArrayList<Mapping>());
    	
    	Map<String, Mapping> processedNormalizedVals = new TreeMap<String, Mapping>();
    	List<String> columnNames = new ArrayList<String>();
    	
    	String line = br.readLine();
    	try{
    		//process column names	
        	if(!line.contains(Constants.NORMALIZED_VALUE))
        		throw new RuntimeException("The first row of CSV file must contain column names");
        	_logger.info("CSV file contains following headers {}", line);
        	
        	for(String col: line.split("\\t")){    	
        		if(col.toUpperCase().equals(Constants.REMARKS)){
        			remarksExists = true;
        			break;//last column - don't need it in xml map
        		}
        		
        		if(col.toUpperCase().equals(Constants.REGULAR_EXPRESSION))
        			regExpresionExists = true;
        		columnNames.add(col.toLowerCase());
        		
        	}    	
        	    	
        	while ((line = br.readLine()) != null) {
        		csvRowCnt++;
        		if(line.trim().isEmpty() || line.startsWith("//"))
        			continue;
        		while(!remarksExists && line.charAt(line.length() - 1) != '\t'){//line must end with a tab, otherwise means that value contains line break -> &#xA;
        			_logger.debug("line {} contains line break!", csvRowCnt);
        			line += "\n" + br.readLine();
        		}
        			
        		String[] tokens = line.split("\\t");
        		if(remarksExists && tokens.length == columnNames.size() + 1){ //remove last col (remarks)
        			String[] tokenstWithoutRemarks = new String[tokens.length - 1];
        			System.arraycopy(tokens, 0, tokenstWithoutRemarks, 0, tokenstWithoutRemarks.length);
        			tokens = tokenstWithoutRemarks;
        		}
        		
        		String normalizedVal = regExpresionExists? tokens[3] : tokens[2];
				//strip qoutes
				if((normalizedVal.startsWith("\"") || normalizedVal.startsWith("'")) && (normalizedVal.endsWith("\"") || normalizedVal.endsWith("'")) && normalizedVal.length() > 1){
					normalizedVal = normalizedVal.substring(1, normalizedVal.length() - 1);
				}
        		
        		if(normalizedVal.isEmpty())
        			throw new RuntimeException("Normalized value (the 4th column) is mandatory. You can comment the line with //");    		
        		    		
        		Variant var = null;
        		
        		if(!tokens[0].isEmpty()){ // Organization vocab has normalized values without variants
        			var = new Variant();
        			var.setValue(tokens[0]);
        			_logger.debug("new varinat was found, variant value is {}. ", var.getValue());
            		
            		//we are setting regEx attr only if it's true
            		if(regExpresionExists && tokens[1].toLowerCase().equals("true"))
            			var.setRegExp(true);
            		List<CrossMapping> crossMappings = new ArrayList<CrossMapping>();
            		for(int i = regExpresionExists? 4 : 3; i < tokens.length; i++){
            			if(tokens[i].isEmpty()) //skip when val is null (empty)
            				continue;
            			
            			CrossMapping crossMapping = new CrossMapping();
            			crossMapping.setFacet(columnNames.get(i));
            			crossMapping.setValue(tokens[i]);
            			crossMappings.add(crossMapping);
            			_logger.debug("new cross-mapping was found {}:{} ", crossMapping.getFacet(), crossMapping.getValue());
            		}//end cross-mappings
            		
            		if(crossMappings.size() > 0)
            			var.setCrossMappings(crossMappings);
        		}
        		
        		Mapping mapping = processedNormalizedVals.get(normalizedVal);
        		if(mapping == null){
        			mapping = new Mapping();        			
        			mapping.setValue(normalizedVal);
        			processedNormalizedVals.put(normalizedVal, mapping);
        			_logger.debug("new mapping was found, normalized value is {}", mapping.getValue());
        			csvNormValsCnt++;
        		}
        		if(var != null){
        			mapping.getVariants().add(var);
        		}else{
        			mapping.setVariants(null); //to skip it in XML
        		}
        		
        		
     	    }//end while
        	
        	map.setField(columnNames.get(0));
        	for(String s: processedNormalizedVals.keySet()){
        		map.getMappings().add(processedNormalizedVals.get(s));
        	}
    	}catch(Exception e){
    		_logger.error("Error on line {}", line, e);
    	}
    	
    	

    	
    	return map;
    	
    }

	public int getCsvRowCnt() {
		return csvRowCnt;
	}

	public int getCsvNormValsCnt() {
		return csvNormValsCnt;
	}

	public int getXmlRowCnt() {
		return xmlRowCnt;
	}

	public int getXmlNormValsCnt() {
		return xmlNormValsCnt;
	}


 

}
