package eu.clarin.cmdi.vlo.importer;

import com.ximpleware.AutoPilot;
import com.ximpleware.NavException;
import com.ximpleware.VTDGen;
import com.ximpleware.VTDNav;
import com.ximpleware.XPathEvalException;
import com.ximpleware.XPathParseException;
import eu.clarin.cmdi.vlo.FacetConstants;
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.FacetConcept;
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.AcceptableContext;
import eu.clarin.cmdi.vlo.importer.FacetConceptMapping.RejectableContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.*;

/**
 * Creates facet-mappings (xpaths) from a configuration. As they say "this is
 * where the magic happens". Also does some caching.
 */
public class CCRFacetMappingFactory {

    private final static Logger LOG = LoggerFactory.getLogger(CCRFacetMappingFactory.class);

    private final Map<String, FacetMapping> mapping = new HashMap<>();

    /**
     * Our one instance of the FMF.
     */
    private final static CCRFacetMappingFactory INSTANCE = new CCRFacetMappingFactory();

    private CCRFacetMappingFactory() {
    }
    
    /**
     * 
     * @param facetConceptsFile path to facet concepts file, leave null or empty to use default
     * @param xsd
     * @param useLocalXSDCache
     * @return 
     */
    public static FacetMapping getFacetMapping(String facetConceptsFile, String xsd, Boolean useLocalXSDCache) {
        return INSTANCE.getOrCreateMapping(facetConceptsFile, xsd, useLocalXSDCache);
    }

    /**
     * Get facet concept mapping.
     *
     * Get facet mapping used to map meta data based on a facet concepts file
     * and url to cmdi meta data profile.
     *
     * @param facetConcepts name of the facet concepts file
     * @param xsd url of xml schema of cmdi profile
     * @param useLocalXSDCache use local XML schema files instead of accessing the component registry 
     *
     * @return facet concept mapping
     */
    private FacetMapping getOrCreateMapping(String facetConcepts, String xsd, Boolean useLocalXSDCache) {
        // check if concept mapping has already been created
        FacetMapping result = mapping.get(xsd);
        if (result == null) {
            result = createMapping(facetConcepts, xsd, useLocalXSDCache);
            mapping.put(xsd, result);
        }
        return result;
    }

    /**
     * Create facet concept mapping.
     *
     * Create facet mapping used to map meta data based on a facet concept
     * mapping file and url to cmdi meta data profile.
     *
     * @param facetConcepts name of the facet concepts file
     * @param xsd url of xml schema of cmdi profile
     * @param useLocalXSDCache use local XML schema files instead of accessing the component registry 
     *
     * @return the facet mapping used to map meta data to facets
     */
    private FacetMapping createMapping(String facetConcepts, String xsd, Boolean useLocalXSDCache) {

        FacetMapping result = new FacetMapping();
        // Gets the configuration. VLOMarshaller only reads in the facetconceptmapping.xml file and returns the result (though the reading in is implicit).
        FacetConceptMapping conceptMapping = VLOMarshaller.getFacetConceptMapping(facetConcepts);
        try {
            //The magic
            Map<String, List<String>> conceptLinkPathMapping = createConceptLinkPathMapping(xsd, useLocalXSDCache);
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
                                    pathConceptLinkMapping = new HashMap<>();
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
                                    xpaths.put(path, concept);//xpaths.add(path);
                                }
                            }
                        } else {
                        	//xpaths.addAll(paths);
                        	for(String path: paths)
                       		 	xpaths.put(path,concept);
                       	}
                    }
                }

                // pattern-based blacklisting: remove all XPath expressions that contain a blacklisted substring;
                // this is basically a hack to enhance the quality of the visualised information in the VLO;
                // should be replaced by a more intelligent approach in the future
                for (String blacklistPattern : facetConcept.getBlacklistPatterns()) {
                	if(xpaths.containsKey(blacklistPattern))
                		xpaths.remove(blacklistPattern);
                	
//                    Iterator<String> xpathIterator = xpaths.iterator();
//                    while (xpathIterator.hasNext()) {
//                        String xpath = xpathIterator.next();
//                        if (xpath.contains(blacklistPattern)) {
//                            LOG.debug("Rejecting {} because of blacklisted substring {}", xpath, blacklistPattern);
//                            xpathIterator.remove();
//                        }
//                    }
                }

                config.setCaseInsensitive(facetConcept.isCaseInsensitive());
                config.setAllowMultipleValues(facetConcept.isAllowMultipleValues());
                config.setName(facetConcept.getName());

