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
