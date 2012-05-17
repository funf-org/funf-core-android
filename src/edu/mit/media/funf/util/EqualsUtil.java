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