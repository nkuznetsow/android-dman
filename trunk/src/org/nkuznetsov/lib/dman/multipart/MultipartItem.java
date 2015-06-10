package org.nkuznetsov.lib.dman.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;


public abstract class MultipartItem
{
	protected static final String LINE_FEED = "\r\n";
	
	protected String field;
	
	public MultipartItem(String field)
	{
		this.field = field;
	}
	
	public abstract void write(OutputStream os, Writer writer, String boundary) throws IOException;
	
	public String getField()
	{
		return field;
	}
}
