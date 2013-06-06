package org.nkuznetsov.lib.cman.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import android.util.SparseArray;

public class CManUtils
{
	private static final int bufferSize = 1024 * 8;
	
	private static final SparseArray<String> md5Cache = new SparseArray<String>();
	
	/**
	 * Calculation md5 hash of string
	 * 
	 * @param s - string for hash calculation
	 * @return md5 hash of input string
	 */
	public static String MD5Hash(String s) 
    {
		int sHash = s.hashCode();
		
		String result = md5Cache.get(sHash);
		
		if (result != null) return result;
		
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
        	
        	result = hexString.toString();
        }
        catch (NoSuchAlgorithmException e) {}
        
        if (result == null) result = String.valueOf(s.hashCode());
        md5Cache.put(sHash, result);
       
        return result;
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
