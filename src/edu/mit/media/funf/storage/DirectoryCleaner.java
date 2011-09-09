package edu.mit.media.funf.storage;

import java.io.File;
import java.util.Arrays;
import java.util.Comparator;

import android.os.StatFs;

/**
 * Cleans unneeded files from directories
 */
public interface DirectoryCleaner {

	public void clean(File directory);
	
	
	public static class KeepAll implements DirectoryCleaner {
		@Override
		public void clean(File directory) {}
	}
	
	/**
	 * Keep only the most recent specified amount of files
	 *
	 */
	public static class KeepMostRecent implements DirectoryCleaner {
		private final int numToKeep;
		public KeepMostRecent(int numToKeep) {
			this.numToKeep = numToKeep;
		}
		
		@Override
		public void clean(File directory) {
			if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (files != null && files.length > numToKeep) {
					// Sort descending by last modified time
					Arrays.sort(files, new DescendingByLastModifiedComaparator());
					// Delete all except the first numToKeep files
					for (int i=numToKeep; i<files.length; i++) {
						files[i].delete();
					}
				}
			}
		}
	}
	
	/**
	 * Only Keep files for a certain length of time
	 *
	 */
	public static class KeepForLengthOfTime implements DirectoryCleaner {
		private final long millisToKeep;
		public KeepForLengthOfTime(long millisToKeep) {
			this.millisToKeep = millisToKeep;
		}
		
		@Override
		public void clean(File directory) {
			if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				if (files != null) {
					long now = System.currentTimeMillis();
					for (File file : files) {
						if ((now - file.lastModified()) > millisToKeep) {
							file.delete();
						}
					}
				}
			}
		}
	}
	
	/**
	 * Keeps the total size of all files in the directory under the number of bytes specified
	 */
	public static class KeepUnderStorageLimit implements DirectoryCleaner {
		private final long maxBytesToKeep;
		public KeepUnderStorageLimit(long maxBytesToKeep) {
			this.maxBytesToKeep = maxBytesToKeep;
		}
		
		@Override
		public void clean(File directory) {
			if (directory.isDirectory()) {
				File[] files = directory.listFiles();
				long bytesToDelete = size(directory) - maxBytesToKeep;
				if (bytesToDelete > 0) {
					Arrays.sort(files, new DescendingByLastModifiedComaparator());
					for (int i=files.length; i>=0; i--) {
						if (bytesToDelete > 0) {
							bytesToDelete -= size(files[i]);
							files[i].delete();
						}
					}
				}
			}
		}
		
		/**
		 * Calculate the size of the file or directory recursively.
		 * @param file
		 */
		private long size(File file) {
			if (file.isDirectory()) {
				long bytes = 0;
				for (File childFile : file.listFiles()) {
					bytes += size(childFile);
				}
				return bytes;
			} else {
				return file.length();
			}
		}
	}
	

	/**
	 * Cleans files to use only a certain percentage of the disk that is free
	 */
	public static class KeepUnderPercentageOfDiskFree implements DirectoryCleaner {

		private final double percentageOfDiskFree;
		private final long minBytesToKeep;
		
		public KeepUnderPercentageOfDiskFree(double percentageOfDiskFree, long minBytesToKeep) {
			assert percentageOfDiskFree > 0 && percentageOfDiskFree <= 1;
			this.percentageOfDiskFree = percentageOfDiskFree;
			this.minBytesToKeep = minBytesToKeep;
		}
		
		@Override
		public void clean(File directory) {
			StatFs stat = new StatFs(directory.getAbsolutePath());
			long bytesAvailable = (long)stat.getAvailableBlocks() * (long)stat.getBlockSize();
			long bytesToKeep = Math.max((long)(bytesAvailable * percentageOfDiskFree), minBytesToKeep);
			new KeepUnderStorageLimit(bytesToKeep).clean(directory);
		}
		
	}
	
	/**
	 * Applies all of the cleaning strategies to the directory
	 */
	public static class CompositeDirectoryCleaner implements DirectoryCleaner {

		private final DirectoryCleaner[] cleaners;
		
		public CompositeDirectoryCleaner(DirectoryCleaner... cleaners) {
			this.cleaners = cleaners;
		}
		
		@Override
		public void clean(File directory) {
			for(DirectoryCleaner cleaner : cleaners) {
				cleaner.clean(directory);
			}
		}
		
	}
	
	
	static class DescendingByLastModifiedComaparator implements Comparator<File> {
		@Override
		public int compare(File file0, File file1) {
			return Long.valueOf(file1.lastModified()).compareTo(
					file0.lastModified());
		}
	}
}
