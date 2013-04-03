package org.nkuznetsov.lib.cman.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CManUtils
{
	private static final int bufferSize = 1024 * 8;
	
	/**
	 * Calculation md5 hash of string
	 * 
	 * @param s - string for hash calculation
	 * @return md5 hash of input string
	 */
	public static String MD5Hash(String s) 
    {
        MessageDigest alg = null;
        StringBuffer hexString = new StringBuffer(32);
        try 
        {
        	alg = MessageDigest.getInstance("MD5");
        	alg.update(s.getBytes());
        	
        	byte[] digest = alg.digest();
        	int length = digest.length;
        	String hex = null;
        	for (int i = 0; i < length; i++)
        	{
        		hex = Integer.toHexString(0xFF & digest[i]);
        		if (hex.length() == 1) hexString.append('0');
        		hexString.append(hex);
        	}
        }
        catch (NoSuchAlgorithmException e) {}
        return hexString.toString();
    }
	
	public static byte[] readBytes(InputStream inputStream) throws IOException 
	{
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		
		byte[] buffer = new byte[bufferSize];
		int readed = 0;
		while ((readed = inputStream.read(buffer)) != -1) 
			byteBuffer.write(buffer, 0, readed);
		
		return byteBuffer.toByteArray();
	}
}
