package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;

import android.util.Log;

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

		@Override
		public boolean copy(File sourceFile, File destinationFile) {
			throw new UnsupportedOperationException("Not yet implemented");
		}
		
	}
	
	
	/**
	 * Copys the the srcFile to the dstFile, overwriting and making directories as necessary.
	 * @param srcFile
	 * @param dstFile
	 * @return
	 */
	/*
	private static boolean copy(File srcFile, File dstFile) {
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
	*/
}
