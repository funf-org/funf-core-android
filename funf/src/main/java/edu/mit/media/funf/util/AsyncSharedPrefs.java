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

import static edu.mit.media.funf.util.LogUtil.TAG;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.WeakHashMap;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.util.Log;

/**
 * A convenience class to make sure that writes happen in a separate thread,
 * but data is changed in the SharedPreferences object immediately.
 * 
 * Listeners still receive notifications when disk storage is updated.
 * 
 * @author alangardner
 *
 */
public class AsyncSharedPrefs implements SharedPreferences, OnSharedPreferenceChangeListener {

	private static final Object mContent = new Object();
	
	private final Map<String,Object> mMap;
	private final SharedPreferences prefs;
    private final WeakHashMap<OnSharedPreferenceChangeListener, Object> mListeners;

	private AsyncSharedPrefs(SharedPreferences prefs) {
		this.prefs = prefs;
		this.mMap = new HashMap<String, Object>();
		this.mListeners = new WeakHashMap<OnSharedPreferenceChangeListener, Object>();
		prefs.registerOnSharedPreferenceChangeListener(this);
		this.mMap.putAll(prefs.getAll());
	}
	
	private static final Map<SharedPreferences, AsyncSharedPrefs> instances = new HashMap<SharedPreferences, AsyncSharedPrefs>();
	public static AsyncSharedPrefs async(SharedPreferences prefs) {
		AsyncSharedPrefs asyncPrefs = instances.get(prefs);
		if (asyncPrefs == null) {
			synchronized (instances) {
				// Check one more time when we are synchronized
				asyncPrefs = instances.get(prefs);
				if (asyncPrefs == null) {
					asyncPrefs = new AsyncSharedPrefs(prefs);
					instances.put(prefs, asyncPrefs);
				}
			}
		}
		return asyncPrefs;
	}
	
