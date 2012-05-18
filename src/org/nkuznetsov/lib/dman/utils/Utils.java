package org.nkuznetsov.lib.dman.utils;

import java.io.File;

public class Utils
{	
	public static String getFileExtensionWithoutDot(File file)
	{
		return getFileExtensionWithoutDot(file.getName());
	}
	
	public static String getFileExtensionWithoutDot(String file)
	{
		return file.substring(file.lastIndexOf(".") + 1);
	}
}
