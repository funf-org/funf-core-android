/**
 * 
 * Funf: Open Sensing Framework
 * Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner
 * Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation, either version 3 of
 * the License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public
 * License along with Funf. If not, see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.storage;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;

import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

/**
 * Archives a file to the url specified using POST HTTP method.
 *
 */
@DefaultSchedule(interval=21600) // 6h
public class HttpArchive implements RemoteFileArchive {
	
    @Configurable
	private String url;
    
    @Configurable
    private boolean wifiOnly = false;
    
    private Context context;
    
	@SuppressWarnings("unused")
	private String mimeType;
	
	public HttpArchive() {
	  
	}
	
	public HttpArchive(Context context, final String uploadUrl) {
		this(context, uploadUrl, "application/x-binary");
	}
	
	public HttpArchive(Context context, final String uploadUrl, final String mimeType) {
		this.url = uploadUrl;
		this.mimeType = mimeType;
		this.context = context;
	}
	
	public void setContext(Context context) {
	  this.context = context;
	}
	
	public void setUrl(String url) {
	  this.url = url;
	}
	
	public boolean isAvailable() {
	  assert context != null;
	  ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
      NetworkInfo netInfo = connectivityManager.getActiveNetworkInfo();
      if (!wifiOnly && netInfo != null && netInfo.isConnectedOrConnecting()) {
        return true;
      } else if (wifiOnly) {
        State wifiInfo = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI).getState();
        if (State.CONNECTED.equals(wifiInfo) || State.CONNECTING.equals(wifiInfo)) {
          return true;
        }
      }
      return false;
	}
	
	public String getId() {
	  return url;
	}
	
	
	
	public boolean add(File file) {
		return IOUtil.isValidUrl(url) ? uploadFile(file, url) : false;
	}
	
	/**
	 * Based on funf v3 from OpenSensing
	 * @param file
	 * @param uploadurl
	 * @return
	 */
	public static boolean uploadFile(File file,String uploadurl) {
		HttpClient httpClient = new DefaultHttpClient() ;
		HttpPost httpPost = new HttpPost(uploadurl);

		MultipartEntityBuilder builder = MultipartEntityBuilder.create();
		builder.setMode(HttpMultipartMode.BROWSER_COMPATIBLE);
		FileBody fileBody = new FileBody(file);
		builder.addPart("uploadedfile", fileBody);
		HttpEntity entity = builder.build();

		httpPost.setEntity(entity);
		HttpResponse response = null;

		try {
			response = httpClient.execute(httpPost);
		} catch (ClientProtocolException e) {
			Log.e("ClientProtocolExc: "+e, e.getMessage());
			return false;
		} catch (IOException e) {
			Log.e("IOException : "+e, e.getMessage());
			return false;
		}
		if(response == null) {
			return false;
		}
		if(response.getStatusLine().getStatusCode() == 200) {
			return true;
		}
		return false;
	}
}
