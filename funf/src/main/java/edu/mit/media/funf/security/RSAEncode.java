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
