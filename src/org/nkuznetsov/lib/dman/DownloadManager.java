package org.nkuznetsov.lib.dman;

import java.io.File;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.security.KeyStore;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.HttpVersion;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.conn.params.ConnPerRouteBean;
import org.apache.http.conn.scheme.PlainSocketFactory;
import org.apache.http.conn.scheme.Scheme;
import org.apache.http.conn.scheme.SchemeRegistry;
import org.apache.http.conn.ssl.SSLSocketFactory;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntity;
import org.apache.http.entity.mime.content.ByteArrayBody;
import org.apache.http.entity.mime.content.ContentBody;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.entity.mime.content.InputStreamBody;
import org.apache.http.entity.mime.content.StringBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.conn.tsccm.ThreadSafeClientConnManager;
import org.apache.http.message.BasicHeader;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.CoreConnectionPNames;
import org.apache.http.params.CoreProtocolPNames;
import org.apache.http.params.HttpParams;
import org.apache.http.protocol.HTTP;
import org.nkuznetsov.lib.cman.Cache;
import org.nkuznetsov.lib.dman.utils.DManUtils;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.webkit.MimeTypeMap;

public class DownloadManager
{
	private static final int RETRIES = 3;
	
	private static Cache cache;
	private static final DefaultHttpClient httpClient;
	private static boolean debug = true;
	private static String boundary = "------AndroidDeviceshjgjas";
	
	static
	{
		HttpParams params = new BasicHttpParams();
		params.setParameter(CoreProtocolPNames.PROTOCOL_VERSION, HttpVersion.HTTP_1_1);
		params.setParameter(CoreProtocolPNames.HTTP_CONTENT_CHARSET, HTTP.UTF_8);
		params.setParameter(CoreProtocolPNames.USER_AGENT, "Android_" + Build.VERSION.SDK_INT);
		params.setParameter(CoreConnectionPNames.SO_TIMEOUT, 15000);
		params.setParameter(CoreConnectionPNames.CONNECTION_TIMEOUT, 15000);
		params.setParameter(CoreConnectionPNames.STALE_CONNECTION_CHECK, false);
		params.setParameter(CoreConnectionPNames.TCP_NODELAY, true);
		params.setParameter(ConnManagerParams.MAX_CONNECTIONS_PER_ROUTE, new ConnPerRouteBean(5));
		params.setParameter(ConnManagerParams.MAX_TOTAL_CONNECTIONS, 15);
		
		SchemeRegistry schemeRegistry = new SchemeRegistry();
		schemeRegistry.register(new Scheme("http", PlainSocketFactory.getSocketFactory(), 80));
		schemeRegistry.register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
		
		ThreadSafeClientConnManager mgr = new ThreadSafeClientConnManager(params, schemeRegistry);
		
		httpClient = new DefaultHttpClient(mgr, params);
		
		GZIPHeaderInterceptor gzipHeaderInterceptor = new GZIPHeaderInterceptor();
		
		httpClient.addResponseInterceptor(gzipHeaderInterceptor);
		httpClient.addRequestInterceptor(gzipHeaderInterceptor);
	}
	
	private Method method = Method.GET;
	private String requestURL;
	private ArrayList<BasicNameValuePair> postStrings = new ArrayList<BasicNameValuePair>();
	private HashMap<String, ContentBody> postMultipart = new HashMap<String, ContentBody>();
	private final List<Header> httpHeaders = new ArrayList<Header>();
	private Exception executeException;
	private InputStream responseStream;
	private HttpUriRequest request;
	private HttpResponse response;
	private HttpEntity responseEntity;
	private long length;
	
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
		postMultipart.put(field, body);
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
		postStrings.add(new BasicNameValuePair(field, value));
	}
	
	public void addHeader(String name, String value)
	{
		httpHeaders.add(new BasicHeader(name, value));
	}
	
	public int execute(int cacheTime)
	{
		if (postMultipart.size() > 0 || postStrings.size() > 0) method = Method.POST;
		
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
			
			request = new HttpGet(requestURL);
		}
		else 
		{
			request = new HttpPost(requestURL);
		
			HttpPost postRequest = (HttpPost) request;
		
			if (postMultipart.size() > 0)
			{
				MultipartEntity entity = new MultipartEntity(HttpMultipartMode.BROWSER_COMPATIBLE, boundary, Charset.forName("UTF-8"));
				
				for (BasicNameValuePair pair : postStrings)
				{
					try
					{
						entity.addPart(pair.getName(), new StringBody(pair.getValue()));
					}
					catch (Exception e) {}
				}
				
				for (Iterator<String> keys = postMultipart.keySet().iterator(); keys.hasNext();)
				{
					String key = keys.next();
					entity.addPart(key, postMultipart.get(key));
				}

				postRequest.setEntity(entity);
				postRequest.addHeader("Content-Type", "multipart/form-data; boundary=" + boundary);
			}
			else if (postStrings.size() > 0)
			{
				try
				{
					postRequest.setEntity(new UrlEncodedFormEntity(postStrings, "UTF-8"));
					postRequest.addHeader("Content-Type", "application/x-www-form-urlencoded");
				}
				catch (Exception e) {}
			}
		}
		
		for (Header header : httpHeaders) request.addHeader(header);
		
		int retriesCount = RETRIES;
		int responseCode = -1;
		
		while (retriesCount-- != 0)
		{			
			try
			{
				if (debug) Log.d("DMAN: ", "execute(" + request.getURI() + ")");
				response = httpClient.execute(request);
				responseCode = response.getStatusLine().getStatusCode();
				responseEntity = response.getEntity();
				if (responseEntity != null)
				{
					responseStream = responseEntity.getContent();

					length = responseEntity.getContentLength();
					
					if (responseCode == HttpStatus.SC_OK && 
							cacheTime > 0 && 
							cache != null && 
							cache.getMaxNewCacheFileSize() > length)
					{
						responseStream = cache.put(requestURL, responseStream, cacheTime);
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
	
	public long getLength()
	{
		return length;
	}
	
	public static InputStream get(String url)
	{
		return get(url, 0, null);
	}
	
	public static InputStream get(String url, AtomicLong length)
	{
		return get(url, 0, length);
	}
	
	public static InputStream get(String url, int cacheTime)
	{
		return get(url, cacheTime, null);
	}
	
	public static InputStream get(String url, int cacheTime, AtomicLong length)
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
	
	public static void setDebug(boolean debug)
	{
		DownloadManager.debug = debug;
	}
	
	public static void acceptAllSertificates(boolean accept)
	{
		if (httpClient == null) return;
		if (httpClient.getConnectionManager() == null) return;
		if (httpClient.getConnectionManager().getSchemeRegistry() == null) return;
		Scheme scheme = httpClient.getConnectionManager().getSchemeRegistry().get("https");
		if (accept && !(scheme.getSocketFactory() instanceof AcceptAllSSLSocketFactory)) 
		{
			try
			{
				KeyStore trustStore = KeyStore.getInstance(KeyStore.getDefaultType());
				trustStore.load(null, null);
				SSLSocketFactory sf = new AcceptAllSSLSocketFactory(trustStore);
		        sf.setHostnameVerifier(SSLSocketFactory.ALLOW_ALL_HOSTNAME_VERIFIER);
		        httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", sf, 443));
			}
			catch (Exception e) {}
		}
		if (!accept && !(scheme.getSocketFactory() instanceof SSLSocketFactory)) httpClient.getConnectionManager().getSchemeRegistry().register(new Scheme("https", SSLSocketFactory.getSocketFactory(), 443));
	}
	
	public static enum CacheType
	{
		file, database;
	}
}