package eu.clarin.cmdi.vlo;

import java.security.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.TreeMap;


public class Plural2Singular {
	
	public static final Map<String, String> hackList = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	static{
		hackList.put("corpora", "corpus");
		hackList.put("corpus", "corpus");
	}

	public static final Map<String, String> irregularNouns = new TreeMap<String, String>(String.CASE_INSENSITIVE_ORDER);
	static{
		irregularNouns.put("men", "Man");
		irregularNouns.put("women", "Woman");
		irregularNouns.put("children", "Child");
		irregularNouns.put("teeth", "Tooth");
		irregularNouns.put("feet", "Foot");
		irregularNouns.put("people", "Person");
		irregularNouns.put("leaves", "Leaf");
		irregularNouns.put("mice", "Mouse");
		irregularNouns.put("geese", "Goose");
		irregularNouns.put("halves", "Half");
		irregularNouns.put("knives", "Knife");
		irregularNouns.put("wives", "Wife");
		irregularNouns.put("lives", "Life");
		irregularNouns.put("elves", "Elf");
		irregularNouns.put("loaves", "Loaf");
		irregularNouns.put("potatoes", "Potato");
		irregularNouns.put("tomatoes", "Tomato");
		irregularNouns.put("cacti", "Cactus");
		irregularNouns.put("foci", "Focus");
		irregularNouns.put("fungi", "Fungus");
		irregularNouns.put("nuclei", "Nucleus");
		irregularNouns.put("syllabi", "Syllabus"); //syllabuses is also valid 
		irregularNouns.put("analyses", "Analysis");
		irregularNouns.put("diagnoses", "Diagnosis");
		irregularNouns.put("oases", "Oasis");
		irregularNouns.put("theses", "Thesis");
		irregularNouns.put("crises", "Crisis");
		irregularNouns.put("phenomena", "Phenomenon");
		irregularNouns.put("criteria", "Criterion");
		//irregularNouns.put("data", "Datum"); //could be dangerous
		irregularNouns.put("sheep", "Sheep");
		irregularNouns.put("fish ", "Fish");
		irregularNouns.put("deer ", "Deer");
		irregularNouns.put("species ", "Species");
		irregularNouns.put("aircraft ", "Aircraft");
	}
	
	/*
	 * 
	 */
	
	public static String getSingular(String value){
		
		//extract the last camelCase from input
		String[] camelCases = value.split(StringUtils.CAMEL_CASE_PATTERN);
		String lastCamelCase = camelCases[camelCases.length - 1].toLowerCase();
		
		for(String plural: hackList.keySet())
			if(lastCamelCase.equals(plural)){
				camelCases[camelCases.length - 1] = hackList.get(plural);
				return StringUtils.createStringFromArray(camelCases);
			}
		//nice to have but kills performances
//		for(String plural: irregularNouns.keySet())
//			if(lastCamelCase.equals(plural)){
//				camelCases[camelCases.length - 1] = irregularNouns.get(plural);
//				return StringUtils.createStringFromArray(camelCases);
//			}				
		
		if(!value.endsWith("s"))
			return value;		
		
		if(value.endsWith("ies")) //ies -> y
			return value.substring(0, value.length() - 3) + "y";
		
		if(value.endsWith("ches") || value.endsWith("shes")) //remove es
			return value.substring(0, value.length() - 2); 
		
		if(value.endsWith("ses") || value.endsWith("xes") || value.endsWith("zes")) //remove es
			return value.substring(0, value.length() - 2);
		
		return value.substring(0, value.length() - 1);
	}
	
	
	public static void main(String[] args) {
		String value = "foo bar Baz";
		
		long t1 = System.nanoTime();
		value = value.trim();
		value = StringUtils.uncapitalizeFirstLetter(value);
		if(!(value.contains("--") || value.contains("corporaCorpus"))){//handle it by vocabulary			
			value = StringUtils.camelCaseIt(value);
			value = getSingular(value);	
		}
		System.out.println("Elapsed time (ms): " + ((System.nanoTime() - t1) / 1000000));
		System.out.println(value);
	}
	
	
}
