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
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.util.Base64;
import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.entity.mime.HttpMultipartMode;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.FileBody;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.impl.execchain.MainClientExec;

import edu.mit.media.funf.FunfManager;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.pipeline.BasicPipeline;
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
		String currentUrl = IOUtil.formatServerUrl(url, file.getName());
		Log.i(LogUtil.TAG, "UPLOADER: "+currentUrl + " "+IOUtil.isValidUrl(currentUrl));
		return IOUtil.isValidUrl(currentUrl) ? uploadFile(file, currentUrl,FunfManager.funfManager.getAuthToken(url)) : false;
	}
	
	/**
	 * Based on funf v3 from OpenSensing
	 * @param file
	 * @param uploadurl
	 * @return
	 */
	public static boolean uploadFile(File file,String uploadurl,String accessToken) {
		if (uploadurl == null) return false;
		if (uploadurl.equals("")) return false;
		HttpClient httpClient = new DefaultHttpClient() ;
		HttpPost httpPost;
		try {
			httpPost = new HttpPost(new URI(uploadurl));
		} catch (URISyntaxException e) {
			e.printStackTrace();
			return false;
		}

		InputStreamEntity reqEntity = null;
		HttpResponse response = null;
		try {
			reqEntity = new InputStreamEntity(new FileInputStream(file), -1);
			reqEntity.setContentType("binary/octet-stream");
			reqEntity.setChunked(true); // Send in multiple parts if needed
			httpPost.setEntity(reqEntity);
			response = httpClient.execute(httpPost);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			return false;
		} catch (IOException e) {
			e.printStackTrace();
			return false;
		}
		if (response == null) {
			return false;
		}
		if (response.getStatusLine().getStatusCode() == 200) {
			return true;
		}
		if (response.getStatusLine().getStatusCode() == 401) {
			//Auth error
			Log.i(LogUtil.TAG, "Auth Error "+response.getStatusLine().getStatusCode());
			FunfManager.funfManager.authError(BasicPipeline.ACTION_UPLOAD, accessToken);
		}


		return false;
	}
}
