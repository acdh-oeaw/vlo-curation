package eu.clarin.cmdi.vlo.importer;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Enumeration;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;

import org.apache.commons.io.FileUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import eu.clarin.cmdi.vlo.LanguageCodeUtils;
import eu.clarin.cmdi.vlo.config.DefaultVloConfigFactory;
import eu.clarin.cmdi.vlo.config.VloConfig;
import eu.clarin.cmdi.vlo.config.VloConfigFactory;

public abstract class ImporterTestcase {

    private final VloConfigFactory configFactory = new DefaultVloConfigFactory();
    protected VloConfig config;
    
    private static int cnt = 0;

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    protected File createCmdiFile(String name, String content) throws IOException {
        File file = tempFolder.newFile(name + cnt++ + ".cmdi");
        FileUtils.writeStringToFile(file, content, "UTF-8");
        return file;
    }

    @After
    public void cleanup() {
        MetadataImporter.config = null;
    }

    @Before
    public void setup() throws Exception {
        // read the configuration defined in the packaged configuration file
        MetadataImporter.config = configFactory.newConfig();

        // optionally, modify the configuration here

        MetadataImporter.config.setComponentRegistryRESTURL("http://catalog.clarin.eu/ds/ComponentRegistry/rest/registry/profiles/");
        //point to transformed maps
        MetadataImporter.config.setLicenseAvailabilityMapUrl(MetadataImporter.config.getLicenseAvailabilityMapUrl());
        MetadataImporter.config.setLanguageNameVariantsUrl(MetadataImporter.config.getLanguageNameVariantsUrl());
        MetadataImporter. config.setNationalProjectMapping(MetadataImporter.config.getNationalProjectMapping());
        MetadataImporter.config.setOrganisationNamesUrl(MetadataImporter.config.getOrganisationNamesUrl());
        MetadataImporter.config.setResourceClassMapUrl(MetadataImporter.config.getResourceClassMapUrl());
        
        config = MetadataImporter.config;

        MetadataImporter.languageCodeUtils = new LanguageCodeUtils(config);
        
      
    }

    public static String getTestFacetConceptFilePath() {
        try {
            return new File(ImporterTestcase.class.getResource("/facetConceptsTest.xml").toURI()).getAbsolutePath();
        } catch (URISyntaxException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    
    
}
