package org.nkuznetsov.lib.dman;

import java.io.File;
import java.io.InputStream;
import java.util.Vector;
import org.apache.http.Header;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicHeader;
import org.nkuznetsov.lib.cman.Cache;
import org.nkuznetsov.lib.cman.utils.CManUtils;
import org.nkuznetsov.lib.dman.utils.DManUtils;
import android.content.Context;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class DownloadManager
{
	private static final int RETRIES = 3;
	
	private static Cache cache;
	private Method method = Method.GET;
	private String requestURL;
	private MultipartEntity multipartEntity;
	private Vector<Header> httpHeaders;
	private Exception executeException;
	private InputStream responseStream;
	
	public DownloadManager(String url)
	{
		requestURL = url;
	}
	
	public DownloadManager(String url, Method method)
	{
		this(url);
		this.method = method;
	}

	public String getRequestURL()
	{
		return requestURL;
	}
	
	public InputStream getResponseStream()
	{
		return responseStream;
	}
	
	public Exception getExecuteException()
	{
		return executeException;
	}
	
	private void addMultipart(String field, ContentBody body)
	{
		if (multipartEntity == null) multipartEntity = new MultipartEntity();
		multipartEntity.addPart(field, body);
		method = Method.POST;
	}
	
	/**
	 * Add file to upload to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for file content
	 * @param file - file object, wich will be uploaded to server
	 */
	public void addFile(String field, File file)
	{
		addMultipart(field, new FileBody(file, 
				MimeTypeMap.getSingleton().getMimeTypeFromExtension(DManUtils.getFileExtensionWithoutDot(file))));
	}
	
	/**
	 * Add byte array to upload to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for byte array content
	 * @param data - byte array, wich will be uploaded to server
	 * @param filename - filename of content
	 */
	public void addByteArray(String field, byte[] data, String filename)
	{
		addMultipart(field, new ByteArrayBody(data, filename));
	}
	
	/**
	 * Add input stream to upload it content to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for input stream content
	 * @param in - input stream, wich content will be uploaded to server
	 * @param filename - filename of stream
	 */
	public void addInputStream(String field, InputStream in, String filename)
	{
		addMultipart(field, new InputStreamBody(in, filename));
	}
	
	public void addString(String field, String value)
	{
		try
		{
			if (multipartEntity == null) multipartEntity = new MultipartEntity();
			multipartEntity.addPart(field, new StringBody(value));
			method = Method.POST;
		}
		catch (Exception e) { Log.e("DMAN", e.toString()); }
	}
	
	public void addHeader(String name, String value)
	{
		if (httpHeaders == null) httpHeaders = new Vector<Header>();
		httpHeaders.add(new BasicHeader(name, value));
	}
	
	public int execute(int cacheTime)
	{	
		if (multipartEntity != null) method = Method.POST;
		String md5URL = CManUtils.MD5Hash(requestURL);
		
		if (cache != null && method.equals(Method.GET))
		{
			if (cache.isCached(md5URL))
			{
				if (cacheTime > 0)
				{
					responseStream = cache.getStream(md5URL);
					if (responseStream != null) return 0;
				}
				else cache.remove(md5URL);
			}
		}
		
		HttpUriRequest request;
		
		if (method.equals(Method.GET)) request = new HttpGet(requestURL);
		else request = new HttpPost(requestURL);
		
		if (multipartEntity != null) ((HttpPost)request).setEntity(multipartEntity);
		if (httpHeaders != null) for (Header header : httpHeaders) request.addHeader(header);
		
		int retriesCount = RETRIES;
		int responseCode = -1;
		
		while (retriesCount-- != 0)
		{			
			try
			{
				Log.d("DownloadManager: ", "execute(" + request.getURI() + ")");
				HttpResponse response = new DefaultHttpClient().execute(request);
				responseCode = response.getStatusLine().getStatusCode();
				if (response.getEntity() != null)
				{
					responseStream = response.getEntity().getContent();
					long length = response.getEntity().getContentLength();
					if (responseCode == 200 && cacheTime > 0 && cache != null && 
							cache.getMaxNewCacheFileSize() > length)
					{
						responseStream = cache.put(md5URL, responseStream, cacheTime);
						if (responseStream == null) continue;
					}
				}
				break;
			}
			catch (Exception e) 
			{
				Log.e("DMAN", e.toString());
				executeException = e;
				responseCode = -1;
			}
		}
		return responseCode;
	}
	
	public static InputStream get(String url)
	{
		return get(url, 0);
	}
	
	public static InputStream get(String url, int cacheTime)
	{
		DownloadManager dm = new DownloadManager(url, Method.GET);
		int code = dm.execute(cacheTime);
		if (code == 0 || code == 200)
		{
			return dm.getResponseStream();
		}
		return null;
	}
	
	public static enum Method
	{
		GET, POST;
	}
	
	public static void initCache(Context context, CacheType type)
	{
		if (type.equals(CacheType.file)) cache = Cache.createCacheFiles(context);
		if (type.equals(CacheType.database)) cache = Cache.createCacheDatabase(context);
	}
	
	public static void initCache(Context context)
	{
		initCache(context, CacheType.file);
	}
	
	public static Cache getCache()
	{
		return cache;
	}
	
	public static enum CacheType
	{
		file, database;
	}
}