// using /res/raw/pub.enc as the RSA Public Key
package edu.mit.media.hd.funf;
/**
 * 
 * This file is part of the FunF Software System
 * Copyright � 2010, Massachusetts Institute of Technology  
 * Do not distribute or use without permission.
 * Contact: Nadav Aharony (nadav@mit.edu) or friendsandfamily@media.mit.edu
 * 
 * @date Feb, 2010
 * 
 */

import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.ArrayList;

import javax.crypto.Cipher;

import android.content.Context;
import android.util.Log;



public class RSAEncode {
	private static final String TAG = "funf_RSA";
	private static Cipher cipher = null;
	private static void genCipher(Context context, int publicKey) {
		Log.i(TAG, "generate new cipher");
		byte[] data = null;
		X509EncodedKeySpec key = new X509EncodedKeySpec(data);
		KeyFactory kee = null;
		PublicKey key1 = null;
		try {
			kee = KeyFactory.getInstance("RSA", "BC");
			key1 = kee.generatePublic(key);
			cipher = Cipher.getInstance("RSA", "BC");
			cipher.init(Cipher.ENCRYPT_MODE, key1);
		} catch (Exception e) {
			Log.e(TAG, "fatal! cannot initialize RSA cipher");
			e.printStackTrace();
		}
	}
	
	public static String encodeStringRSA(Context context, String txt) {
		if (cipher == null)
			return null;
		if (txt == null)
			return null;
		byte[] result = null;
		ArrayList<byte[]> chunkList = new ArrayList<byte[]>(); 	
		int blocksize = cipher.getBlockSize();
		byte[] txtbytes = txt.getBytes(); 		
		int remainder_len = txtbytes.length;
		
		int count = 0;				
		while (remainder_len > 0){	     
	         // Now, encrypt them and write them to the encrypted file...
			byte[] clearchunk = new byte[blocksize];
			System.arraycopy(txtbytes, blocksize*count, clearchunk, 0, Math.min(blocksize, remainder_len));
			count +=1; 
			try{
				byte[] encryptedBytes = cipher.doFinal(clearchunk);				
				//byte[] encryptedBytes = cipher.update(txtbytes,(txtbytes.length-remainder_len), 10);// blocksize);
				chunkList.add(encryptedBytes);
				// debugging:				
	        	
			}catch (Exception e) {
				System.err.println("cannot encrypt text");
				//  Log.e(TAG, "cannot encrypt text");
	            e.printStackTrace();
	            return null;
			}
			remainder_len = remainder_len - Math.min(blocksize, remainder_len);							
		} // while
		
		result = arrayMerge(chunkList);

		return new String(Base64Coder.encode(result));
	}
	
	// generic function for merging arrays
	public static byte[] arrayMerge(ArrayList<byte[]>chunkList)
	{
	    // Determine required size of new array
	    int count = 0;
	    for (byte[] chunk : chunkList){
	    	count += chunk.length;
	    }
	    
	    // create new array of required class
	    byte[] mergedArray = new byte[count];
	    // Merge each array into new array

	    int offset = 0;
	    for (byte[] chunk : chunkList){
	        System.arraycopy(chunk, 0,
	           mergedArray, offset, chunk.length);
	        offset += chunk.length;
	    }
	    return mergedArray;
	} 
}
