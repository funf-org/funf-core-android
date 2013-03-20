/**
 * 
 * Funf: Open Sensing Framework Copyright (C) 2010-2011 Nadav Aharony, Wei Pan, Alex Pentland.
 * Acknowledgments: Alan Gardner Contact: nadav@media.mit.edu
 * 
 * This file is part of Funf.
 * 
 * Funf is free software: you can redistribute it and/or modify it under the terms of the GNU Lesser
 * General Public License as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Funf is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License along with Funf. If not,
 * see <http://www.gnu.org/licenses/>.
 * 
 */
package edu.mit.media.funf.util;

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;

public class FileUtil {


  public static boolean isSDCardReady() {
    try {
      return Environment.getExternalStorageDirectory().exists();
    } catch (Exception e) {}
    return false;
  }

  public static void sendFile(Context context, String filepath) {
    Uri zipFileUri = Uri.parse("file://" + filepath);
    Intent sendIntent = new Intent(Intent.ACTION_SEND);
    sendIntent.setType("application/zip");
    sendIntent.putExtra(Intent.EXTRA_STREAM, zipFileUri);
    context.startActivity(Intent.createChooser(sendIntent, "Send data using"));
  }

  public static final int BUFFER_LENGTH = 2048;

  public static void zip(List<File> files, String zipFile) throws IOException {
    FileOutputStream dest = null;
    ZipOutputStream out = null;
    try {
      dest = new FileOutputStream(zipFile);
      out = new ZipOutputStream(new BufferedOutputStream(dest));

      byte data[] = new byte[BUFFER_LENGTH];

      for (File file : files) {
        Log.v(TAG, "Adding: " + file);
        FileInputStream fi = new FileInputStream(file);
        BufferedInputStream origin = new BufferedInputStream(fi, BUFFER_LENGTH);
        ZipEntry entry = new ZipEntry(file.getName());
        out.putNextEntry(entry);
        int count;
        while ((count = origin.read(data, 0, BUFFER_LENGTH)) != -1) {
          out.write(data, 0, count);
        }
        origin.close();
      }
    } catch (IOException e) {
      Log.e(TAG, e.getLocalizedMessage());
      e.printStackTrace();
      throw e;
    } finally {
      if (out != null) out.close();
      if (dest != null) dest.close();
    }
  }

  public static void copyFile(InputStream in, OutputStream out) throws IOException {
    byte[] buffer = new byte[1024];
    int read;
    while ((read = in.read(buffer)) != -1) {
      out.write(buffer, 0, read);
    }
  }

  public static void delete(File f) throws IOException {
    if (f.isDirectory()) {
      for (File c : f.listFiles())
        delete(c);
    }
    if (!f.delete()) throw new FileNotFoundException("Failed to delete file: " + f);
  }

  public static List<File> getSubdirs(File file) {
    File[] subdirFiles = file.listFiles(new FileFilter() {
      public boolean accept(File f) {
        return f.isDirectory();
      }
    });
    if (subdirFiles == null) {
      subdirFiles = new File[0];
    }
    List<File> subdirs = Arrays.asList(subdirFiles);
    subdirs = new ArrayList<File>(subdirs);

    List<File> deepSubdirs = new ArrayList<File>();
    for (File subdir : subdirs) {
      deepSubdirs.addAll(getSubdirs(subdir));
    }
    subdirs.addAll(deepSubdirs);
    return subdirs;
  }

  public static boolean writeStringToFile(File file, String content) {
    boolean fileWritten = false;
    // if (file.canWrite()) { // TODO: figure out why this returns false if file does not exist
    Writer out = null;
    try {
      out = new OutputStreamWriter(new FileOutputStream(file), Charset.defaultCharset().name());
      out.write(content);
      fileWritten = true;
    } catch (UnsupportedEncodingException e) {
      Log.e(TAG, "File not written, not a supportted encoding: " + Charset.defaultCharset().name());
    } catch (FileNotFoundException e) {
      Log.e(TAG, "File not found: " + file.getAbsolutePath());
    } catch (IOException e) {
      Log.e(TAG, "Error writing file: " + e.getLocalizedMessage());
    } finally {
      if (out != null) {
        try {
          out.close();
        } catch (IOException e) {
          Log.e(TAG, "Error closing file: " + e.getLocalizedMessage());
        }
      }
    }
    // }
    return fileWritten;
  }

  public static String getStringFromFile(File file) {
    try {
      FileInputStream stream = new FileInputStream(file);
      try {
        FileChannel channel = stream.getChannel();
        MappedByteBuffer bb = channel.map(FileChannel.MapMode.READ_ONLY, 0, channel.size());
        return Charset.defaultCharset().decode(bb).toString();
      } finally {
        stream.close();
      }
    } catch (FileNotFoundException e) {
      Log.e(TAG, "File not found: " + file.getAbsolutePath());
    } catch (IOException e) {
      Log.e(TAG, "Error reading file: " + file.getAbsolutePath());
    }
    return null;
  }

  public static String getStringFromFileWithLimit(File file, long byteLimit) {
    // TODO: could optimize length count by getting a url and using .openStream().available()
    if (file.length() > byteLimit) {
      throw new IllegalArgumentException("File too large.");
    }
    return FileUtil.getStringFromFile(file);
  }

  public static String getSdCardPath(Context context) {
    return new File(Environment.getExternalStorageDirectory(), context.getPackageName()) + "/";
  }

}
