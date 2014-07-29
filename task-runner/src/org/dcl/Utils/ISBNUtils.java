package org.dcl.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ISBNUtils
{

	public String detectGetISBN(String text)
	{
		String result = "";
		if(text == null)
		{
			return result;
		}
		
		Matcher matcher = Pattern.compile("\\d{12}[X\\d]").matcher(text);
		if(matcher.find())
		{
			result = matcher.group();
		}
		return result;
	}
	
	
	
}
