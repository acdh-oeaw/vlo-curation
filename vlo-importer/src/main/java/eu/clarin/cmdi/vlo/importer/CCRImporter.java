package eu.clarin.cmdi.vlo.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;

import eu.clarin.cmdi.vlo.FacetConstants;
import eu.clarin.cmdi.vlo.config.VloConfig;

public class CCRImporter implements CMDIDataProcessor{
	   
    
    //private static final Map<String, FacetMapping> profileXSDCache = new HashMap<String, FacetMapping>();
    
    private static final Set<String> indexedConcepts = new HashSet<String>();
    
    
    
    private final Map<String, PostProcessor> postProcessors;
    private final Boolean useLocalXSDCache;
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(".*(clarin.eu:cr1:p_[0-9]+).*");
    private final static Logger LOG = LoggerFactory.getLogger(CMDIParserVTDXML.class);

    private static final String DEFAULT_LANGUAGE = "code:und";
    private final VloConfig config;
    
    
    public CCRImporter(Map<String, PostProcessor> postProcessors, VloConfig config, Boolean useLocalXSDCache) {
        this.postProcessors = postProcessors;
        this.useLocalXSDCache = useLocalXSDCache;
        this.config = config;
    }
    
	

	@Override
	public CMDIData process(File file) throws Exception {
		CMDIData cmdiData = new CMDIData();
        VTDGen vg = new VTDGen();
        FileInputStream fileInputStream = new FileInputStream(file);
        
        try {
            vg.setDoc(IOUtils.toByteArray(fileInputStream));
            vg.parse(true);
        } finally {
            fileInputStream.close();
        }

        final VTDNav nav = vg.getNav();
        final FacetMapping facetMapping = getFacetMapping(nav.cloneNav(), cmdiData);

        if (facetMapping.getFacets().isEmpty()) {
            LOG.error("Problems mapping facets for file: {}", file.getAbsolutePath());
        }
        
        nav.toElement(VTDNav.ROOT);
        processResources(cmdiData, nav);
        processFacets(cmdiData, nav, facetMapping);
        return cmdiData;
	}
	
	
    @Override
    public String extractMdSelfLink(File file) throws VTDException, IOException {
        final VTDGen vg = new VTDGen();
        try (FileInputStream fileInputStream = new FileInputStream(file)) {
            vg.setDoc(IOUtils.toByteArray(fileInputStream));
            vg.parse(true);
        }
        final VTDNav nav = vg.getNav();
        nav.toElement(VTDNav.ROOT);
        AutoPilot ap = new AutoPilot(nav);
        setNameSpace(ap, null);
        ap.selectXPath("/cmd:CMD/cmd:Header/cmd:MdSelfLink/text()");
        int index = ap.evalXPath();

        String mdSelfLink = null;
        if (index != -1) {
            mdSelfLink = nav.toString(index).trim();
        }
        return mdSelfLink;
    }
    
    /**
     * Setting namespace for Autopilot ap
     *
     * @param ap
     */
    private void setNameSpace(AutoPilot ap, String profileId) {
        ap.declareXPathNameSpace("cmd", "http://www.clarin.eu/cmd/1");
        if(profileId != null) {
            ap.declareXPathNameSpace("cmdp", "http://www.clarin.eu/cmd/1/profiles/"+profileId);
        }
    }

    private FacetMapping getFacetMapping(VTDNav nav, CMDIData cmdiData) throws VTDException {
        String profileId = extractXsd(nav);
        if (profileId == null) {
            throw new RuntimeException("Cannot get xsd schema so cannot get a proper mapping. Parse failed!");
        }
        
        cmdiData.addDocField("profileId", profileId, false);

        String facetConceptsFile = MetadataImporter.config.getFacetConceptsFile();
        if (facetConceptsFile != null && !facetConceptsFile.isEmpty()) {
            URI configLocation = MetadataImporter.config.getConfigLocation();
            if (configLocation != null && !configLocation.getScheme().equals("jar")) {
                URI facetConceptsLocation = configLocation.resolve(facetConceptsFile);
                facetConceptsFile = new File(facetConceptsLocation).getAbsolutePath();
            }
        }

        //return getFacetMapping(facetConceptsFile, profileId, cmdiData);
        return CCRFacetMappingFactory.getFacetMapping(facetConceptsFile, profileId, useLocalXSDCache);
  }
	 
	 
//	 private FacetMapping getFacetMapping(String facetConcepts, String xsd, CMDIData cmdiData) {
//	        // check if concept mapping has already been created
//	        FacetMapping result = profileXSDCache.get(xsd);
//	        if (result == null) {
//	            result = createMapping(facetConcepts, xsd, cmdiData);
//	            
//	            profileXSDCache.put(xsd, result);
//	        }
//	        return result;
//	    }
	
