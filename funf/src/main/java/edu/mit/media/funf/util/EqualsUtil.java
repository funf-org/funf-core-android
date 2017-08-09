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
package edu.mit.media.funf.util;

import java.util.List;
import java.util.Set;

import android.os.Bundle;


/**
* Collected methods which allow easy implementation of <code>equals</code>.
*
* Example use case in a class called Car:
* <pre>
public boolean equals(Object aThat){
  if ( this == aThat ) return true;
  if ( !(aThat instanceof Car) ) return false;
  Car that = (Car)aThat;
  return
    EqualsUtil.areEqual(this.fName, that.fName) &&
    EqualsUtil.areEqual(this.fNumDoors, that.fNumDoors) &&
    EqualsUtil.areEqual(this.fGasMileage, that.fGasMileage) &&
    EqualsUtil.areEqual(this.fColor, that.fColor) &&
    Arrays.equals(this.fMaintenanceChecks, that.fMaintenanceChecks); //array!
}
* </pre>
*
* <em>Arrays are not handled by this class</em>.
* This is because the <code>Arrays.equals</code> methods should be used for
* array fields.
*/
public final class EqualsUtil {

  static public boolean areEqual(boolean aThis, boolean aThat){
    //System.out.println("boolean");
    return aThis == aThat;
  }

  static public boolean areEqual(char aThis, char aThat){
    //System.out.println("char");
    return aThis == aThat;
  }

  static public boolean areEqual(long aThis, long aThat){
    /*
    * Implementation Note
    * Note that byte, short, and int are handled by this method, through
    * implicit conversion.
    */
    //System.out.println("long");
    return aThis == aThat;
  }

  static public boolean areEqual(float aThis, float aThat){
    //System.out.println("float");
    return Float.floatToIntBits(aThis) == Float.floatToIntBits(aThat);
  }

  static public boolean areEqual(double aThis, double aThat){
    //System.out.println("double");
    return Double.doubleToLongBits(aThis) == Double.doubleToLongBits(aThat);
  }

  /**
  * Possibly-null object field.
  *
  * Includes type-safe enumerations and collections, but does not include
  * arrays. See class comment.
  */
  static public boolean areEqual(Object aThis, Object aThat){
    //System.out.println("Object");
    return aThis == null ? aThat == null : aThis.equals(aThat);
  }
  

	/**
	 * This is a convenience method to test whether or not two bundles' contents are equal.
	 * This method only works if the contents of each bundle are primitive data types or strings.
	 * Otherwise this method may wrongly report they they are not equal.
	 * @param bundle1
	 * @param bundle2
	 * @return
	 */
	public static boolean areEqual(Bundle bundle1, Bundle bundle2) {
		// Null for none or both, or the same
		if (bundle1 == null) {
			return bundle2 == null;
		} else if (bundle2 == null) {
			return false;
		}else if (bundle1 == bundle2) {
			return true;
		}
		
		// Same key set
		Set<String> keySet = bundle1.keySet();
		if (!keySet.equals(bundle2.keySet())) {
			return false;
		}
		
		// Same values in key set
		for (String key : keySet) {
			Object value1 = bundle1.get(key);
			Object value2 = bundle2.get(key);
			if (!(value1 == null ? value2 == null : value1.equals(value2))) {
				return false;
			}
		}
		return true;
	}
	
	/**
	 * This is a convenience method to test whether or not two bundles' contents are equal.
	 * This method only works if the contents of each bundle are primitive data types or strings.
	 * Otherwise this method may wrongly report they they are not equal.
	 * @param bundle1
	 * @param bundle2
	 * @return
	 */
	public static boolean areEqual(Bundle[] bundles1, Bundle[] bundles2) {
		// Null for none or both, or the same
		if (bundles1 == null) {
			return bundles2 == null;
		} else if (bundles2 == null) {
			return false;
		} else if (bundles1 == bundles2) {
			return true;
		}
		
		// Same length
		if (bundles1.length != bundles2.length) {
			return false;
		}
		
		// Same contents
		for (int i=0; i<bundles1.length; i++) {
			if(!areEqual(bundles1[i], bundles2[i])) {
				return false;
			}
		}
		
		return true;
	}
	
	/**
	 * This is a convenience method to test whether or not two bundles' contents are equal.
	 * This method only works if the contents of each bundle are primitive data types or strings.
	 * Otherwise this method may wrongly report they they are not equal.
	 * @param bundle1
	 * @param bundle2
	 * @return
	 */
	public static boolean areEqual(List<Bundle> bundles1, List<Bundle> bundles2) {
		// Null for none or both, or the same
		if (bundles1 == null) {
			return bundles2 == null;
		} else if (bundles2 == null) {
			return false;
		} else if (bundles1 == bundles2) {
			return true;
		}
		
		// Same length
		if (bundles1.size() != bundles2.size()) {
			return false;
		}
		
		// Same contents
		for (int i=0; i<bundles1.size(); i++) {
			if(!areEqual(bundles1.get(i), bundles2.get(i))) {
				return false;
			}
		}
		
		return true;
		
	}
}