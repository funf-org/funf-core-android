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
// using /res/raw/pub.enc as the RSA Public Key
package edu.mit.media.funf.security;

import java.util.ArrayList;

import javax.crypto.Cipher;

import android.content.Context;



public class RSAEncode {
	
	private static Cipher cipher = null;
	
	/*
	 * // Currently unused
	 * 
	private static void genCipher(Context context, int publicKey) {
		Log.i(LogUtil.TAG, "generate new cipher");
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
			Log.e(LogUtil.TAG, "fatal! cannot initialize RSA cipher");
			e.printStackTrace();
		}
	}
	*/
	
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
		
		synchronized (cipher) {
			
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

		}
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
