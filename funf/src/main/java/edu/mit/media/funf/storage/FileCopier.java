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
package edu.mit.media.funf.storage;



import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;
import javax.crypto.SecretKey;

import android.util.Log;
import edu.mit.media.funf.util.IOUtil;
import edu.mit.media.funf.util.LogUtil;

public interface FileCopier {

	
	/**
	 * Copy file from source to destination.  FileCopier may modify content when copying or have other side effects.
	 * 
	 * @param sourceFile
	 * @param destinationFile
	 * @return
	 */
	public boolean copy(File sourceFile, File destinationFile);
	
	
	
	/**
	 * Basic FileCopier
	 *
	 */
	public static class SimpleFileCopier implements FileCopier {
		
		@Override
		public boolean copy(File srcFile, File dstFile) {
			try {
				if (srcFile.exists()) {
					dstFile.getParentFile().mkdirs();
					FileChannel src = new FileInputStream(srcFile).getChannel();
					FileChannel dst = new FileOutputStream(dstFile).getChannel();
					try {
						dst.transferFrom(src, 0, src.size());
					} catch (IOException e) {
						Log.e(LogUtil.TAG, "Error backing up file. " + e.getLocalizedMessage());
						return false;
					} finally {
						try {
							if (src != null) {src.close();}
							if (dst != null) {dst.close();}
						} catch (IOException e) {
							Log.e(LogUtil.TAG, "Error closing db files. " + e.getLocalizedMessage());
							return false;
						}
					}
				} else {
					Log.e(LogUtil.TAG, "File does not exist " + srcFile.getAbsolutePath());
					return false;
				}
			} catch (FileNotFoundException e) {
				Log.e(LogUtil.TAG, "Unable to create backup. " + e.getLocalizedMessage());
				return false;
			}
			return true;
		}
	}
	
	public static class EncryptedFileCopier implements FileCopier {
		public static final String TAG = EncryptedFileCopier.class.getName();
		private final SecretKey key;
		private final String transformation;
		
		public EncryptedFileCopier(SecretKey key, String transformation) {
			this.key = key;
			this.transformation = transformation;
		}
		
		private Cipher cipher; // Cache
		protected Cipher getCipher() {
			if (cipher == null) {
				synchronized (this) {
					if (cipher == null) {
						try {
							cipher = Cipher.getInstance(transformation);     
							cipher.init(Cipher.ENCRYPT_MODE, key);
						} catch (Exception e) {
							Log.e(TAG, "Error creating cipher", e);
						}
					}
				}
			}
			return cipher;
		}
		
		@Override
		public boolean copy(File sourceFile, File destinationFile) {
			Log.i(TAG, "encrypting + copying " + sourceFile.getPath() + " to " + destinationFile.getPath());

			Cipher ecipher = getCipher();
			if (ecipher == null) {
				return false;
			}
	        
			InputStream in = null;
			OutputStream out = null;
			CipherOutputStream co = null;
			try {
				in = new FileInputStream(sourceFile);
				out = new FileOutputStream(destinationFile); 
				co = new CipherOutputStream(out, ecipher);
				byte[] buf = new byte[128*4096]; 
				int len = 0; 
				while ((len = in.read(buf)) > 0) 
				{ 
					co.write(buf, 0, len); 
				} 
			} catch (FileNotFoundException e) {
				Log.e(TAG, "File not found", e);
				return false;
			} catch (IOException e) {
				Log.e(TAG, "IOException", e);
				return false;
			} finally {
				IOUtil.close(in);
				IOUtil.close(co);
				IOUtil.close(out);
			}
			Log.i(TAG, "done copy");
			return true;
		}
		
	}
}
