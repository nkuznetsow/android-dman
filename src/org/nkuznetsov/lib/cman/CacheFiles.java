package org.nkuznetsov.lib.cman;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;

import org.nkuznetsov.lib.cman.utils.CManUtils;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Environment;
import android.os.StatFs;

public class CacheFiles extends Cache
{
	private static final String CACHE_DIR = "CacheManager_f3/";
	private static final String CACHE_PREFFIX = "c_";
	
	private MountStateReceiver mountStateReceiver = new MountStateReceiver();
	private HashMap<String, File> cacheList = new HashMap<String, File>(2048);
	private File usedCachePath;
	
	public CacheFiles(Context context) 
	{
		super(context);
		mountStateReceiver.register();
		
		init();
	}
	
	private void init()
	{	
		synchronized (cacheList) 
		{
			cacheList.clear();
			
			String name;
			int start, end;
			
			for (File cacheFile : getCacheFiles()) 
			{
				if (!isExpired(cacheFile))
				{
					name = cacheFile.getName();
					start = name.indexOf("_") + 1;
					end = name.lastIndexOf("_");
					if (start < end) 
						cacheList.put(name.substring(start, end), cacheFile.getAbsoluteFile());
				}
				else cacheFile.delete();
			}
		}
		
		usedCachePath = null;
		usedCachePath = getExternalCacheDirectory();
		if (usedCachePath == null) usedCachePath = getInternalCacheDirectory();
		if (usedCachePath == null) throw new IllegalStateException("Cannot find any cache directory");
	}
	
	private ArrayList<File> getCacheFiles()
	{
		ArrayList<File> cacheFiles = new ArrayList<File>(2048);
		
		File[] avaliableCachePaths = new File[2];
		avaliableCachePaths[0] = getInternalCacheDirectory();
		avaliableCachePaths[1] = getExternalCacheDirectory();
		
		for (File cachPath : avaliableCachePaths) 
		{
			if (cachPath != null)
			{
				File[] list = cachPath.listFiles(cahceFileFilter);
				if (list != null) cacheFiles.addAll(Arrays.asList(list));
			}
		}
		return cacheFiles;
	}
	
	private static final FileFilter cahceFileFilter = new FileFilter()
	{
		public boolean accept(File file)
		{
			return file.getName().startsWith(CACHE_PREFFIX);
		}
	};
	
	private File getInternalCacheDirectory()
	{
		File path = new File(context.getCacheDir(), CACHE_DIR);
		if (!path.exists()) path.mkdirs();
		if (!path.canWrite()) return null;
		return path;
	}
	
	private File getExternalCacheDirectory()
	{
		if (Environment.MEDIA_MOUNTED.equals(Environment.getExternalStorageState()))
		{
			File path = new File(Environment.getExternalStorageDirectory(), 
					"/Android/data/" + context.getPackageName() + "/cache/" + CACHE_DIR);
			if (!path.exists()) path.mkdirs();
			if (!path.canWrite()) return null;
			return path;
		}
		return null;
	}
	
	private boolean isExpired(File file)
	{
		String name = file.getName();
		int start = name.lastIndexOf("_") + 1;
		if (start > 0)
		{
			long exp = Long.valueOf(name.substring(start));
			long now = new Date().getTime();
			return exp <= now;
		}
		return false;
	}
	
	@Override
	public long getMaxNewCacheFileSize()
	{
		if (!usedCachePath.canWrite()) init();
		if (!usedCachePath.canWrite()) return 0;
		
		StatFs stat = new StatFs(usedCachePath.getPath());
		long avaliableBlocks = stat.getAvailableBlocks();
		long blockSize = stat.getBlockSize();
		double maxPresentsOfUsage = 0.8;
		double freeSize = avaliableBlocks * blockSize * maxPresentsOfUsage;
		return Math.round(freeSize);
	}
	
	@Override
	public void destroy() 
	{
		mountStateReceiver.unregister();
	}
	
	@Override
	public boolean isCached(String url) 
	{
		synchronized (cacheList)
		{
			if (cacheList.containsKey(url))
			{
				File file = cacheList.get(url);
				if (file.exists() && !isExpired(file)) return true;
				else remove(url);
			}
			return false;
		}
	}

	@Override
	public void clear() 
	{
		synchronized (cacheList)
		{
			for (File file : getCacheFiles()) file.delete();
		}
		init();
	}

	@Override
	public void clearExpired() 
	{
		init();
	}

	@Override
	public byte[] get(String url) 
	{
		InputStream is = getStream(url);
		try
		{
			byte[] bytes = CManUtils.readBytes(is);
			is.close();
			return bytes;
		}
		catch (Exception e) {}
		return null;
	}
	
	@Override
	public InputStream getStream(String url)
	{
		if (isCached(url))
		{
			File inFile = cacheList.get(url);
			if (!isExpired(inFile))
			{
				try
				{
					return new FileInputStream(inFile);
				} 
				catch (Exception e) {}
			}
			else remove(url);
		}
		return null;
	}

	@Override
	public void put(String url, byte[] data, int expired) 
	{		
		try
		{
			put(url, new ByteArrayInputStream(data), expired);
		}
		catch (Exception e) {}
	}
	
	@Override
	public InputStream put(String url, InputStream is, int expired) throws IOException
	{
		File outFile = new File(usedCachePath, CACHE_PREFFIX + url + "_" + String.valueOf(getExpiredTime(expired)));
		try
		{
			FileOutputStream os = new FileOutputStream(outFile);
			byte[] buffer = new byte[1024 * 8];
			int readed = 0;
			while ((readed = is.read(buffer)) != -1) os.write(buffer, 0, readed);
			os.flush();
			os.close();
			is.close();
			synchronized (cacheList)
			{
				cacheList.put(url, outFile);
			}
		}
		catch (Exception e) 
		{
			outFile.delete();
		}
		return getStream(url);
	};
	
	@Override
	public void remove(String url)
	{
		File file = cacheList.remove(url);
		if (file != null) file.delete();
	}
	
	private class MountStateReceiver extends BroadcastReceiver
	{
		private boolean registred = false;
		
		@Override
		public void onReceive(Context context, Intent intent) 
		{
			init();
		}
		
		public void register()
		{
			IntentFilter intentFilter = new IntentFilter();
			intentFilter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
			intentFilter.addAction(Intent.ACTION_MEDIA_EJECT);
			intentFilter.addAction(Intent.ACTION_MEDIA_MOUNTED);
			intentFilter.addAction(Intent.ACTION_MEDIA_REMOVED);
			intentFilter.addDataScheme("file");
			context.registerReceiver(this, intentFilter);
			registred = true;
		}
		
		public void unregister()
		{
			if (registred) context.unregisterReceiver(this);
		}
	}
}
