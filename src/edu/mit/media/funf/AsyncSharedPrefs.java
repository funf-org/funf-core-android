package edu.mit.media.funf;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.Map.Entry;

import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Looper;
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
		synchronized (this) {
			if (prefs.contains(key)) {
				mMap.put(key, sharedPreferences.getAll().get(key));
			} else {
				mMap.remove(key);
			}
			// Already will be running on main thread
			for (OnSharedPreferenceChangeListener listener : mListeners.keySet()) {
				if (listener != null) {
					listener.onSharedPreferenceChanged(this, key);
				}
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
	}
	
	
	// STATIC apply
	
	
	private static final Method applyMethod = getApplyMethod();
	
	private static Method getApplyMethod() {
		try {
			return SharedPreferences.Editor.class.getMethod("apply");
		} catch (NoSuchMethodException e) {
			Log.i(Utils.TAG, "Apply method does not exist, using async commit.");
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
		new Thread(new Runnable() {
			@Override
			public void run() {
				editor.commit();
			}
		}).start();
	}

}
