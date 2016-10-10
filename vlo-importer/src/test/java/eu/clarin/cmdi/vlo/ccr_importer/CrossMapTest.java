package eu.clarin.cmdi.vlo.ccr_importer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.util.List;

import org.apache.solr.common.SolrInputDocument;
import org.junit.Before;
import org.junit.Test;

import eu.clarin.cmdi.vlo.FacetConstants;
import eu.clarin.cmdi.vlo.importer.CCRImporter;
import eu.clarin.cmdi.vlo.importer.CMDIData;
import eu.clarin.cmdi.vlo.importer.CMDIDataProcessor;
import eu.clarin.cmdi.vlo.importer.ImporterTestcase;
import eu.clarin.cmdi.vlo.importer.MetadataImporter;
import eu.clarin.cmdi.vlo.importer.Resource;

public class CrossMapTest extends ImporterTestcase {

	@Before
	@Override
	public void setup() throws Exception {
		super.setup();
		// make sure the mapping file for testing is used
		config.setFacetConceptsFile(getTestFacetConceptFilePath());
	}

	private CMDIDataProcessor getDataParser() {
		return new CCRImporter(MetadataImporter.POST_PROCESSORS, config, true);
	}

	//@Test
	public void testCreateCMDIDataFromCorpus() throws Exception {
		String content = "";
		content += "<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n";
		content += "<CMD xmlns=\"http://www.clarin.eu/cmd/1\" xmlns:cmdp=\"http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/clarin.eu:cr1:p_1387365569663\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\">\n";
		content += "   <Header>\n";
		content += "      <MdCreationDate>2003-01-14</MdCreationDate>\n";
		content += "      <MdSelfLink>test-hdl:1839/00-0000-0000-0000-0001-D</MdSelfLink>\n";
		content += "      <MdProfile>clarin.eu:cr1:p_1387365569663</MdProfile>\n";
		content += "   </Header>\n";
		content += "   <Resources>\n";
		content += "      <ResourceProxyList>\n";
		content += "         <ResourceProxy id=\"d28635e19\">\n";
		content += "            <ResourceType>Metadata</ResourceType>\n";
		content += "            <ResourceRef>../acqui_data/Corpusstructure/acqui.imdi.cmdi</ResourceRef>\n";
		content += "         </ResourceProxy>\n";
		content += "      </ResourceProxyList>\n";
		content += "      <JournalFileProxyList/>\n";
		content += "      <ResourceRelationList/>\n";
		content += "   </Resources>\n";
		content += "   <Components>\n";
		content += "      <cmdp:imdi-corpus>\n";
		content += "         <cmdp:Corpus>\n";
		content += "            <cmdp:Name>MPI corpora</cmdp:Name>\n";
		content += "            <cmdp:Title>Corpora of the Max-Planck Institute for Psycholinguistics</cmdp:Title>\n";
		content += "            <cmdp:CorpusLink Name=\"Acquisition\">../acqui_data/Corpusstructure/acqui.imdi</cmdp:CorpusLink>\n";
		content += "            <cmdp:CorpusLink Name=\"Comprehension\">../Comprehension/Corpusstructure/comprehension.imdi</cmdp:CorpusLink>\n";
		content += "            <cmdp:CorpusLink Name=\"Language and Cognition\">../lac_data/Corpusstructure/lac.imdi</cmdp:CorpusLink>\n";
		content += "            <cmdp:descriptions>\n";
		content += "               <cmdp:Description LanguageId=\"\">IMDI corpora</cmdp:Description>\n";
		content += "               <cmdp:Description LanguageId=\"\"/>\n";
		content += "            </cmdp:descriptions>\n";
		content += "         </cmdp:Corpus>\n";
		content += "      </cmdp:imdi-corpus>\n";
		content += "   </Components>\n";
		content += "</CMD>\n";
		File cmdiFile = createCmdiFile("testCorpus", content);
		CMDIDataProcessor processor = getDataParser();
		CMDIData data = processor.process(cmdiFile);
		assertEquals("test-hdl_58_1839_47_00-0000-0000-0000-0001-D", data.getId());
		List<Resource> resources = data.getMetadataResources();
		assertEquals(3, resources.size());
		Resource res = resources.get(0);
		assertEquals("../acqui_data/Corpusstructure/acqui.imdi.cmdi", res.getResourceName());
		assertEquals(null, res.getMimeType());
		assertEquals(0, data.getDataResources().size());
		SolrInputDocument doc = data.getSolrDocument();
		assertTrue(doc.getFieldValues(FacetConstants.FIELD_CLARIN_PROFILE).contains("imdi-corpus"));
		assertNotNull(doc);
	}
}
