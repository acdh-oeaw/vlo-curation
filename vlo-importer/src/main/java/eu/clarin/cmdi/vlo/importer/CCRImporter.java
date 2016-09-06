package eu.clarin.cmdi.vlo.importer;

import java.io.File;
import java.io.FileInputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
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
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.AcceptableContext;
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.FacetConcept;
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.RejectableContext;

public class CCRImporter implements CMDIDataProcessor{
	
	private static final Pattern PROFILE_ID_PATTERN = Pattern.compile(".*(clarin.eu:cr1:p_[0-9]+).*");
    private final static Logger LOG = LoggerFactory.getLogger(CCRImporter.class);

    private static final String DEFAULT_LANGUAGE = "code:und";
    
    
    private static final Map<String, FacetMapping> profileXSDCache = new HashMap<String, FacetMapping>();
    
    private static final Set<String> indexedConcepts = new HashSet<String>();
	

	@Override
	public CMDIData process(File file) throws Exception {
		CMDIData cmdiData = new CMDIData();
        VTDGen vg = new VTDGen();
        FileInputStream fileInputStream = new FileInputStream(file);
        vg.setDoc(IOUtils.toByteArray(fileInputStream));
        vg.parse(true);
        fileInputStream.close();

        VTDNav nav = vg.getNav();
        String profileId = extractXsd(nav.cloneNav());
        cmdiData.addDocField("profileId", profileId, false);
        FacetMapping facetMapping = getFacetMapping(profileId, cmdiData);

        if (facetMapping.getFacets().isEmpty()) {
            LOG.error("Problems mapping facets for file: {}", file.getAbsolutePath());
        }
        
        nav.toElement(VTDNav.ROOT);
        processResources(cmdiData, nav);
        processFacets(cmdiData, nav, facetMapping);
        return cmdiData;
	}

	
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
	        setNameSpace(ap);
	        ap.selectXPath("/c:CMD/c:Header/c:MdProfile/text()");
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
	 
	 
	 private void setNameSpace(AutoPilot ap) {
	        ap.declareXPathNameSpace("c", "http://www.clarin.eu/cmd/");
	    }
	 
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
	            
	            String sValues = "";
	            for(String s: values)
	        	sValues += value + ";";
	            
