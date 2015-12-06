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
/**
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
 */
package edu.mit.media.funf.util;

import android.content.Context;
import android.util.Log;
import android.util.Patterns;

import org.apache.http.params.HttpParams;

import java.io.BufferedInputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import edu.mit.media.funf.FunfManager;

public class IOUtil {
	
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

	public static String httpGet(String uri,HttpParams params,String action,String accessToken) {
		//params are deprecated

		HttpURLConnection urlConnection = null;
		int responseCode = 0;
		try {
			URL url = new URL(uri);
			urlConnection = (HttpURLConnection) url.openConnection();
			responseCode = urlConnection.getResponseCode();
			InputStream in = new BufferedInputStream(urlConnection.getInputStream());
			String returnValue = inputStreamToString(in, "UTF-8");
			return returnValue;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
			if (responseCode == 401) FunfManager.funfManager.authError(action, accessToken);
		}
		finally {
			if (urlConnection != null) {
				urlConnection.disconnect();
			}
		}
		return null;
	}

	public static String httpGet(String uri,HttpParams params){
		return httpGet(uri,params,"unknown","");
	}

	/**
	 * Closes a stream, and swallows null cases our IOExceptions.
	 * @param stream
	 */
	public static boolean close(Closeable stream) {
		if(stream != null) {
			try {
				stream.close();
				return true;
			} catch (IOException e) {
				Log.e(LogUtil.TAG, "Error closing stream", e);
			}
		}
		return false;
	}

  public static boolean isValidUrl(String url) {
  	Log.d(LogUtil.TAG, "Validating url");
  	boolean isValidUrl = Patterns.WEB_URL.matcher(url).matches();
  	Log.d(LogUtil.TAG, "Valid url? " + isValidUrl);
  	return isValidUrl;
  }

	public static String formatServerUrl(String uploadurl, String filename) {

		URI uri = null;
		String accessToken = "";
		try {
			uri = new URI(uploadurl);
			accessToken = FunfManager.context.getSharedPreferences("funf_auth", Context.MODE_PRIVATE).getString(md5(uri.getHost()), "");
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}

		String formattedUploadUrl = uploadurl;
		formattedUploadUrl = formattedUploadUrl.replace("$FILENAME$", filename);
		formattedUploadUrl = formattedUploadUrl.replace("$ACCESS_TOKEN$", accessToken);

		return formattedUploadUrl;
	}

	public static final String md5(final String s) {
		try {
			// Create MD5 Hash
			MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
			digest.update(s.getBytes());
			byte messageDigest[] = digest.digest();

			// Create Hex String
			StringBuffer hexString = new StringBuffer();
			for (int i = 0; i < messageDigest.length; i++) {
				String h = Integer.toHexString(0xFF & messageDigest[i]);
				while (h.length() < 2)
					h = "0" + h;
				hexString.append(h);
			}
			return hexString.toString();

		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
		} catch (NullPointerException e) {
			e.printStackTrace();
		}
		return "";
	}
}
