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
