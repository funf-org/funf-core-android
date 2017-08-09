/**
 * BSD 3-Clause License
 *
 * Copyright (c) 2010-2012, MIT
 * Copyright (c) 2012-2016, Nadav Aharony, Alan Gardner, and Cody Sumter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * * Redistributions of source code must retain the above copyright notice, this
 *   list of conditions and the following disclaimer.
 *
 * * Redistributions in binary form must reproduce the above copyright notice,
 *   this list of conditions and the following disclaimer in the documentation
 *   and/or other materials provided with the distribution.
 *
 * * Neither the name of the copyright holder nor the names of its
 *   contributors may be used to endorse or promote products derived from
 *   this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
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

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.io.Closeable;
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

import android.net.Uri;
import android.util.Log;

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
  	boolean isValidUrl = false;
  	if (url != null &&  !url.trim().equals("")) {
  		try {
  			Uri test = Uri.parse(url);
  			isValidUrl = test.getScheme() != null 
  			&& test.getScheme().startsWith("http") 
  			&& test.getHost() != null 
  			&& !test.getHost().trim().equals("");
  		} catch (Exception e) {
  			Log.d(LogUtil.TAG, "Not valid", e);
  		}
  	}
  	Log.d(LogUtil.TAG, "Valid url? " + isValidUrl);
  	return isValidUrl;
  }
}
