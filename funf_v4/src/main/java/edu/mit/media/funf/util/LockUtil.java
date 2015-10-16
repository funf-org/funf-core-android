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

import android.content.Context;
import android.os.PowerManager;

public class LockUtil {

	public static PowerManager.WakeLock getWakeLock(Context context) {
		return LockUtil.getWakeLock(context, context.getClass().getName());
	}

	public static PowerManager.WakeLock getWakeLock(Context context, String tag) {
		PowerManager pm = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
		PowerManager.WakeLock lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, tag);
		lock.acquire();
		return lock;
	}

}
