package eu.clarin.cmdi.vlo.importer;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
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

public class CMDIParserVTDXML implements CMDIDataProcessor {

    private final Map<String, PostProcessor> postProcessors;
    private final Boolean useLocalXSDCache;
    private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(".*(clarin.eu:cr1:p_[0-9]+).*");
    private final static Logger LOG = LoggerFactory.getLogger(CMDIParserVTDXML.class);

    private static final String DEFAULT_LANGUAGE = "code:und";

    public CMDIParserVTDXML(Map<String, PostProcessor> postProcessors, Boolean useLocalXSDCache) {
        this.postProcessors = postProcessors;
        this.useLocalXSDCache = useLocalXSDCache;
    }

    @Override
    public CMDIData process(File file) throws VTDException, IOException {
        CMDIData cmdiData = new CMDIData();
        VTDGen vg = new VTDGen();
        FileInputStream fileInputStream = new FileInputStream(file);
        vg.setDoc(IOUtils.toByteArray(fileInputStream));
        vg.parse(true);
        fileInputStream.close();

        VTDNav nav = vg.getNav();
        String profileId = extractXsd(nav.cloneNav());
        cmdiData.addDocField("profileId", profileId, false);
        FacetMapping facetMapping = getFacetMapping(profileId);

        if (facetMapping.getFacets().isEmpty()) {
            LOG.error("Problems mapping facets for file: {}", file.getAbsolutePath());
        }
        
        nav.toElement(VTDNav.ROOT);
        processResources(cmdiData, nav);
        processFacets(cmdiData, nav, facetMapping);
        return cmdiData;
    }

    /**
     * Setting namespace for Autopilot ap
     *
     * @param ap
     */
    private void setNameSpace(AutoPilot ap) {
        ap.declareXPathNameSpace("c", "http://www.clarin.eu/cmd/");
    }

    /**
     * Extracts valid XML patterns for all facet definitions
     *
     * @param nav VTD Navigator
     * @return the facet mapping used to map meta data to facets
     * @throws VTDException
     */
    private FacetMapping getFacetMapping(String profileId) throws VTDException {
        if (profileId == null) {
            throw new RuntimeException("Cannot get xsd schema so cannot get a proper mapping. Parse failed!");
        }
//        final VloConfig config = MetadataImporter.config;
//        final URI facetConceptsFile
//                = FacetConceptsMarshaller.resolveFacetsFile(config.getConfigLocation(), config.getFacetConceptsFile());
//        final String facetConceptsFilePath = new File(facetConceptsFile).getAbsolutePath();
//        return FacetMappingFactory.getFacetMapping(facetConceptsFilePath, profileId, useLocalXSDCache);

        String facetConceptsFile = MetadataImporter.config.getFacetConceptsFile();

        //resolve against config location? (empty = default location)
        if (facetConceptsFile != null && !facetConceptsFile.isEmpty()) {
            URI configLocation = MetadataImporter.config.getConfigLocation();
            if (configLocation != null && !configLocation.getScheme().equals("jar")) {
                URI facetConceptsLocation = configLocation.resolve(facetConceptsFile);
                facetConceptsFile = new File(facetConceptsLocation).getAbsolutePath();
            }
        }

        return FacetMappingFactory.getFacetMapping(facetConceptsFile, profileId, useLocalXSDCache);
    }

    /**
     * Try two approaches to extract the XSD schema information from the CMDI
     * file
     *
     * @param nav VTD Navigator
     * @return ID of CMDI schema, or null if neither the CMDI header nor the
     * XMLSchema-instance's attributes contained the information
     * @throws VTDException
     */
    String extractXsd(VTDNav nav) throws VTDException {
        String profileID = getProfileIdFromHeader(nav);
        if (profileID == null) {
            profileID = getProfileIdFromSchemaLocation(nav);
        }
        return profileID;
    }

    /**
     * Extract XSD schema information from CMDI header (using element
     * //Header/MdProfile)
     *
     * @param nav VTD Navigator
     * @return ID of CMDI schema, or null if content of //Header/MdProfile
     * element could not be read
     * @throws XPathParseException
     * @throws XPathEvalException
     * @throws NavException
     */
    private String getProfileIdFromHeader(VTDNav nav) throws XPathParseException, XPathEvalException, NavException {
        nav.toElement(VTDNav.ROOT);
        AutoPilot ap = new AutoPilot(nav);
        setNameSpace(ap);
        ap.selectXPath("/c:CMD/c:Header/c:MdProfile/text()");
        int index = ap.evalXPath();
        String profileId = null;
        if (index != -1) {
            profileId = nav.toString(index).trim();
        }
        return profileId;
    }

