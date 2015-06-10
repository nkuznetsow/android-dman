package org.nkuznetsov.lib.dman;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicLong;
import javax.net.ssl.HostnameVerifier;
import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSession;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;
import org.nkuznetsov.lib.cman.Cache;
import org.nkuznetsov.lib.dman.multipart.ByteArrayMultipartItem;
import org.nkuznetsov.lib.dman.multipart.FileMultipartItem;
import org.nkuznetsov.lib.dman.multipart.MultipartItem;
import org.nkuznetsov.lib.dman.multipart.StreamMultipartItem;
import org.nkuznetsov.lib.dman.multipart.StringMultipartItem;
import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

public class DownloadManager
{
	private static final int RETRIES = 3;
	
	private static Cache cache;
	private static boolean debug = false;
	private static String boundary = "------AndroidDeviceshjgjas";
	
	private HttpURLConnection urlConnection;
	private Method method = Method.GET;
	private Method forceMethod;
	private String requestURL;
	
	private ArrayList<NameValuePair> postStrings = new ArrayList<NameValuePair>();
	private ArrayList<MultipartItem> postMultipart1 = new ArrayList<MultipartItem>();
	
	private Exception executeException;
	private InputStream responseStream;
	private long length;
	
	public DownloadManager(String url)
	{
		this(url, Method.GET);
	}
	