	private String extractXsd(VTDNav nav) throws VTDException {
        String profileID = getProfileIdFromHeader(nav);
        if (profileID == null) {
            profileID = getProfileIdFromSchemaLocation(nav);
        }
        return profileID;
    }
	
	private String getProfileIdFromHeader(VTDNav nav) throws XPathParseException, XPathEvalException, NavException {
        nav.toElement(VTDNav.ROOT);
        AutoPilot ap = new AutoPilot(nav);
        setNameSpace(ap, null);
        ap.selectXPath("/cmd:CMD/cmd:Header/cmd:MdProfile/text()");
        int index = ap.evalXPath();
        String profileId = null;
        if (index != -1) {
            profileId = nav.toString(index).trim();
        }
        return profileId;
    }
	 
	 private String getProfileIdFromSchemaLocation(VTDNav nav) throws NavException {
	        String result = null;
	        nav.toElement(VTDNav.ROOT);
	        int index = nav.getAttrValNS("http://www.w3.org/2001/XMLSchema-instance", "schemaLocation");
	        if (index != -1) {
	            String schemaLocation = nav.toNormalizedString(index);
	            result = schemaLocation.split(" ")[1];
	        } else {
	            index = nav.getAttrValNS("http://www.w3.org/2001/XMLSchema-instance", "noNamespaceSchemaLocation");
	            if (index != -1) {
	                result = nav.toNormalizedString(index);
	            }
	        }

	        // extract profile ID
	        if (result != null) {
	            Matcher m = PROFILE_ID_PATTERN.matcher(result);
	            if (m.find()) {
	                return m.group(1);
	            }
	        }
	        return null;
	    }
	 
	 private void processResources(CMDIData cmdiData, VTDNav nav) throws VTDException {
	        AutoPilot mdSelfLink = new AutoPilot(nav);
	        setNameSpace(mdSelfLink, null);
	        mdSelfLink.selectXPath("/cmd:CMD/cmd:Header/cmd:MdSelfLink");
	        String mdSelfLinkString = mdSelfLink.evalXPathToString();
	        if (config.isProcessHierarchies()) {	        
	        	ResourceStructureGraph.addResource(mdSelfLinkString);
	        }

	        AutoPilot resourceProxy = new AutoPilot(nav);
	        setNameSpace(resourceProxy, null);
	        resourceProxy.selectXPath("/cmd:CMD/cmd:Resources/cmd:ResourceProxyList/cmd:ResourceProxy");

	        AutoPilot resourceRef = new AutoPilot(nav);
	        setNameSpace(resourceRef, null);
	        resourceRef.selectXPath("cmd:ResourceRef");

	        AutoPilot resourceType = new AutoPilot(nav);
	        setNameSpace(resourceType, null);
	        resourceType.selectXPath("cmd:ResourceType");

	        AutoPilot resourceMimeType = new AutoPilot(nav);
	        setNameSpace(resourceMimeType, null);
	        resourceMimeType.selectXPath("cmd:ResourceType/@mimetype");

	        while (resourceProxy.evalXPath() != -1) {
	            String ref = resourceRef.evalXPathToString();
	            String type = resourceType.evalXPathToString();
	            String mimeType = resourceMimeType.evalXPathToString();

	            if (!ref.equals("") && !type.equals("")) {
	                // note that the mime type could be empty
	                cmdiData.addResource(ref, type, mimeType);
	            }

	            // resource hierarchy information?
	            if (type.toLowerCase().equals("metadata")) {
	                ResourceStructureGraph.addEdge(ref, mdSelfLinkString);
	            }
	        }
	    }
	 
