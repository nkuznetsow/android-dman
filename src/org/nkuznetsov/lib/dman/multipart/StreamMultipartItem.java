package org.nkuznetsov.lib.dman.multipart;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.net.URLConnection;

public abstract class StreamMultipartItem extends MultipartItem
{
	protected String filename;
	
	public StreamMultipartItem(String field, String filename)
	{
		super(field);
		this.filename = filename;
	}
	
	protected abstract InputStream getInputStream()  throws IOException;
	
	@Override
	public void write(OutputStream os, Writer writer, String boundary) throws IOException
	{
		writer
			.append("--" + boundary)
			.append(LINE_FEED)
			.append("Content-Disposition: form-data; name=\"" + field + "\"; filename=\"" + filename + "\"")
			.append(LINE_FEED)
			.append("Content-Type: " + URLConnection.guessContentTypeFromName(filename))
			.append(LINE_FEED)
			.append("Content-Transfer-Encoding: binary")
			.append(LINE_FEED)
			.append(LINE_FEED)
			.flush();
		
		InputStream is = getInputStream();
		
		byte[] buffer = new byte[1024 * 8];
		int bytesRead = -1;
		while ((bytesRead = is.read(buffer)) != -1)
			os.write(buffer, 0, bytesRead);
		os.flush();
		is.close();
		
		writer
			.append(LINE_FEED)
			.flush();
	}
}