	public DownloadManager(String url, Method method)
	{
		this.requestURL = url;
		this.method = method;
		
		try
		{
			urlConnection = (HttpURLConnection) new URL(url).openConnection();
			urlConnection.setReadTimeout(10000);
			urlConnection.setConnectTimeout(15000);
			urlConnection.setDoInput(true);
		}
		catch (Exception e)
		{
			executeException = e;
		}
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
	
	/**
	 * Add file to upload to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for file content
	 * @param file - file object, wich will be uploaded to server
	 */
	public void addFile(String field, File file)
	{
		if (file == null || !file.exists()) return;
		
		postMultipart1.add(new FileMultipartItem(field, file.getName(), file));
	}
	
	/**
	 * Add byte array to upload to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for byte array content
	 * @param data - byte array, wich will be uploaded to server
	 * @param filename - filename of content
	 */
	public void addByteArray(String field, byte[] data, String filename)
	{
		if (data == null) return;
		
		postMultipart1.add(new ByteArrayMultipartItem(field, filename, data));
	}
	
	/**
	 * Add input stream to upload it content to server. Request method will be changed to POST, cache will be disabled
	 * @param field - field name for input stream content
	 * @param in - input stream, wich content will be uploaded to server
	 * @param filename - filename of stream
	 */
	public void addInputStream(String field, final InputStream in, String filename)
	{
		if (in == null) return;
		
		postMultipart1.add(new StreamMultipartItem(field, filename)
		{
			@Override
			protected InputStream getInputStream() throws FileNotFoundException
			{
				return in;
			}
		});
	}
	
	public void addString(String field, String value)
	{
		postStrings.add(new NameValuePair(field, value));
	}
	
	public void addStringMultypart(String field, String value)
	{
		postMultipart1.add(new StringMultipartItem(field, value));
	}
	
	public void addHeader(String name, String value)
	{
		urlConnection.addRequestProperty(name, value);
	}
	
	public int execute(int cacheTime)
	{
		if (executeException != null) return -1;
		
		ArrayList<NameValuePair> postLogs = new ArrayList<NameValuePair>();
		
		if (postMultipart1.size() > 0 || postStrings.size() > 0) method = Method.POST;
	
		if (forceMethod != null) method = forceMethod;
		
		int retriesCount = RETRIES;
		int responseCode = -1;
		
		while (retriesCount-- != 0)
		{			
			try
			{
				if (method == Method.GET)
				{
					if (cache != null)
					{
						if (cacheTime > 0)
						{
							responseStream = cache.getStream(requestURL);
							if (responseStream != null) return 0;
						}
						else cache.remove(requestURL);
					}
					
					urlConnection.setRequestMethod("GET");
				}
				else if (method == Method.POST || method == Method.PUT)
				{
					urlConnection.setDoOutput(true);
					
					urlConnection.setRequestMethod(method == Method.POST ? "POST" : "PUT");
					
					if (postMultipart1.size() > 0)
						urlConnection.setRequestProperty("Content-Type", "multipart/form-data; boundary=" + boundary);
					else urlConnection.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
					
					OutputStream os = urlConnection.getOutputStream();
					BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
					
					if (postMultipart1.size() > 0)
					{
						writer.append(MultipartItem.LINE_FEED);
						writer.append(MultipartItem.LINE_FEED);
						writer.flush();
						
						for (NameValuePair pair : postStrings)
						{
							postLogs.add(pair);
							new StringMultipartItem(pair.getName(), pair.getValue()).write(os, writer, boundary);
						}
						
						for (MultipartItem multipartItem : postMultipart1)
						{
							postLogs.add(new NameValuePair(multipartItem.getField(), "[DATA]"));
							multipartItem.write(os, writer, boundary);
						}
						
						writer.append("--" + boundary + "--");
						writer.flush();
					}
					else if (postStrings.size() > 0)
					{
						StringBuilder sb = new StringBuilder();
						
						for (int i = 0; i < postStrings.size(); i ++)
						{
							NameValuePair postString = postStrings.get(i);
							postLogs.add(postString);
							sb.append(postString.toURLEncodedString());
							if (i != postStrings.size() - 1) sb.append("&");
						}
						
						writer.write(sb.toString());
						writer.flush();
					}
					
					writer.close();
					os.close();	
				}
		
				if (debug) Log.d(method.name(), "" + requestURL + (postLogs.size() > 0 ? (" params: " + postLogs) : ""));
				
				
				urlConnection.connect();
				
				responseCode = urlConnection.getResponseCode();
				
				responseStream = urlConnection.getInputStream();
				
				length = urlConnection.getContentLength();
				
				if (responseCode == 200 && 
						cacheTime > 0 && 
						cache != null && 
						cache.getMaxNewCacheFileSize() > length)
				{
					responseStream = cache.put(requestURL, responseStream, cacheTime);
					if (responseStream == null) continue;
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
	
	public long getLength()
	{
		return length;
	}
	
	public void setMethod(Method method)
	{
		this.forceMethod = method;
	}
	
	public static InputStream get(String url) throws MalformedURLException, IOException
	{
		return get(url, 0, null);
	}
	
	public static InputStream get(String url, AtomicLong length) throws MalformedURLException, IOException
	{
		return get(url, 0, length);
	}
	
	public static InputStream get(String url, int cacheTime) throws MalformedURLException, IOException
	{
		return get(url, cacheTime, null);
	}
	
	public static InputStream get(String url, int cacheTime, AtomicLong length) throws MalformedURLException, IOException
	{
		DownloadManager dm = new DownloadManager(url, Method.GET);
		int code = dm.execute(cacheTime);
		if (code == 0 || code == 200)
		{
			if (length != null) length.set(dm.getLength());
			return dm.getResponseStream();
		}
		return null;
	}
	
	public static enum Method
	{
		GET, POST, PUT, DELETE;
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
	
	public static void setDebug(boolean debug)
	{
		DownloadManager.debug = debug;
	}
	
	static boolean allSSLAccepter;
	static SSLSocketFactory defaultSSlSocketFactory;
	static HostnameVerifier defaultHostNameVerifier;
	
	@SuppressLint("TrulyRandom")
	public static void acceptAllSertificates(boolean accept)
	{
		if (!allSSLAccepter && accept)
		{
			try
			{
				TrustManager[] trustAllCerts = new TrustManager[] {new X509TrustManager() 
		        {
		        	public java.security.cert.X509Certificate[] getAcceptedIssuers() 
		        	{
		                    return null;
		            }
		        	
		        	public void checkClientTrusted(X509Certificate[] certs, String authType) {}
		                
		        	public void checkServerTrusted(X509Certificate[] certs, String authType) {}
		            }
		        };
		 
		        SSLContext sc = SSLContext.getInstance("SSL");
		        sc.init(null, trustAllCerts, new java.security.SecureRandom());
		        
		        defaultSSlSocketFactory = HttpsURLConnection.getDefaultSSLSocketFactory();
		        HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
		        
		        HostnameVerifier allHostsValid = new HostnameVerifier()
		        {
		            public boolean verify(String hostname, SSLSession session) 
		            {
		                return true;
		            }
		        };
		        
		        defaultHostNameVerifier = HttpsURLConnection.getDefaultHostnameVerifier();
		        HttpsURLConnection.setDefaultHostnameVerifier(allHostsValid);
		        
		        allSSLAccepter = true;
			}
			catch (Exception e) {}
		}
		else if (allSSLAccepter && !accept)
		{
			allSSLAccepter = false;
			
			HttpsURLConnection.setDefaultSSLSocketFactory(defaultSSlSocketFactory);
			HttpsURLConnection.setDefaultHostnameVerifier(defaultHostNameVerifier);
		}
	}
	
	public static enum CacheType
	{
		file, database;
	}
}