	            if(values != null && !values.isEmpty() && values.get(0).equals("--")){//ignore this values
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
	        if (MetadataImporter.POST_PROCESSORS.containsKey(facetName)) {
	            PostProcessor processor = MetadataImporter.POST_PROCESSORS.get(facetName);
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
	 
	 
	 private FacetMapping getFacetMapping(String profileId, CMDIData cmdiData) throws VTDException {
	        if (profileId == null) {
	            throw new RuntimeException("Cannot get xsd schema so cannot get a proper mapping. Parse failed!");
	        }

	        String facetConceptsFile = MetadataImporter.config.getFacetConceptsFile();
	        if (facetConceptsFile != null && !facetConceptsFile.isEmpty()) {
	            URI configLocation = MetadataImporter.config.getConfigLocation();
	            if (configLocation != null && !configLocation.getScheme().equals("jar")) {
	                URI facetConceptsLocation = configLocation.resolve(facetConceptsFile);
	                facetConceptsFile = new File(facetConceptsLocation).getAbsolutePath();
	            }
	        }

	        return getFacetMapping(facetConceptsFile, profileId, cmdiData);
	  }
	 
	 
	 private FacetMapping getFacetMapping(String facetConcepts, String xsd, CMDIData cmdiData) {
	        // check if concept mapping has already been created
	        FacetMapping result = profileXSDCache.get(xsd);
	        if (result == null) {
	            result = createMapping(facetConcepts, xsd, cmdiData);
	            
	            profileXSDCache.put(xsd, result);
	        }
	        return result;
	    }
	 
	 
	 
	 
	 private FacetMapping createMapping(String facetConcepts, String xsd, CMDIData cmdiData) {

	        FacetMapping result = new FacetMapping();
	        // Gets the configuration. VLOMarshaller only reads in the facetconceptmapping.xml file and returns the result (though the reading in is implicit).
	        FacetConceptMapping conceptMapping = VLOMarshaller.getFacetConceptMapping(facetConcepts);
	        try {
	            //The magic
	            Map<String, List<String>> conceptLinkPathMapping = createConceptLinkPathMapping(xsd);
	            Map<String, String> pathConceptLinkMapping = null;
	            // Below we put the stuff we found into the configuration class.
	            for (FacetConcept facetConcept : conceptMapping.getFacetConcepts()) {
	                FacetConfiguration config = new FacetConfiguration();
	                Map<String, String> xpaths = new HashMap<String, String>();
	                handleId(xpaths, facetConcept);
	                for (String concept : facetConcept.getConcepts()) {
	                    List<String> paths = conceptLinkPathMapping.get(concept);
	                    if (paths != null) {
	                        if (facetConcept.hasContext()) {
	                            for (String path : paths) {
	                                // lazily instantiate the reverse mapping, i.e., from concept to path
	                                if (pathConceptLinkMapping == null) {
	                                    pathConceptLinkMapping = new HashMap<String, String>();
	                                    for (String c : conceptLinkPathMapping.keySet()) {
	                                        for (String p : conceptLinkPathMapping.get(c)) {
	                                            pathConceptLinkMapping.put(p, c);
	                                        }
	                                    }
	                                }
	                                String context = getContext(path, pathConceptLinkMapping);
	                                boolean handled = false;
	                                // check against acceptable context
	                                if (facetConcept.hasAcceptableContext()) {
	                                    AcceptableContext acceptableContext = facetConcept.getAcceptableContext();
	                                    if (context == null && acceptableContext.includeEmpty()) {
	                                        // no context is accepted
	                                        LOG.debug("facet[{}] path[{}] context[{}](empty) is accepted", facetConcept.getName(), path, context);
	                                        xpaths.put(path,concept);//xpaths.add(path);
	                                        handled = true;
	                                    } else if (acceptableContext.getConcepts().contains(context)) {
	                                        // a specific context is accepted
	                                        LOG.debug("facet[{}] path[{}] context[{}] is accepted", facetConcept.getName(), path, context);
	                                        xpaths.put(path,concept);//xpaths.add(path);
	                                        handled = true;
	                                    }
	                                }
	                                // check against rejectable context
	                                if (!handled && facetConcept.hasRejectableContext()) {
	                                    RejectableContext rejectableContext = facetConcept.getRejectableContext();
	                                    if (context == null && rejectableContext.includeEmpty()) {
	                                        // no context is rejected
	                                        LOG.debug("facet[{}] path[{}] context[{}](empty) is rejected", facetConcept.getName(), path, context);
	                                        handled = true;
	                                    } else if (rejectableContext.getConcepts().contains(context)) {
	                                        // a specific context is rejected
	                                        LOG.debug("facet[{}] path[{}] context[{}] is rejected", facetConcept.getName(), path, context);
	                                        handled = true;
	                                    } else if (rejectableContext.includeAny()) {
	                                        // any context is rejected
	                                        LOG.debug("facet[{}] path[{}] context[{}](any) is rejected", facetConcept.getName(), path, context);
	                                        handled = true;
	                                    }
	                                }
	                                if (!handled && context != null && facetConcept.hasAcceptableContext() && facetConcept.getAcceptableContext().includeAny()) {
	                                    // any, not rejected context, is accepted
	                                    LOG.debug("facet[{}] path[{}] context[{}](any) is accepted", facetConcept.getName(), path, context);
	                                    xpaths.put(path,concept);//xpaths.add(path);
	                                }
	                            }
	                        } else {
	                        	for(String path: paths){
	                        		 xpaths.put(path,concept);
	                        	}
	                        	//xpaths.addAll(paths);
	                        }
	                    }
	                }

	                // pattern-based blacklisting: remove all XPath expressions that contain a blacklisted substring;
	                // this is basically a hack to enhance the quality of the visualised information in the VLO;
	                // should be replaced by a more intelligent approach in the future
	                for (String blacklistPattern : facetConcept.getBlacklistPatterns()) {
	                	if(xpaths.containsKey(blacklistPattern))
	                		xpaths.remove(blacklistPattern);
	                	
//	                    Iterator<String> xpathIterator = xpaths.iterator();
//	                    while (xpathIterator.hasNext()) {
//	                        String xpath = xpathIterator.next();
//	                        if (xpath.contains(blacklistPattern)) {
//	                            LOG.debug("Rejecting {} because of blacklisted substring {}", xpath, blacklistPattern);
//	                            xpathIterator.remove();
//	                        }
//	                    }
	                }

	                config.setCaseInsensitive(facetConcept.isCaseInsensitive());
	                config.setAllowMultipleValues(facetConcept.isAllowMultipleValues());
	                config.setName(facetConcept.getName());

//	                LinkedHashSet<String> linkedHashSet = new LinkedHashSet<String>(xpaths);
//	                if(xpaths.size() != linkedHashSet.size()) {
//	                    LOG.error("Duplicate XPaths in : "+xpaths);
//	                }
	                
	                config.setPatternsMap(xpaths);
	                //config.setPatterns(new ArrayList<String>(linkedHashSet));
	                config.setFallbackPatterns(facetConcept.getPatterns());
	                config.setDerivedFacets(facetConcept.getDerivedFacets());

	                if (!config.getPatternsMap().isEmpty() || !config.getFallbackPatterns().isEmpty()) {
	                    result.addFacet(config);
	                }
	            }
	        } catch (NavException e) {
	            LOG.error("Error creating facetMapping from xsd: {}", xsd, e);
	        }
	        return result;
	    }
	 
	 
	 private void handleId(Map<String, String> xpaths, FacetConcept facetConcept) {
	        if (FacetConstants.FIELD_ID.equals(facetConcept.getName())) {
	        	for(String pattern: facetConcept.getPatterns())
	        		xpaths.put(pattern, null);
	        }
	    }
	 
	    private String getContext(String path, Map<String, String> pathConceptLinkMapping) {
	        String context = null;
	        String cpath = path;
	        while (context == null && !cpath.equals("/text()")) {
	            cpath = cpath.replaceAll("/[^/]*/text\\(\\)", "/text()");
	            context = pathConceptLinkMapping.get(cpath);
	        }
	        return context;
	    }
	 
	 private Map<String, List<String>> createConceptLinkPathMapping(String xsd) throws NavException {
	        Map<String, List<String>> result = new HashMap<String, List<String>>();
	        VTDGen vg = new VTDGen();
	        boolean parseSuccess;
	        parseSuccess = vg.parseHttpUrl(MetadataImporter.config.getComponentRegistryProfileSchema(xsd), true);
	            
	        if (!parseSuccess) {
	            LOG.error("Cannot create ConceptLink Map from xsd (xsd is probably not reachable): " + xsd + ". All metadata instances that use this xsd will not be imported correctly.");
	            return result; //return empty map, so the incorrect xsd is not tried for all metadata instances that specify it.
	        }
	        
	        VTDNav vn = vg.getNav();
	        AutoPilot ap = new AutoPilot(vn);
	        ap.selectElement("xs:element");
	        Deque<Token> elementPath = new LinkedList<Token>();
	        while (ap.iterate()) {
	            int i = vn.getAttrVal("name");
	            if (i != -1) {
	                String elementName = vn.toNormalizedString(i);
	                updateElementPath(vn, elementPath, elementName);
	                int datcatIndex = getDatcatIndex(vn);
	                if (datcatIndex != -1) {
	                    String conceptLink = vn.toNormalizedString(datcatIndex);
	                    String xpath = createXpath(elementPath);
	                    List<String> values = result.get(conceptLink);
	                    if (values == null) {
	                        values = new ArrayList<String>();
	                        result.put(conceptLink, values);
	                    }
	                    values.add(xpath);
	                }
	            }
	        }
	        return result;
	    }

	    /**
	     * Goal is to get the "datcat" attribute. Tries a number of different favors
	     * that were found in the xsd's.
	     *
	     * @return -1 if index is not found.
	     */
	    private int getDatcatIndex(VTDNav vn) throws NavException {
	        int result = -1;
	        result = vn.getAttrValNS("http://www.isocat.org/ns/dcr", "datcat");
	        if (result == -1) {
	            result = vn.getAttrValNS("http://www.isocat.org", "datcat");
	        }
	        if (result == -1) {
	            result = vn.getAttrVal("dcr:datcat");
	        }
	        return result;
	    }

	    /**
	     * Given an xml-token path thingy create an xpath.
	     *
	     * @param elementPath
	     * @return
	     */
	    private String createXpath(Deque<Token> elementPath) {
	        StringBuilder xpath = new StringBuilder("/");
	        for (Token token : elementPath) {
	            xpath.append("c:").append(token.name).append("/");
	        }
	        return xpath.append("text()").toString();
	    }

	    /**
	     * does some updating after a step. To keep the path proper and path-y.
	     *
	     * @param vn
	     * @param elementPath
	     * @param elementName
	     */
	    private void updateElementPath(VTDNav vn, Deque<Token> elementPath, String elementName) {
	        int previousDepth = elementPath.isEmpty() ? -1 : elementPath.peekLast().depth;
	        int currentDepth = vn.getCurrentDepth();
	        if (currentDepth == previousDepth) {
	            elementPath.removeLast();
	        } else if (currentDepth < previousDepth) {
	            while (currentDepth <= previousDepth) {
	                elementPath.removeLast();
	                previousDepth = elementPath.peekLast().depth;
	            }
	        }
	        elementPath.offerLast(new Token(currentDepth, elementName));
	    }

	    class Token {

	        final String name;
	        final int depth;

	        public Token(int depth, String name) {
	            this.depth = depth;
	            this.name = name;
	        }

	        @Override
	        public String toString() {
	            return name + ":" + depth;
	        }
	    }
	    
	    
	public static void main(String[] args) {
		System.out.println(CCRService.getPrefLabel("http://hdl.handle.net/11459/CCR_C-3786_21c37142-994f-63a8-5a5b-a9fce07681a7"));
		System.out.println(CCRService.getPrefLabel("http://hdl.handle.net/11459/CCR_C-2547_7883d382-b3ce-8ab4-7052-0138525a8ba1"));
	}
	
	
	
}
