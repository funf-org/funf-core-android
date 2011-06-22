package edu.mit.media.hd.funf;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.HttpParams;

import android.util.Log;

public class IOUtils {
	public static final String TAG = IOUtils.class.getName();
	
	public static String inputStreamToString(InputStream is, String encoding) throws IOException {
		final char[] buffer = new char[0x10000];
		StringBuilder out = new StringBuilder();
		Reader in = new InputStreamReader(is, encoding);
		int read;
		do {
		  read = in.read(buffer, 0, buffer.length);
		  if (read>0) {
		    out.append(buffer, 0, read);
		  }
		} while (read>=0);
		return out.toString();
	}
	
	public static String httpGet(String uri,HttpParams params){
		String responseBody=null;
		HttpClient httpclient = new DefaultHttpClient();
		StringBuilder uriBuilder = new StringBuilder(uri);
		HttpGet httpget = new HttpGet(uriBuilder.toString());
		if (params != null) {
			httpget.setParams(params);
		}
        ResponseHandler<String> responseHandler = new BasicResponseHandler();
        try {
		    responseBody = httpclient.execute(httpget, responseHandler);
		} catch (ClientProtocolException e) {
			Log.e(TAG, "HttpGet Error: ", e);
			responseBody=null;
		} catch (IOException e) {
			Log.e(TAG, "HttpGet Error: ", e);
			responseBody=null;
		} finally{
	        httpclient.getConnectionManager().shutdown();  
		}
        return responseBody;
	}
}