    /**
     * Extract XSD schema information from schemaLocation or
     * noNamespaceSchemaLocation attributes
     *
     * @param nav VTD Navigator
     * @return ID of CMDI schema, or null if attributes don't exist
     * @throws NavException
     */
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

    /**
     * Extract ResourceProxies from ResourceProxyList
     *
     * @param cmdiData representation of the CMDI document
     * @param nav VTD Navigator
     * @throws VTDException
     */
    private void processResources(CMDIData cmdiData, VTDNav nav) throws VTDException {
        AutoPilot mdSelfLink = new AutoPilot(nav);
        setNameSpace(mdSelfLink);
        mdSelfLink.selectXPath("/c:CMD/c:Header/c:MdSelfLink");
        String mdSelfLinkString = mdSelfLink.evalXPathToString();
        ResourceStructureGraph.addResource(mdSelfLinkString);

        AutoPilot resourceProxy = new AutoPilot(nav);
        setNameSpace(resourceProxy);
        resourceProxy.selectXPath("/c:CMD/c:Resources/c:ResourceProxyList/c:ResourceProxy");

        AutoPilot resourceRef = new AutoPilot(nav);
        setNameSpace(resourceRef);
        resourceRef.selectXPath("c:ResourceRef");

        AutoPilot resourceType = new AutoPilot(nav);
        setNameSpace(resourceType);
        resourceType.selectXPath("c:ResourceType");

        AutoPilot resourceMimeType = new AutoPilot(nav);
        setNameSpace(resourceMimeType);
        resourceMimeType.selectXPath("c:ResourceType/@mimetype");

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

    /**
     * Extracts facet values according to the facetMapping
     *
     * @param cmdiData representation of the CMDI document
     * @param nav VTD Navigator
     * @param facetMapping the facet mapping used to map meta data to facets
     * @throws VTDException
     */
    private void processFacets(CMDIData cmdiData, VTDNav nav, FacetMapping facetMapping) throws VTDException {
    	    	
        List<FacetConfiguration> facetList = facetMapping.getFacets();    	
        for (FacetConfiguration config : facetList) {
            boolean matchedPattern = false;
            List<String> patterns = config.getPatterns();
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

    /**
     * Extracts content from CMDI file for a specific facet based on a single
     * XPath expression
     *
     * @param cmdiData representation of the CMDI document
     * @param nav VTD Navigator
     * @param config facet configuration
     * @param pattern XPath expression
     * @param allowMultipleValues information if multiple values are allowed in
     * this facet
     * @return pattern matched a node in the CMDI file?
     * @throws VTDException
     */
    private boolean matchPattern(CMDIData cmdiData, VTDNav nav, FacetConfiguration config, String pattern, Boolean allowMultipleValues) throws VTDException {
        final AutoPilot ap = new AutoPilot(nav);
        setNameSpace(ap);
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
            
            insertFacetValues(config.getName(), values, cmdiData, languageCode, allowMultipleValues, config.isCaseInsensitive(), true);
            
            //in case of profile name forward normalized value (not profileId)
            crossMap(config, config.getName().equals(FacetConstants.FIELD_CLARIN_PROFILE)? values.get(0) : value, cmdiData, languageCode);
            
            //add also non curated values
            
            switch(config.getName()){
            	case FacetConstants.FIELD_RESOURCE_CLASS: cmdiData.addDocField("resourceClassOrig", value, false); break;
            	case FacetConstants.FIELD_LANGUAGE_CODE:  cmdiData.addDocField("languageOrig", value, false); break;
            	case FacetConstants.FIELD_COUNTRY: cmdiData.addDocField("countryOrig", value, false); break;
            	case FacetConstants.FIELD_AVAILABILITY: cmdiData.addDocField("availabilityOrig", value, false); break;
            	case FacetConstants.FIELD_ORGANISATION: cmdiData.addDocField("organisationOrig", value, false); break;
            	case FacetConstants.FIELD_NATIONAL_PROJECT: cmdiData.addDocField("natProjectOrig", value, false); break;            	
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

        return postProcessors.get(FacetConstants.FIELD_LANGUAGE_CODE).process(languageCode).get(0);
    }

    
    /*
	 * Add values to facet either they come from MD fields either from cross mapping
	 * Advantage is given to the values from MD fields. They will be always at the begging of the list and in case
	 * when facet doesn't allow multiple values and we already had value from cross mapping this value will be overridden
	 * 
	 */
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
    	
        if (postProcessors.containsKey(config.getName())){
            PostProcessor processor = postProcessors.get(config.getName());
            if(processor instanceof PostProcessorsWithVocabularyMap){
            	
            	List<String> facetNames = MetadataImporter.config.getAllFacetFields();
            	
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
    
}
