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
package edu.mit.media.funf.probe.builtin;

import java.util.HashMap;
import java.util.Map;

import android.net.Uri;
import android.provider.MediaStore.Images;
import edu.mit.media.funf.probe.Probe.DisplayName;
import edu.mit.media.funf.time.DecimalTimeUnit;

@DisplayName("Image File Stats Probe")
public class ImageMediaProbe extends DatedContentProviderProbe {

	@Override
	protected Uri getContentProviderUri() {
		return Images.Media.EXTERNAL_CONTENT_URI;
	}

	@Override
	protected String getDateColumnName() {
		return  Images.Media.DATE_MODIFIED;
	}

	@Override
	protected Map<String, CursorCell<?>> getProjectionMap() {
		Map<String, CursorCell<?>> projectionMap = new HashMap<String, CursorCell<?>>();
		projectionMap.put(Images.Media._ID, stringCell());
		// Ignoring DATA, too large and not relevant
		projectionMap.put(Images.Media.DATE_ADDED, longCell());
		projectionMap.put(Images.Media.DATE_MODIFIED, longCell());
		projectionMap.put(Images.Media.DISPLAY_NAME, sensitiveStringCell());
		projectionMap.put(Images.Media.MIME_TYPE, stringCell());
		projectionMap.put(Images.Media.SIZE, longCell());
		projectionMap.put(Images.Media.TITLE, sensitiveStringCell());
		projectionMap.put(Images.Media.BUCKET_DISPLAY_NAME, sensitiveStringCell());
		projectionMap.put(Images.Media.BUCKET_ID, stringCell());
		projectionMap.put(Images.Media.DATE_TAKEN, longCell());
		projectionMap.put(Images.Media.DESCRIPTION, sensitiveStringCell());
		projectionMap.put(Images.Media.IS_PRIVATE, intCell());
		projectionMap.put(Images.Media.LATITUDE, doubleCell());
		projectionMap.put(Images.Media.LONGITUDE, doubleCell());
		projectionMap.put(Images.Media.MINI_THUMB_MAGIC, intCell());
		projectionMap.put(Images.Media.ORIENTATION, intCell());
		projectionMap.put(Images.Media.PICASA_ID, stringCell());
		return projectionMap;
	}
	
	@Override
	protected DecimalTimeUnit getDateColumnTimeUnit() {
		return DecimalTimeUnit.SECONDS;
	}

}
