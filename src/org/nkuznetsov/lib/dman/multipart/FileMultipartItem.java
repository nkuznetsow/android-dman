package org.nkuznetsov.lib.dman.multipart;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

public class FileMultipartItem extends StreamMultipartItem
{
	private File file;

	public FileMultipartItem(String field, String filename, File file)
	{
		super(field, filename);
		this.file = file;
	}

	@Override
	protected InputStream getInputStream() throws FileNotFoundException
	{
		return new FileInputStream(file);
	}

}