	 private void processFacets(CMDIData cmdiData, VTDNav nav, FacetMapping facetMapping) throws VTDException {
	    	
	        List<FacetConfiguration> facetList = facetMapping.getFacets();    	
	        for (FacetConfiguration config : facetList) {
	            boolean matchedPattern = false;
	            Collection<String> patterns = config.getPatternsMap().keySet();
	            for (String pattern : patterns) {
	                matchedPattern = matchPattern(cmdiData, nav, config, pattern, config.getAllowMultipleValues());
	                if (matchedPattern && !config.getAllowMultipleValues()) {
	                    break;
	                }
	            }

	            // using fallback patterns if extraction failed
	            if (matchedPattern == false) {
	                for (String pattern : config.getFallbackPatterns()) {
	                    matchedPattern = matchPattern(cmdiData, nav, config, pattern, config.getAllowMultipleValues());
	                    if (matchedPattern && !config.getAllowMultipleValues()) {
	                        break;
	                    }
	                }
	            }
	        }
	    }
	 
	 
	 private boolean matchPattern(CMDIData cmdiData, VTDNav nav, FacetConfiguration config, String pattern, Boolean allowMultipleValues) throws VTDException {
	        final AutoPilot ap = new AutoPilot(nav);
	        setNameSpace(ap, extractXsd(nav));
	        ap.selectXPath(pattern);

	        boolean matchedPattern = false;
	        int index = ap.evalXPath();
	        while (index != -1) {
	            matchedPattern = true;
	            if (nav.getTokenType(index) == VTDNav.TOKEN_ATTR_NAME) {
	                //if it is an attribute you need to add 1 to the index to get the right value
	                index++;
	            }
	            final String value = nav.toString(index);

	            final String languageCode = extractLanguageCode(nav);

	            // ignore non-English language names for facet LANGUAGE_CODE
	            if (config.getName().equals(FacetConstants.FIELD_LANGUAGE_CODE) && !languageCode.equals("code:eng") && !languageCode.equals("code:und")) {
	                index = ap.evalXPath();
	                continue;
	            }
	            
	            final List<String> values = postProcess(config.getName(), value);
	            
	            
//	            String sValues = "";
//	            for(String s: values)
//	        	sValues += value + ";";
	            
	            //discard '--' values
	            if (values != null && !values.isEmpty() && values.get(0).equals("--")) {
	                return matchedPattern;
	            }
	            
	            insertFacetValues(config.getName(), values, cmdiData, languageCode, allowMultipleValues, config.isCaseInsensitive(), true);
	            
	            //in case of profile name forward normalized value (not profileId)
	            crossMap(config, config.getName().equals(FacetConstants.FIELD_CLARIN_PROFILE)? values.get(0) : value, cmdiData, languageCode);
	            
	            //add also non curated values 
	            //and concepts for Licence and availability
	            
	            switch(config.getName()){
	            	case FacetConstants.FIELD_RESOURCE_CLASS: cmdiData.addDocField("resourceClassOrig", value, false); break;
	            	case FacetConstants.FIELD_LANGUAGE_CODE:  cmdiData.addDocField("languageOrig", value, false); break;
	            	case FacetConstants.FIELD_COUNTRY: cmdiData.addDocField("countryOrig", value, false); break;	            	
	            	case FacetConstants.FIELD_ORGANISATION: cmdiData.addDocField("organisationOrig", value, false); break;
	            	case FacetConstants.FIELD_NATIONAL_PROJECT: cmdiData.addDocField("natProjectOrig", value, false); break;
	            	case FacetConstants.FIELD_AVAILABILITY: 
	            		cmdiData.addDocField("availabilityOrig", value, false);
	            		//in case of availability or licence add index for concept with prefLabel as facet name
	            	case FacetConstants.FIELD_LICENSE:
	            		String conceptURL = config.getPatternsMap().get(pattern);
	            		if(conceptURL != null){//if it comes from concept, not from hardcoded xpaths
	            			String conceptPrefLabel = CCRService.getPrefLabel(conceptURL).trim().replaceAll(" ", "_");
	            			cmdiData.addDocField(conceptPrefLabel, value, false);
	            			if(!indexedConcepts.contains(conceptPrefLabel)){
	            				System.out.println("New Concept added: " + conceptPrefLabel);
	            				indexedConcepts.add(conceptPrefLabel);
	            			}
	            		}
	            		break;
	            }
	            
	            
	            
	            // insert post-processed values into derived facet(s) if configured
	            for (String derivedFacet : config.getDerivedFacets()) {
	                final List<String> derivedValues = new ArrayList<String>();
	                for (String postProcessedValue : values) {
	                    derivedValues.addAll(postProcess(derivedFacet, postProcessedValue));
	                }
	                insertFacetValues(derivedFacet, derivedValues, cmdiData, languageCode, allowMultipleValues, config.isCaseInsensitive(), true);
	            }

	            index = ap.evalXPath();

	            if (!allowMultipleValues) {
	                break;
	            }
	        }
	        return matchedPattern;
	    }

