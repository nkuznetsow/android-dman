package org.nkuznetsov.lib.dman.multipart;

import java.io.ByteArrayInputStream;
import java.io.InputStream;

public class ByteArrayMultipartItem extends StreamMultipartItem
{
	private byte[] bytes;

	public ByteArrayMultipartItem(String field, String filename, byte[] bytes)
	{
		super(field, filename);
		this.bytes = bytes;
	}

	@Override
	protected InputStream getInputStream()
	{
		return new ByteArrayInputStream(bytes);
	}

}
