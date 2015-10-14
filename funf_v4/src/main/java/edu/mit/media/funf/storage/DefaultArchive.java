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
import java.security.GeneralSecurityException;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;

import android.content.Context;
import edu.mit.media.funf.Schedule.DefaultSchedule;
import edu.mit.media.funf.config.Configurable;
import edu.mit.media.funf.security.Base64Coder;
import edu.mit.media.funf.util.FileUtil;
import edu.mit.media.funf.util.NameGenerator;
import edu.mit.media.funf.util.NameGenerator.CompositeNameGenerator;
import edu.mit.media.funf.util.NameGenerator.RequiredSuffixNameGenerator;
import edu.mit.media.funf.util.NameGenerator.SystemUniqueTimestampNameGenerator;
import edu.mit.media.funf.util.StringUtil;

/**
 * A default implementation of a file archive, which should be good enough for most cases.
 *
 * This archive provides internal memory and SD card redundancy, managed backups, as well as file encryption.
 * Archives are singletons by database name.
 */
@DefaultSchedule(interval=3600)
public class DefaultArchive implements FileArchive {

	private static final String DES_ENCRYPTION = "DES";
	
	private final static byte[] SALT = {
        (byte)0xa6, (byte)0xab, (byte)0x09, (byte)0x93,
        (byte)0xf4, (byte)0xcc, (byte)0xee, (byte)0x10
    };
	private final static int ITERATION_COUNT = 135; // # of times password is hashed
	
	@Configurable
	protected String name = "default";
	
	@Configurable
	protected String password;
	
	@Configurable
	protected String key;
	
    protected Context context;
	
	public DefaultArchive() {
	}
	
	public DefaultArchive(Context ctx, String name) {
	  this.context = ctx;
	  this.name = name;
	}
	
	public void setContext(Context context) {
	  this.context = context;
	}
	
	public void setName(String name) {
	  this.name = name;
	}
	
	/**
	 * Set the encryption key using a password.  
	 * Does not store the password, but instead uses it to derive a DES key to encrypt files.
	 * @param encryptionPassword
	 */
	public void setEncryptionPassword(char[] encryptionPassword) { // Uses char[] instead of String to prevent caching
	  if (encryptionPassword == null || encryptionPassword.length == 0) {
	    setEncryptionKey(null);
	  } else {
		PBEKeySpec keySpec = new PBEKeySpec(encryptionPassword, SALT, ITERATION_COUNT);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey secretKey = factory.generateSecret(keySpec);
			setEncryptionKey(secretKey.getEncoded());
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to encrypt data files.", e);
		} 
	  }
	}
	
	public void setEncryptionKey(byte[] encryptionKey) {
	  if (encryptionKey == null || encryptionKey.length == 0) {
        saveKey(null);
      } else {
		try {
			DESKeySpec des = new DESKeySpec(encryptionKey);
			SecretKey key = SecretKeyFactory.getInstance(DES_ENCRYPTION).generateSecret(des);
			saveKey(key);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to build key for encryption", e);
		} 
      }
	}
	
	
	private SecretKey keyCache = null;
	public SecretKey getSecretKey() {
	  if (keyCache == null) {
	    if (key != null) {
	      setEncryptionKey(Base64Coder.decode(key.toCharArray()));
	    } else if (password != null) {
	      setEncryptionPassword(password.toCharArray());
	    } 
	  }
	  return keyCache; 
	}
	
    private void saveKey(SecretKey secretKey) {
      keyCache = secretKey;
      // Reset delegate archive, to reinitialize key
      delegateArchive = null;
      getDelegateArchive();
    }

	/////////////////////
	// Delegate
    
    
    private String getCleanedName() {
      return StringUtil.simpleFilesafe(name);
    }
	
	public String getPathOnSDCard() {
		return FileUtil.getSdCardPath(context) + getCleanedName() + "/";
	}
	
	private FileArchive delegateArchive; // Cache
	protected FileArchive getDelegateArchive() {
		if (delegateArchive == null) {
			synchronized (this) {
				if (delegateArchive == null) {
					SecretKey key = getSecretKey();
					String rootSdCardPath = getPathOnSDCard();
					FileArchive backupArchive = FileDirectoryArchive.getRollingFileArchive(new File(rootSdCardPath + "backup"));
					FileArchive mainArchive = new CompositeFileArchive(
							getTimestampedDbFileArchive(new File(rootSdCardPath + "archive"), context, key),
							getTimestampedDbFileArchive(context.getDir("funf_" + getCleanedName() + "_archive", Context.MODE_PRIVATE), context, key)
							);
					delegateArchive = new BackedUpArchive(mainArchive, backupArchive);
				}
			}
		}
		return delegateArchive;
	}
	
	static FileDirectoryArchive getTimestampedDbFileArchive(File archiveDir, Context context, SecretKey encryptionKey) {
		NameGenerator nameGenerator = new CompositeNameGenerator(new SystemUniqueTimestampNameGenerator(context), new RequiredSuffixNameGenerator(".db"));
		FileCopier copier = (encryptionKey == null) ? new FileCopier.SimpleFileCopier() : new FileCopier.EncryptedFileCopier(encryptionKey, DES_ENCRYPTION);
		return new FileDirectoryArchive(archiveDir, nameGenerator, copier, new DirectoryCleaner.KeepAll());
	}
	
	@Override
	public boolean add(File item) {
		return getDelegateArchive().add(item);
	}

	@Override
	public boolean contains(File item) {
		return getDelegateArchive().contains(item);
	}

	@Override
	public File[] getAll() {
		return getDelegateArchive().getAll();
	}

	@Override
	public boolean remove(File item) {
		return getDelegateArchive().remove(item);
	}
	
	
}
