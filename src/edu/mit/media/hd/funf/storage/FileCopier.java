package edu.mit.media.hd.funf.storage;

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
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;

import android.util.Log;
import edu.mit.media.hd.funf.Utils;

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
		public final static String TAG = SimpleFileCopier.class.getName();
		
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
						Log.e(TAG, "Error backing up file. " + e.getLocalizedMessage());
						return false;
					} finally {
						try {
							if (src != null) {src.close();}
							if (dst != null) {dst.close();}
						} catch (IOException e) {
							Log.e(TAG, "Error closing db files. " + e.getLocalizedMessage());
							return false;
						}
					}
				} else {
					Log.e(TAG, "File does not exist " + srcFile.getAbsolutePath());
					return false;
				}
			} catch (FileNotFoundException e) {
				Log.e(TAG, "Unable to create backup. " + e.getLocalizedMessage());
				return false;
			}
			return true;
		}
	}
	
	public static class EncryptedFileCopier implements FileCopier {
		public static final String TAG = EncryptedFileCopier.class.getName();
		private final byte[] dataKey;
		
		public EncryptedFileCopier(byte[] dataKey) {
			assert dataKey != null;
			this.dataKey = new byte[dataKey.length];
			System.arraycopy(dataKey, 0, this.dataKey, 0, dataKey.length);
		}
		
		@Override
		public boolean copy(File sourceFile, File destinationFile) {
			Log.i(TAG, "encrypting + copying " + sourceFile.getPath() + " to " + destinationFile.getPath());

			Cipher ecipher = null;
			try {
				DESKeySpec des = new DESKeySpec(dataKey);
				SecretKey key = SecretKeyFactory.getInstance("DES").generateSecret(des);
				ecipher = Cipher.getInstance("DES");     
		        ecipher.init(Cipher.ENCRYPT_MODE, key);
			} catch (Exception e) {
				Log.e(TAG, "Error creating cipher", e);
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
				Utils.close(in);
				Utils.close(co);
				Utils.close(out);
			}
			Log.i(TAG, "done copy");
			return true;
		}
		
	}
}
