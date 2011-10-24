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
package edu.mit.media.funf.opp;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import edu.mit.media.funf.Utils;

import android.os.Bundle;

public class OppProbe {
	
	/**
	 * The intent parameter keys reserved for OPP
	 * @author alangardner
	 *
	 */
	// TODO: Should these reserved parameters be scoped to action types?
	public enum ReservedParamaters {
		PACKAGE("PACKAGE", "Package", "The package name for who is requesting data from this probe."),
		REQUEST_ID("REQUEST_ID", "RequestId", "The client chosen identifier for this request."),
		REQUESTS("REQUESTS", "Requests", "An array of Bundles that represent individual data request for the probes."),
		NONCE("NONCE", "Nonce", "A randomly generated long that is used to verify the identity of an android package."),
		TIMESTAMP("TIMESTAMP", "Timestamp", "A long millisecond timestamp that identifies when the action was sent."),
		PARAMETERS("PARAMETERS", "Parameters", "The parameter values used to run the probe (DATA), or the defaults (STATUS).");
		
		public final String name, displayName, description;
		
		private  ReservedParamaters(String name, String displayName, String description) {
			this.name = name;
			this.displayName = displayName;
			this.description = description;
		}
	}
	
	
	/**
	 * Convenience class for interacting with an OPP probe status bundle
	 * @author alangardner
	 *
	 */
	public final static class Status {
		private Bundle bundle;
		public Status(String name, String displayName, 
				boolean enabled, boolean running, 
				long nextRun, long previousRun, 
				String[] requiredPermissions, 
				String[] requiredFeatures, 
				List<Parameter> parameters) {
			this.bundle = new Bundle();
			bundle.putBoolean("ENABLED", enabled);
			bundle.putBoolean("RUNNING", running);
			bundle.putLong("NEXT_RUN", nextRun);
			bundle.putLong("PREVIOUS_RUN", previousRun);
			bundle.putString("NAME", name);
			bundle.putString("DISPLAY_NAME", displayName);
			bundle.putStringArray("REQUIRED_PERMISSIONS", requiredPermissions == null ? new String[]{} : requiredPermissions);
			bundle.putStringArray("REQUIRED_FEATURES", requiredFeatures == null ? new String[]{} : requiredFeatures);
			ArrayList<Bundle> paramBundles = new ArrayList<Bundle>();
			if (parameters != null) {
				for (Parameter param : parameters) {
					paramBundles.add(param.getBundle());
				}
			}
			bundle.putParcelableArrayList("PARAMETERS", paramBundles);
			
		}
		public Status(Bundle bundle) {
			this.bundle = new Bundle(bundle);
		}
		
		/**
		 * A copy constructor that allows you to change the dynamic values in a status message.
		 * @param bundle
		 * @param enabled
		 * @param running
		 * @param nextRun
		 * @param previousRun
		 */
		public Status(Status otherStatus, boolean enabled, boolean running, long nextRun, long previousRun) {
			this.bundle = new Bundle(otherStatus.getBundle());
			bundle.putBoolean("ENABLED", enabled);
			bundle.putBoolean("RUNNING", running);
			bundle.putLong("NEXT_RUN", nextRun);
			bundle.putLong("PREVIOUS_RUN", previousRun);
		}
		public String getName() {
			return bundle.getString("NAME");
		}
		public String getDisplayName() {
			return bundle.getString("DISPLAY_NAME");
		}
		public boolean isEnabled() {
			return bundle.getBoolean("ENABLED");
		}
		public boolean isRunning() {
			return bundle.getBoolean("RUNNING");
		}
		public long getNextRun() {
			return bundle.getLong("NEXT_RUN");
		}
		public long getPreviousRun() {
			return bundle.getLong("PREVIOUS_RUN");
		}
		public String[] getRequiredPermissions() {
			return bundle.getStringArray("REQUIRED_PERMISSIONS");
		}
		public String[] getRequiredFeatures() {
			return bundle.getStringArray("REQUIRED_FEATURES");
		}
		public Parameter getParameter(final String name) {
			for(Parameter parameter : getParameters()) {
				if (parameter.getName().equalsIgnoreCase(name)) {
					return parameter;
				}
			}
			return null;
		}
		public Parameter[] getParameters() {
			ArrayList<Bundle> paramBundles = bundle.getParcelableArrayList("PARAMETERS");
			List<Parameter> paramList = new ArrayList<Parameter>();
			for (Bundle paramBundle : paramBundles) {
				paramList.add(new Parameter(paramBundle));
			}
			Parameter[] parameters = new Parameter[paramBundles.size()];
			paramList.toArray(parameters);
			return parameters;
		}
		public Bundle getBundle() {
			return bundle;
		}
		@Override
		public boolean equals(Object o) {
			return o != null 
			&& o instanceof Status 
			&& getName().equals(((Status)o).getName());
		}
		@Override
		public int hashCode() {
			return getName().hashCode();
		}
	}
	
	/**
	 * Represents a parameter that can be passed to a probe
	 * @author alangardner
	 *
	 */
	public static class Parameter {

		public static final String NAME_KEY = "NAME";
		public static final String DEFAULT_VALUE_KEY = "DEFAULT_VALUE";
		public static final String DISPLAY_NAME_KEY = "DISPLAY_NAME";
		public static final String DESCRIPTION_KEY = "DESCRIPTION";
		
