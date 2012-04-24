/**
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
 */
package edu.mit.media.funf.storage;

import java.io.File;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.DESKeySpec;
import javax.crypto.spec.PBEKeySpec;

import android.content.Context;
import android.content.SharedPreferences;
import edu.mit.media.funf.Base64Coder;
import edu.mit.media.funf.Utils;
import edu.mit.media.funf.storage.NameGenerator.CompositeNameGenerator;
import edu.mit.media.funf.storage.NameGenerator.RequiredSuffixNameGenerator;
import edu.mit.media.funf.storage.NameGenerator.SystemUniqueTimestampNameGenerator;
import static edu.mit.media.funf.AsyncSharedPrefs.async;

/**
 * A default implementation of a file archive, which should be good enough for most cases.
 *
 * This archive provides internal memory and SD card redundancy, managed backups, as well as file encryption.
 * Archives are singletons by database name.
 */
public class DefaultArchive implements Archive<File> {

	private static final String ENCRYPTION_PREFS = "edu.mit.media.funf.configured.ConfiguredEncryption";
	private static final String ENCRYPTION_KEY = "ENCRYPTION_KEY";
	private static final String DES_ENCRYPTION = "DES";
	
	private static final char[] DEFAULT_PASSWORD = "changeme".toCharArray();
	
	private final static byte[] SALT = {
        (byte)0xa6, (byte)0xab, (byte)0x09, (byte)0x93,
        (byte)0xf4, (byte)0xcc, (byte)0xee, (byte)0x10
    };
	private final static int ITERATION_COUNT = 135; // # of times password is hashed
	
	protected final String databaseName;
	protected final Context context;
	protected final SharedPreferences preferences;
	
	
	private DefaultArchive(Context context, String databaseName) {
		this.context = context.getApplicationContext();
		this.databaseName = databaseName;
		this.preferences = async(this.context.getSharedPreferences(ENCRYPTION_PREFS, Context.MODE_PRIVATE));
	}
	
	private static final Map<String,DefaultArchive> instances = new HashMap<String, DefaultArchive>();
	/**
	 * Get an instance of the default archive for the specified database name
	 * @param context
	 * @param databaseName
	 * @return
	 */
	public synchronized static DefaultArchive getArchive(Context context, String databaseName) {
		DefaultArchive archive = instances.get(databaseName);
		if (archive == null) {
			archive = new DefaultArchive(context, databaseName);
			instances.put(databaseName, archive);
		}
		return archive;
	}
	
	/**
	 * Set the encryption key using a password.  
	 * Does not store the password, but instead uses it to derive a DES key to encrypt files.
	 * @param encryptionPassword
	 */
	public void setEncryptionPassword(char[] encryptionPassword) { // Uses char[] instead of String to prevent caching
		PBEKeySpec keySpec = new PBEKeySpec(encryptionPassword, SALT, ITERATION_COUNT);
		try {
			SecretKeyFactory factory = SecretKeyFactory.getInstance("PBEWithMD5AndDES");
			SecretKey secretKey = factory.generateSecret(keySpec);
			saveKey(secretKey);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to encrypt data files.", e);
		} 
	}
	
	public void setEncryptionKey(byte[] encryptionKey) {
		try {
			DESKeySpec des = new DESKeySpec(encryptionKey);
			SecretKey key = SecretKeyFactory.getInstance(DES_ENCRYPTION).generateSecret(des);
			saveKey(key);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to build key for encryption", e);
		} 
	}
	
	protected SecretKey getKey() {
		String encodedDesKey = preferences.getString(ENCRYPTION_KEY, null);
		if (encodedDesKey == null) { // Use default password if key set
			setEncryptionPassword(DEFAULT_PASSWORD); 
			encodedDesKey = preferences.getString(ENCRYPTION_KEY, null);
		}
		assert encodedDesKey != null;
		try {
			DESKeySpec des = new DESKeySpec(Base64Coder.decode(encodedDesKey.toCharArray()));
			return SecretKeyFactory.getInstance(DES_ENCRYPTION).generateSecret(des);
		} catch (GeneralSecurityException e) {
			throw new RuntimeException("Unable to build key for encryption", e);
		} 
	}
	
	private void saveKey(SecretKey secretKey) {
		SharedPreferences.Editor edit = preferences.edit();
		edit.putString(ENCRYPTION_KEY, new String(Base64Coder.encode(secretKey.getEncoded())));
		edit.commit();
		// Reset delegate archive, to reinitialize key
		delegateArchive = null;
		getDelegateArchive();
	}

	/////////////////////
	// Delegate
	
	public String getPathOnSDCard() {
		return Utils.getSdCardPath(context) + databaseName + "/";
	}
	
	private Archive<File> delegateArchive; // Cache
	@SuppressWarnings("unchecked")
	protected Archive<File> getDelegateArchive() {
		if (delegateArchive == null) {
			synchronized (this) {
				if (delegateArchive == null) {
					SecretKey key = getKey();
					String rootSdCardPath = getPathOnSDCard();
					Archive<File> backupArchive = FileDirectoryArchive.getRollingFileArchive(new File(rootSdCardPath + "backup"));
					Archive<File> mainArchive = new CompositeFileArchive(
							getTimestampedDbFileArchive(new File(rootSdCardPath + "archive"), context, key),
							getTimestampedDbFileArchive(context.getDir("funf_" + databaseName + "_archive", Context.MODE_PRIVATE), context, key)
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