	    private String extractLanguageCode(VTDNav nav) throws NavException {
	        // extract language code in xml:lang if available
	        Integer langAttrIndex = nav.getAttrVal("xml:lang");
	        String languageCode;
	        if (langAttrIndex != -1) {
	            languageCode = nav.toString(langAttrIndex).trim();
	        } else {
	            return DEFAULT_LANGUAGE;
	        }

	        return MetadataImporter.POST_PROCESSORS.get(FacetConstants.FIELD_LANGUAGE_CODE).process(languageCode).get(0);
	    }
	    
	    private void insertFacetValues(String name, List<String> valueList, CMDIData cmdiData, String languageCode, boolean allowMultipleValues, boolean caseInsensitive, boolean comesFromConceptMapping) {
	    	
	    	//keep only values from original concepts, not from cross mappings
			if(comesFromConceptMapping && !allowMultipleValues && cmdiData.getSolrDocument() != null && cmdiData.getSolrDocument().containsKey(name)){
				cmdiData.getSolrDocument().remove(name);
			}
			
			if(!comesFromConceptMapping && !allowMultipleValues && cmdiData.getSolrDocument() != null && cmdiData.getSolrDocument().containsKey(name))
				return;
	    	
	        for (int i = 0; i < valueList.size(); i++) {
	            if (!allowMultipleValues && i > 0) {
	                break;
	            }
	            String fieldValue = valueList.get(i).trim();
	            if (name.equals(FacetConstants.FIELD_DESCRIPTION)) {
	                fieldValue = "{" + languageCode + "}" + fieldValue;
	            }
	            cmdiData.addDocField(name, fieldValue, caseInsensitive);
	        }
	    }

	    /**
	     * Applies registered PostProcessor to extracted values
	     *
	     * @param facetName name of the facet for which value was extracted
	     * @param extractedValue extracted value from CMDI file
	     * @return value after applying matching PostProcessor or the original value
	     * if no PostProcessor was registered for the facet
	     */
	    private List<String> postProcess(String facetName, String extractedValue) {
	    	List<String> resultList = new ArrayList<String>();
	        if (postProcessors.containsKey(facetName)) {
	            PostProcessor processor = postProcessors.get(facetName);
	            resultList = processor.process(extractedValue);
	        } else {
	            resultList.add(extractedValue);
	        }
	        return resultList;
	    }
	    
    private void crossMap(FacetConfiguration config, String extractedValue, CMDIData cmdiData, String languageCode){
    	//skip if not enabled
    	if(!MetadataImporter.config.isUseCrossMapping())
    		return;
    	
        if (MetadataImporter.POST_PROCESSORS.containsKey(config.getName())){
            PostProcessor processor = MetadataImporter.POST_PROCESSORS.get(config.getName());
            if(processor instanceof PostProcessorsWithVocabularyMap){
            	
            	List<String> facetNames = MetadataImporter.config.getFacetFields();
            	
            	Map<String, String> crossMap = ((PostProcessorsWithVocabularyMap) processor).getCrossMappings(extractedValue);
            	if(crossMap != null)
	            	for(Entry e: crossMap.entrySet()){
	            		String toFacet = (String) e.getKey();
	            		String value = (String) e.getValue();
	            		for(String facetName: facetNames){
	            			if(toFacet.toLowerCase().equals(facetName.toLowerCase())){//normalize facet name, map can contain it in any case
	            				insertFacetValues(facetName, Arrays.asList(value), cmdiData, languageCode, config.getAllowMultipleValues(), config.isCaseInsensitive(), false);
	            			}
	            		}
	            	}
            }
        }
    }
	    
	public static void main(String[] args) {
		System.out.println(CCRService.getPrefLabel("http://hdl.handle.net/11459/CCR_C-3786_21c37142-994f-63a8-5a5b-a9fce07681a7"));
		System.out.println(CCRService.getPrefLabel("http://hdl.handle.net/11459/CCR_C-2547_7883d382-b3ce-8ab4-7052-0138525a8ba1"));
	}
	
	
}