		private boolean supportedByProbe = true;
		private final Bundle paramBundle;
		
		/**
		 * Custom parameter constructor
		 * @param name
		 * @param value
		 * @param displayName
		 * @param description
		 */
		public Parameter(final String name, final Object value, final String displayName, final String description) {
			paramBundle = new Bundle();
			paramBundle.putString(NAME_KEY, name);
			paramBundle.putString(DISPLAY_NAME_KEY, displayName);
			paramBundle.putString(DESCRIPTION_KEY, description);
			Utils.putInBundle(paramBundle, DEFAULT_VALUE_KEY, value);
		}
		
		
		/**
		 * Convenience constructor to access parameter information from a bundle.
		 * WARNING: this will not correctly set the 'isSupportedByProbe' flag.  Use only for convenient access to other parameters.
		 * @param paramBundle
		 */
		public Parameter(final Bundle paramBundle) {
			// TODO: we might want to ensure that the bundle has the appropriate keys
			this.paramBundle = paramBundle;
		}

		public String getName() {
			return paramBundle.getString(NAME_KEY);
		}

		public Object getValue() {
			return paramBundle.get(DEFAULT_VALUE_KEY);
		}

		public String getDisplayName() {
			return paramBundle.getString(DISPLAY_NAME_KEY);
		}

		public String getDescription() {
			return paramBundle.getString(DESCRIPTION_KEY);
		}
		
		public boolean isSupportedByProbe() {
			return supportedByProbe;
		}
		
		public Bundle getBundle() {
			return paramBundle;
		}
	}
	
	
	
	
	
	////////////////////////////
	// OPP
	////////////////////////////
	
	public static final String ACTION_SEPERATOR = ".";
	public static final String ACTION_POLL = "POLL";
	public static final String ACTION_STATUS = "STATUS";
	public static final String ACTION_REQUEST = "REQUEST";
	public static final String ACTION_DATA = "DATA";
	
	// TODO: make this an OPP namespace
	public static final String OPP_NAMESPACE = "edu.mit.media.funf";

	
	/**
	 * @param action
	 * @return the OPP action that is being invoked with action
	 */
	public static String getOppAction(String action) {
		if (action == null) {
			return null;
		}
		final String[] dividedAction = action.split(Pattern.quote(ACTION_SEPERATOR));
		final String oppAction = dividedAction[dividedAction.length - 1];
		if ( oppAction.equals(ACTION_DATA) || oppAction.equals(ACTION_REQUEST) || oppAction.equals(ACTION_POLL) || oppAction.equals(ACTION_STATUS)) {
			return oppAction;
		} else {
			return null;
		}
	}
	
	/**
	 * @param action
	 * @return the probe that is being invoked with action
	 */
	public static String getProbeName(final String action) {
		final String oppAction = getOppAction(action);
		// Remove .<oppAction> at the end of action
		final String ending = ACTION_SEPERATOR + oppAction;
		if (oppAction != null && action.endsWith(ending)) {
			return action.substring(0, action.length() - ending.length());
		} else {
			return null;
		}
	}
	

	// POLL

	/**
	 * @return OPP Global status request action which all available probes will respond to
	 */
	public static String getGlobalPollAction() {
		return OPP_NAMESPACE + ACTION_SEPERATOR + ACTION_POLL;
	}
	
	/**
	 * @return OPP Status request action
	 */
	public static String getPollAction(String probeName) {
		// TODO: make this an OPP name
		return probeName + ACTION_SEPERATOR + ACTION_POLL;
	}
	
	/**
	 * @return OPP Status request action
	 */
	public static String getPollAction(Class<?> probeClass) {
		// TODO: make this an OPP name
		return getPollAction(probeClass.getName());
	}

	public static boolean isPollAction(final String action) {
		return ACTION_POLL.equals(getOppAction(action));
	}
	
	// STATUS
	
	/**
	 * @return OPP status action
	 */
	public static String getStatusAction(String probeName) {
		return getStatusAction();
	}
	
	/**
	 * @return OPP status action
	 */
	public static String getStatusAction(Class<?> probeClass) {
		return getStatusAction();
	}
	
	public static String getStatusAction() {
		return OPP_NAMESPACE + ACTION_SEPERATOR + ACTION_STATUS;
	}
	
	public static boolean isStatusAction(final String action) {
		return getStatusAction().equals(action);
	}



	/**
	 * @param probeClass
	 * @return OPP Data action for probe with class
	 */
	public static String getDataAction(Class<?> probeClass) {
		return OppProbe.getDataAction(probeClass.getName());
	}

	public static String getDataAction(String probeName) {
		return probeName + ACTION_SEPERATOR + ACTION_DATA;
	}

	public static boolean isDataAction(final String action) {
		return ACTION_DATA.equals(getOppAction(action));
	}

	/**
	 * @param probeClass
	 * @return  OPP Data action for probe with class
	 */
	public static String getGetAction(String probeName) {
		return probeName + ACTION_SEPERATOR + ACTION_REQUEST;
	}

	public static String getGetAction(Class<?> probeClass) {
		return getGetAction(probeClass.getName());
	}

	public static boolean isGetAction(final String action) {
		return action != null && action.endsWith(ACTION_SEPERATOR + ACTION_REQUEST);
	}
}
