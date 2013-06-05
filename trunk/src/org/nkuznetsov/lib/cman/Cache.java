package org.nkuznetsov.lib.cman;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Bitmap.CompressFormat;
import android.graphics.BitmapFactory;

public abstract class Cache
{
	protected Context context;
	
	public Cache(Context context) 
	{
		this.context = context;
	}
	
	/**
	 *  Destroy cache and free all resources
	 *  
	 */
	public abstract void destroy();
	
	/**
	 *  Check is url is cached or not
	 *  
	 *  @param url - hash of url of file
	 *  
	 *  @return true if cached, false if not
	 */
	public abstract boolean isCached(String url);
	
	/**
	 *  Clear all cache items
	 */
	public abstract void clear();
	
	/**
	 *  Clear expired cache items
	 */
	public abstract void clearExpired();
	
	/**
	 * @return value in butes which cache manager able to cache at this moment
	 */
	public abstract long getMaxNewCacheFileSize();
	
	/**
	 * Remove cache for specified url
	 * @param url - url to remove from cache
	 */
	public abstract void remove(String url);
	
	/**
	 *  Get bytes from cache
	 *  
	 *  @param url - hash of url of file
	 *  
	 *  @return bytes array or null if url was not found in cache
	 */
	public abstract byte[] get(String url);
	
	/**
	 *  Get stream from cache
	 *  
	 *  @param url - hash of url of file
	 *  
	 *  @return input stream or null if url was not found in cache
	 */
	public abstract InputStream getStream(String url);
	
	/**
	 *  Put bytes into cache
	 *  
	 *  @param url - hash of url of file
	 *  @param data - bytes of data
	 *  @param expired - expired time in seconds
	 */
	public abstract void put(String url, byte[] data, int expired);
	
	/**
	 *  Put stream into cache
	 *  
	 *  @param url - hash of url of file
	 *  @param is - input stream with data
	 *  @param expired - expired time in seconds
	 *  @throws IOException
	 */
	public abstract InputStream put(String url, InputStream is, int expired) throws IOException;
	
	/**
	 *  Put bytes into cache
	 *  
	 *  @param url - hash of url of file
	 *  @param bitmap - bitmap
	 *  @param expired - expired time in seconds
	 */
	public void putBitmap(String url, Bitmap bitmap, int expired)
	{
		ByteArrayOutputStream bos = new ByteArrayOutputStream();
		bitmap.compress(CompressFormat.PNG, 0, bos);
		put(url, bos.toByteArray(), expired);
		try
		{
			bos.close();
		}
		catch (IOException e) {}
	}
	
	/**
	 *  Get bitmap from cache
	 *  
	 *  @param url - hash of url of file
	 *  
	 *  @return Bitmap or null if url was not found in cache
	 */
	public Bitmap getBitmap(String url)
	{
		byte[] data = get(url);
		return BitmapFactory.decodeByteArray(data, 0, data.length);
	}
	
	/**
	 *  Create cache instance which uses file storage. It is best way to cache large files like images and etc.
	 *  
	 *  @param context - app context
	 */
	public static Cache createCacheFiles(Context context)
	{
		return new CacheFiles(context);
	}
	
	/**
	 *  Create cache instance which uses database storage. It is best way to cache small files like json and etc.
	 *  
	 *  @param context - app context
	 */
	public static Cache createCacheDatabase(Context context)
	{
		return new CacheDatabase(context);
	}
	
	/**
	 * Returns expired time in milliseconds
	 * @param expired - time to cache data in seconds
	 * @return unix time in milseconds
	 */
	public static long getExpiredTime(int expired)
	{
		return new Date().getTime() + expired * 1000L;
	}
}
