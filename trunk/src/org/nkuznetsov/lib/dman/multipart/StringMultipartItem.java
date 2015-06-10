package org.nkuznetsov.lib.dman.multipart;

import java.io.IOException;
import java.io.OutputStream;
import java.io.Writer;

public class StringMultipartItem extends MultipartItem
{
	String value;
	
	public StringMultipartItem(String field, String value)
	{
		super(field);
		this.value = value;
	}
	
	@Override
	public void write(OutputStream os, Writer writer, String boundary) throws IOException
	{
		writer
			.append("--" + boundary).append(LINE_FEED)
			.append("Content-Disposition: form-data; name=\"" + field + "\"")
	        .append(LINE_FEED)
	    	.append("Content-Type: text/plain; charset=UTF-8")
	    	.append(LINE_FEED)
	    	.append(LINE_FEED)
	    	.append(value)
	    	.append(LINE_FEED)
	    	.flush();
	}
}
