package org.nkuznetsov.lib.dman;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

public class NameValuePair
{
	String name, value;
	
	public NameValuePair(String name, String value)
	{
		this.name = name;
		this.value = value;
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
