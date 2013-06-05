package org.nkuznetsov.lib.dman;

import java.io.IOException;

import org.apache.http.Header;
import org.apache.http.HttpException;
import org.apache.http.HttpRequest;
import org.apache.http.HttpRequestInterceptor;
import org.apache.http.HttpResponse;
import org.apache.http.HttpResponseInterceptor;
import org.apache.http.client.entity.GzipDecompressingEntity;
import org.apache.http.protocol.HttpContext;

public class GZIPHeaderInterceptor implements HttpRequestInterceptor, HttpResponseInterceptor
{
	private static String CONTENT_ENCODING = "Content-Encoding";
	private static String ACCEPT_ENCODING = "Accept-Encoding";
	private static String GZIP = "gzip";
	
	
	public void process(HttpResponse response, HttpContext context) throws HttpException, IOException
	{
		Header encodingHeader = response.getFirstHeader(CONTENT_ENCODING);
		
		if (encodingHeader != null && GZIP.equalsIgnoreCase(encodingHeader.getValue()))
			response.setEntity(new GzipDecompressingEntity(response.getEntity()));
	}

	public void process(HttpRequest request, HttpContext context) throws HttpException, IOException
	{
		request.addHeader(ACCEPT_ENCODING, GZIP);
	}
}