	@Override
	public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
		Set<OnSharedPreferenceChangeListener> listeners = new HashSet<OnSharedPreferenceChangeListener>();
		synchronized (this) {
			if (prefs.contains(key)) {
				mMap.put(key, sharedPreferences.getAll().get(key));
			} else {
				mMap.remove(key);
			}
			listeners.addAll(mListeners.keySet());
		}
		for (OnSharedPreferenceChangeListener listener : listeners) {
			if (listener != null) {
				listener.onSharedPreferenceChanged(this, key);
			}
		}
	}

    public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
        	mListeners.put(listener, mContent);
        }
    }

    public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener listener) {
        synchronized(this) {
        	mListeners.remove(listener);
        }
    }

    public Map<String, ?> getAll() {
        synchronized(this) {
            //noinspection unchecked
            return new HashMap<String, Object>(mMap);
        }
    }

    public String getString(String key, String defValue) {
        synchronized (this) {
            String v = (String)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    public int getInt(String key, int defValue) {
        synchronized (this) {
            Integer v = (Integer)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public long getLong(String key, long defValue) {
        synchronized (this) {
            Long v = (Long)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public float getFloat(String key, float defValue) {
        synchronized (this) {
            Float v = (Float)mMap.get(key);
            return v != null ? v : defValue;
        }
    }
    public boolean getBoolean(String key, boolean defValue) {
        synchronized (this) {
            Boolean v = (Boolean)mMap.get(key);
            return v != null ? v : defValue;
        }
    }

    public boolean contains(String key) {
        synchronized (this) {
            return mMap.containsKey(key);
        }
    }

    public Editor edit() {
        return new AsyncEditorImpl();
    }
	
	
	public final class AsyncEditorImpl implements SharedPreferences.Editor {

		private final Map<String, Object> mModified = new HashMap<String, Object>();
        private boolean mClear = false;
		private SharedPreferences.Editor editor = prefs.edit();
        
        public Editor putString(String key, String value) {
            synchronized (this) {
                mModified.put(key, value);
                editor.putString(key, value);
                return this;
            }
        }
        public Editor putStringSet(String key, Set<String> value) {
            synchronized (this) {
              mModified.put(key, value);
              if (setStringSetMethod == null) {
                throw new RuntimeException("Method does not exist.");
              } else {
                try {
                  setStringSetMethod.invoke(editor, key, value);
                } catch (IllegalArgumentException e) {
                  Log.wtf(LogUtil.TAG, "Unable to putStringSet apply for some reason.", e);
                } catch (IllegalAccessException e) {
                  Log.wtf(LogUtil.TAG, "Unable to putStringSet apply for some reason.", e);
                } catch (InvocationTargetException e) {
                  Log.wtf(LogUtil.TAG, "Unable to putStringSet apply for some reason.", e);
                }
              }
              return this;
            }
        }
        public Editor putInt(String key, int value) {
            synchronized (this) {
                mModified.put(key, value);
                editor.putInt(key, value);
                return this;
            }
        }
        public Editor putLong(String key, long value) {
            synchronized (this) {
                mModified.put(key, value);
                editor.putLong(key, value);
                return this;
            }
        }
        public Editor putFloat(String key, float value) {
            synchronized (this) {
                mModified.put(key, value);
                editor.putFloat(key, value);
                return this;
            }
        }
        public Editor putBoolean(String key, boolean value) {
            synchronized (this) {
                mModified.put(key, value);
                editor.putBoolean(key, value);
                return this;
            }
        }

        public Editor remove(String key) {
            synchronized (this) {
                mModified.put(key, this);
                editor.remove(key);
                return this;
            }
        }

        public Editor clear() {
            synchronized (this) {
                mClear = true;
                editor.clear();
                return this;
            }
        }
        
		@Override
		public boolean commit() {
			synchronized (AsyncSharedPrefs.this) {
				synchronized (this) {
					if (mClear) {
                        if (!mMap.isEmpty()) {
                            mMap.clear();
                        }
                        mClear = false;
                    }
					
                    for (Entry<String, Object> e : mModified.entrySet()) {
                        String k = e.getKey();
                        Object v = e.getValue();
                        if (v == this) {  // magic value for a removal mutation
                            mMap.remove(k);
                        } else {
                            mMap.put(k, v);
                        }
                    }
                    mModified.clear();
				}
			}

			new Thread(new Runnable() {
				@Override
				public void run() {
					boolean success = editor.commit();
					if (!success) {
						Log.w(TAG, "AsyncSharedPrefs failed to commit changes to disk.  Rolling back.");
						synchronized (AsyncSharedPrefs.this) {
							// Reset to prefs
							mMap.clear();
							mMap.putAll(prefs.getAll());
						}
					}
				}
			}).start();
			return true;
		}
		
        @Override
        public void apply() {
          if (applyMethod == null) {
            commit();
          } else {
            try {
              applyMethod.invoke(this);
            } catch (IllegalArgumentException e) {
              Log.wtf(LogUtil.TAG, "Unable to run apply for some reason.", e);
            } catch (IllegalAccessException e) {
              Log.wtf(LogUtil.TAG, "Unable to run apply for some reason.", e);
            } catch (InvocationTargetException e) {
              Log.wtf(LogUtil.TAG, "Unable to run apply for some reason.", e);
            }
          }
        }
		
		
	}
	

	  @Override
	  public Set<String> getStringSet(String key, Set<String> defaultValue) {
	    if (getStringSetMethod == null) {
	      Log.wtf(LogUtil.TAG, "Unable to run getStringSet for some reason.");
	      return defaultValue;
	    } else {
	      try {
            return (Set<String>)getStringSetMethod.invoke(this, key, defaultValue);
          } catch (IllegalArgumentException e) {
            Log.wtf(LogUtil.TAG, "Unable to run getStringSet for some reason.", e);
          } catch (IllegalAccessException e) {
            Log.wtf(LogUtil.TAG, "Unable to run getStringSet for some reason.", e);
          } catch (InvocationTargetException e) {
            Log.wtf(LogUtil.TAG, "Unable to run getStringSet for some reason.", e);
          }
	      return defaultValue;
	    }
	  }
	
	
	// STATIC apply
	
	
	private static final Method applyMethod = getApplyMethod();
	
	private static Method getApplyMethod() {
		try {
			return SharedPreferences.Editor.class.getMethod("apply");
		} catch (NoSuchMethodException e) {
			Log.i(TAG, "Apply method does not exist, using async commit.");
		}
		return null;
	}

    private static final Method setStringSetMethod = getSetStringSetMethod();
    
    private static Method getSetStringSetMethod() {
        try {
            return SharedPreferences.Editor.class.getMethod("putStringSet");
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "putStringSet method does not exist, using async commit.");
        }
        return null;
    }
    
    private static final Method getStringSetMethod = getGetStringSetMethod();
    
    private static Method getGetStringSetMethod() {
        try {
            return SharedPreferences.class.getMethod("getStringSet");
        } catch (NoSuchMethodException e) {
            Log.i(TAG, "putStringSet method does not exist, using async commit.");
        }
        return null;
    }
	
	
	/**
	 * Asynchronous commit of shared preferences values
	 * @param editor
	 */
	public static void apply(final SharedPreferences.Editor editor) {
		// Use the apply method if it exists
		try {
			applyMethod.invoke(editor);
			return;
		} catch (InvocationTargetException unused) {
			// fall through
		} catch (IllegalAccessException unused) {
			// fall through
		}
		// Commit if for some reason using apply does not work
		// No apply method, spin up thread to commit
		// TODO: we should have only one thread committing, and the ability to retry if the commit fails.
		new Thread(new Runnable() {
			@Override
			public void run() {
				editor.commit();
			}
		}).start();
	}


}
