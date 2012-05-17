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
