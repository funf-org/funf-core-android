package edu.mit.media.hd.funf.storage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;

import javax.crypto.Cipher;
import javax.crypto.CipherOutputStream;

import android.util.Log;

/**
 * Encryption strategy for archiving files to the SDCard so they can't be read by other applications
 * NOTE: Currently In progress 
 */
// TODO: finish the encrypted file copier and remove this class
public class EncryptedBackup {

	public static final String TAG = EncryptedBackup.class.getName();
	
	public static void archive(String sourcePath, String destinationPath) {
		Cipher c = null;
		try {
			c = Cipher.getInstance("RSA/None/PKCS1Padding");
			c.init(Cipher.ENCRYPT_MODE, getPublicKey());
			File dbFile = new File(sourcePath);
			File backupDbFile = new File(destinationPath);
			encrypt(c, dbFile, backupDbFile);
			dbFile.delete();
		} catch (Exception e) {
			Log.e(TAG, e.getLocalizedMessage());
			e.printStackTrace();
			return;
		} 

		// Backup and encrypt
		// Clear database
	}
	
	public static void encrypt(Cipher cipher, File in, File out) throws IOException, InvalidKeyException {
	    FileInputStream is = new FileInputStream(in);
	    CipherOutputStream os = new CipherOutputStream(new FileOutputStream(out), cipher);
	    copy(is, os);
	    os.close();
	  }
	
	  private static void copy(InputStream is, OutputStream os) throws IOException {
		  int i;
		  byte[] b = new byte[1024];
		  while((i=is.read(b))!=-1) {
			  os.write(b, 0, i);
		  }
	  }

	
	private static RSAPublicKey getPublicKey() throws NoSuchAlgorithmException, InvalidKeySpecException {
		KeyFactory keyFactory = KeyFactory.getInstance("RSA");
	    RSAPublicKeySpec pubKeySpec = new RSAPublicKeySpec(new BigInteger(
	        "12345678", 16), new BigInteger("11", 16));
	    //RSAPrivateKeySpec privKeySpec = new RSAPrivateKeySpec(new BigInteger(
	    //    "12345678", 16), new BigInteger("12345678",
	    //    16));

	    RSAPublicKey pubKey = (RSAPublicKey) keyFactory.generatePublic(pubKeySpec);
	    //RSAPrivateKey privKey = (RSAPrivateKey) keyFactory.generatePrivate(privKeySpec);
	    return pubKey;
	}
	
}
