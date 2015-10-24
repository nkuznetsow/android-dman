package org.nkuznetsov.lib.dman.utils;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

import org.nkuznetsov.lib.dman.NameValuePair;

public class URLEncode
{
	public static String format (List <? extends NameValuePair> parameters) 
	{
        final StringBuilder result = new StringBuilder();
        
        for (final NameValuePair parameter : parameters) 
        {
            final String encodedName = encode(parameter.getName());
            final String value = parameter.getValue();
            final String encodedValue = value != null ? encode(value) : "";
            if (result.length() > 0) result.append("&");
            result.append(encodedName);
            result.append("=");
            result.append(encodedValue);
        }
        return result.toString();
    }
	
	private static String encode (final String content) 
	{
        try 
        {
            return URLEncoder.encode(content, "UTF-8");
        } 
        catch (UnsupportedEncodingException problem) 
        {
            throw new IllegalArgumentException(problem);
        }
    }
}
