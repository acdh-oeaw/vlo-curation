package eu.clarin.cmdi.vlo;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class StringUtils {
	
	public static final String CAMEL_CASE_PATTERN = "(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])"; 

    /**
     * Converts a newlines in to html (&lt;br/&gt;), not using &lt;p&gt; because it renders differently on firefox/safari/chrome/ie etc...
     * Heavily inspired on: 
     * {@link org.apache.wicket.markup.html.basic.MultiLineLabel} 
     * {@link org.apache.wicket.util.string.Strings#toMultilineMarkup(CharSequence)} 
     * 
     * @param s
     */
    public static CharSequence toMultiLineHtml(CharSequence s) {
        StringBuilder result = new StringBuilder();
        if (s == null) {
            return result;
        }
        int newlineCount = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
            case '\n':
                newlineCount++;
                break;
            case '\r':
                break;
            default:
                if (newlineCount == 1) {
                    result.append("<br/>");
                } else if (newlineCount > 1) {
                    result.append("<br/><br/>");
                }
                result.append(c);
                newlineCount = 0;
                break;
            }
        }
        if (newlineCount == 1) {
            result.append("<br/>");
        } else if (newlineCount > 1) {
            result.append("<br/><br/>");
        }
        return result;
    }
    
    private static final Set<Character> reservedCharacters = new HashSet<Character>(Arrays.asList('!', '*', '\'', '(',
            ')', ';', ':', '@', '&', '=', '+', '$', ',', '/', '?', '#', '[', ']'));
        
    /**
     * Return normalized String where all reserved characters in URL encoding
     * are replaced by their ASCII code (in underscores)
     *
     * @param idString String that will be normalized
     * @return normalized version of value where all reserved
     * characters in URL encoding are replaced by their ASCII code
     */
    public static String normalizeIdString(String idString) {
        StringBuilder normalizedString = new StringBuilder();
        for (int i = 0; i < idString.length(); i++) {
            Character character = idString.charAt(i);
            if (reservedCharacters.contains(character)) {
                normalizedString.append("_").append((int) character).append("_");
            } else {
                normalizedString.append(character);
            }
        }
        return normalizedString.toString();
    }
    
    public static String uncapitalizeFirstLetter(String value){
		return value.substring(0, 1).toLowerCase() + value.substring(1);
	}
    
    public static String capitalizeFirstLetter(String value){
		return value.substring(0, 1).toUpperCase() + value.substring(1);
	}
    
    /**
     * Return camel cased String for space separated String
     * 
     * e.g. foo bar Baz -> fooBarBaz
     *
     * @param idString String that will be normalized
     * @return normalized version of value where all reserved
     * characters in URL encoding are replaced by their ASCII code
     */
    
    public static String camelCaseIt(String val){
		
		Pattern firstAfterSpace = Pattern.compile("\\s+\\w");
		Matcher matcher = firstAfterSpace.matcher(val);
		String res = "";
		int ind = 0;
		while(matcher.find()){
			res += val.substring(ind, matcher.start());
			String gr = matcher.group();
			res += gr.substring(gr.length() - 1).toUpperCase();
			ind = matcher.end();
		}
		return res + val.substring(ind);
	}
    
    public static String createStringFromArray(String... values){
    	String res = "";
    	
    	for(String str: values){
    		res += str;
    	}
    	
    	return res;
    	
    }
    

}
