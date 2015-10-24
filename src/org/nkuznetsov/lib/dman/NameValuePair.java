package org.nkuznetsov.lib.dman;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Collection;

public class NameValuePair
{
	String name, value;
	
	public NameValuePair(String name, String value)
	{
		this.name = name;
		this.value = value;
	}
	
	public NameValuePair(String name, Collection<String> values)
	{
		this.name = name;
		
		StringBuilder sb = new StringBuilder();
		
		int count = values.size() - 1;
		int i = 0;
		
		for (String value : values)
		{
			sb.append(value);
			if (i < count) sb.append(',');
			i ++;
		}
		
		this.value = sb.toString();
	}
	
	public String getName()
	{
		return name;
	}
	
	public String getValue()
	{
		return value;
	}
	
	public String toURLEncodedString() throws UnsupportedEncodingException
	{
		return name + "=" + URLEncoder.encode(value, "UTF-8");
	}
	
	@Override
	public String toString()
	{
		return name + "=" + value;
	}
}