//                LinkedHashSet<String> linkedHashSet = new LinkedHashSet<>(xpaths);
//                if(xpaths.size() != linkedHashSet.size()) {
//                    LOG.error("Duplicate XPaths for facet {} in: {}.", facetConcept.getName(), xpaths);
//                }                
               
                //config.setPatterns(new ArrayList<>(linkedHashSet));
                config.setPatternsMap(xpaths);
                config.setFallbackPatterns(facetConcept.getPatterns());
                config.setDerivedFacets(facetConcept.getDerivedFacets());

                if (!xpaths.isEmpty() || !config.getFallbackPatterns().isEmpty()) {
                    result.addFacet(config);
                }
            }
        } catch (NavException e) {
            LOG.error("Error creating facetMapping from xsd: {}", xsd, e);
        }
        return result;
    }

    /**
     * Look if there is a contextual (container) data category associated with
     * an ancestor by walking back.
     */
    private String getContext(String path, Map<String, String> pathConceptLinkMapping) {
        String context = null;
        String cpath = path;
        while (context == null && !cpath.equals("/text()")) {
            cpath = cpath.replaceAll("/[^/]*/text\\(\\)", "/text()");
            context = pathConceptLinkMapping.get(cpath);
        }
        return context;
    }

    /**
     * The id facet is special case and patterns must be added first. The
     * standard pattern to get the id out of the header is the most reliable and
     * it should fall back on concept matching if nothing matches. (Note this is
     * the exact opposite of other facets where the concept match is probably
     * better then the 'hardcoded' pattern).
     */
    private void handleId(Map<String, String> xpaths, FacetConcept facetConcept) {
        if (FacetConstants.FIELD_ID.equals(facetConcept.getName())) {
        	for(String pattern: facetConcept.getPatterns())
        		xpaths.put(pattern, null);
        }
    }

    /**
     * "this is where the magic happens". Finds paths in the xsd to all concepts
     * (isocat data catagories).
     *
     * @param xsd URL of XML Schema of some CMDI profile
     * @param useLocalXSDCache use local XML schema files instead of accessing the component registry 
     * @return Map (Data Category -> List of XPath expressions linked to the key
     * data category which can be found in CMDI files with this schema)
     * @throws NavException
     */
    private Map<String, List<String>> createConceptLinkPathMapping(String xsd, Boolean useLocalXSDCache) throws NavException {
        Map<String, List<String>> result = new HashMap<>();
        VTDGen vg = new VTDGen();
        boolean parseSuccess;   
        
        if(useLocalXSDCache) {
            //Windows platform can't store profiles in the form of profileID -> illegal file name.
            //xsd has to be normalised
            if(MetadataImporter.IS_UNIX_PLATFORM)
            	xsd = xsd.replaceAll(":", "_");     
            
            parseSuccess = vg.parseFile(Thread.currentThread().getContextClassLoader().getResource("testProfiles/"+xsd+".xsd").getPath(), true);
        } else {
            parseSuccess = vg.parseHttpUrl(MetadataImporter.config.getComponentRegistryProfileSchema(xsd), true);
        }
            
        if (!parseSuccess) {
            LOG.error("Cannot create ConceptLink Map from xsd (xsd is probably not reachable): " + xsd + ". All metadata instances that use this xsd will not be imported correctly.");
            return result; //return empty map, so the incorrect xsd is not tried for all metadata instances that specify it.
        }
        VTDNav vn = vg.getNav();
        AutoPilot ap = new AutoPilot(vn);
        ap.selectElement("xs:element");
        Deque<Token> elementPath = new LinkedList<>();
        while (ap.iterate()) {
            int i = vn.getAttrVal("name");
            if (i != -1) {
                String elementName = vn.toNormalizedString(i);
                updateElementPath(vn, elementPath, elementName);
                int datcatIndex = getDatcatIndex(vn);
                if (datcatIndex != -1) {
                    String conceptLink = vn.toNormalizedString(datcatIndex);
                    String xpath = createXpath(elementPath, null);
                    List<String> values = result.get(conceptLink);
                    if (values == null) {
                        values = new ArrayList<>();
                        result.put(conceptLink, values);
                    }
                    values.add(xpath);
                }

                // look for associated attributes with concept links
                vn.push();
                AutoPilot attributeAutopilot = new AutoPilot(vn);
                attributeAutopilot.declareXPathNameSpace("xs", "http://www.w3.org/2001/XMLSchema");

                try {
                    attributeAutopilot.selectXPath("./xs:complexType/xs:simpleContent/xs:extension/xs:attribute | ./xs:complexType/xs:attribute");
                    while (attributeAutopilot.evalXPath() != -1) {
                        int attributeDatcatIndex = getDatcatIndex(vn);
                        int attributeNameIndex = vn.getAttrVal("name");

                        if (attributeNameIndex != -1 && attributeDatcatIndex != -1) {
                            String attributeName = vn.toNormalizedString(attributeNameIndex);
                            String conceptLink = vn.toNormalizedString(attributeDatcatIndex);

                            String xpath = createXpath(elementPath, attributeName);
                            List<String> values = result.get(conceptLink);
                            if (values == null) {
                                values = new ArrayList<>();
                                result.put(conceptLink, values);
                            }
                            values.add(xpath);
                        }
                    }
                } catch (XPathParseException | XPathEvalException | NavException e) {
                    LOG.error("Cannot extract attributes for element "+elementName+". Will continue anyway...", e);
                }

                // returning to normal element-based workflow
                vn.pop();
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
        result = vn.getAttrValNS("http://www.clarin.eu/cmd/1", "ConceptLink");
        if (result == -1) {
            result = vn.getAttrVal("cmd:ConceptLink");
        }
        return result;
    }

    /**
     * Given an xml-token path thingy create an xpath.
     *
     * @param elementPath
     * @param attributeName will be appended as attribute to XPath expression if not null
     * @return
     */
    private String createXpath(Deque<Token> elementPath, String attributeName) {
        StringBuilder xpath = new StringBuilder("/cmd:CMD/cmd:Components/");
        for (Token token : elementPath) {
            xpath.append("cmdp:").append(token.name).append("/");
        }

        if (attributeName != null) {
            return xpath.append("@").append(attributeName).toString();
        } else {
            return xpath.append("text()").toString();
        }
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

    public static void printMapping(File file) throws IOException {
        Set<String> xsdNames = INSTANCE.mapping.keySet();
        FileWriter fileWriter = new FileWriter(file);
        fileWriter.append("This file is generated on " + DateFormat.getDateTimeInstance().format(new Date())
                + " and only used to document the mapping.\n");
        fileWriter.append("This file contains xsd name and a list of conceptName with xpath mappings that are generated.\n");
        fileWriter.append("---------------------\n");
        fileWriter.flush();
        for (String xsd : xsdNames) {
            FacetMapping facetMapping = INSTANCE.mapping.get(xsd);
            fileWriter.append(xsd + "\n");
            for (FacetConfiguration config : facetMapping.getFacets()) {
                fileWriter.append("FacetName:" + config.getName() + "\n");
                fileWriter.append("Mappings:\n");
                for (String pattern : config.getPatterns()) {
                    fileWriter.append("    " + pattern + "\n");
                }
            }
            fileWriter.append("---------------------\n");
            fileWriter.flush();
        }
        fileWriter.close();
    }
}
