package eu.clarin.cmdi.vlo.importer;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

public class CCRService {
	
	private static final String CCR_REST_URL = "https://openskos.meertens.knaw.nl/ccr/api/find-concepts?fl=prefLabel@en&format=json&q=uri:";
	
	private static final Map<String, String> ccrCache = new HashMap<String, String>();
	
	
	public static String getPrefLabel(String conceptUri){
		
		//try with cache
		String out = ccrCache.get(conceptUri);
		
		if(out != null){
			return out;
		}
		
		try{			
			CloseableHttpClient httpClient = HttpClientBuilder.create().build();
			URL url = new URL(CCR_REST_URL + '"' + conceptUri + '"');
			HttpURLConnection conn = (HttpURLConnection) url.openConnection();
			conn.setRequestMethod("GET");
			conn.setRequestProperty("Accept", "application/json");
			
			//System.out.println("URL:" + conn.getURL());

			if (conn.getResponseCode() != 200) {
				throw new RuntimeException("Failed : HTTP error code : "
						+ conn.getResponseCode());
			}

			BufferedReader br = new BufferedReader(new InputStreamReader(
				(conn.getInputStream())));

			out = br.readLine();

			br.close();
			conn.disconnect();
			
			int indOfPrefLab = out.indexOf("prefLabel@en");
			
			if(indOfPrefLab > 0){
				String prefLab = out.substring(out.indexOf('[', indOfPrefLab) + 2, out.indexOf(']', indOfPrefLab) - 1);
				ccrCache.put(conceptUri, prefLab);
				return prefLab;
			}
			
			throw new RuntimeException("Request to CCR finished with error: " + out);
			
		}catch(Exception e){
			throw new RuntimeException(e);
		}
	}

}
