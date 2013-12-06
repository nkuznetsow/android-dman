package org.nkuznetsov.lib.cman.utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;

import android.util.SparseArray;

public class CManUtils
{
	private static final int bufferSize = 1024 * 8;
	
	private static final SparseArray<String> md5Cache = new SparseArray<String>();
	
	private static MessageDigest MD5 = null;
	
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
		
        try 
        {
        	if (MD5 == null) MD5 = MessageDigest.getInstance("MD5");
        	
        	MessageDigest alg = (MessageDigest) MD5.clone();
        	alg.update(s.getBytes());
        	
        	StringBuffer hexString = new StringBuffer(32);
        	
        	for (byte b : alg.digest()) hexString.append(intToHexChars(0xFF & b));
        	
        	result = hexString.toString();
        }
        catch (Exception e) {}
        
        if (result == null) result = String.valueOf(s.hashCode());
        md5Cache.put(sHash, result);
       
        return result;
    }
	
	private static char toHexChar(int i) 
	{
		i &= 15;
		return (i < 10) ? (char)(i + 48) : (char)(i + 87); 
	}

	private static char[] intToHexChars(int n) 
	{
		char[] chars = new char[2];
		for (int i = 0; i < 2; i++) 
		{
			chars[1 - i] = toHexChar(n);
			n >>= 4;
		}
		return chars;
	}
	
	public static byte[] readBytes(InputStream is) throws IOException 
	{
		ByteArrayOutputStream byteBuffer = new ByteArrayOutputStream();
		
		ReadableByteChannel ich = Channels.newChannel(is);
		WritableByteChannel och = Channels.newChannel(byteBuffer);
		
		ByteBuffer buffer = ByteBuffer.allocateDirect(bufferSize);
		
		while (ich.read(buffer) > -1 || buffer.position() > 0)
		{
			buffer.flip();
			och.write(buffer);
			buffer.compact();
		}
		
		ich.close();
		och.close();
		
		return byteBuffer.toByteArray();
	}
}
