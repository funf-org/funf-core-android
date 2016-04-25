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

import java.util.Date;

import android.content.Context;
import edu.mit.media.funf.time.TimeUtil;

public interface NameGenerator {
	
	/**
	 * Generate a name using the name that was passed in.
	 * @param name
	 * @return
	 */
	public String generateName(final String name);
	
	public static class IdentityNameGenerator implements NameGenerator {
		@Override
		public String generateName(final String name) {
			return name;
		}
	}

	public static class TimestampNameGenerator implements NameGenerator {
		@Override
		public String generateName(final String name) {
			return name == null ? null : TimeUtil.getTimestamp() + "_" + name;
		}
	}
	
	public static class DatetimeNameGenerator implements NameGenerator {
		@Override
		public String generateName(final String name) {
			String datetime = java.text.DateFormat.getDateTimeInstance().format(new Date());
			return name == null ? null : datetime + "_" + name;
		}
	}
	
	public static class ConstantNameGenerator implements NameGenerator {
		
		private final String prefix, suffix;
		
		public ConstantNameGenerator(String prefix, String suffix) {
			this.prefix = prefix;
			this.suffix = suffix;
		}
		
		@Override
		public String generateName(final String name) {
			return name == null ? null : prefix + name + suffix;
		}
	}
	
	/**
	 * Applies the name generators in the order they were passed in.
	 *
	 */
	public static class CompositeNameGenerator implements NameGenerator {
		
		private final NameGenerator[] nameGenerators;
		
		public CompositeNameGenerator(NameGenerator... nameGenerators) {
			assert nameGenerators != null;
			this.nameGenerators = new NameGenerator[nameGenerators.length];
			System.arraycopy(nameGenerators, 0, this.nameGenerators, 0, nameGenerators.length);
		}
		
		@Override
		public String generateName(final String name) {
			if (name == null) {
				return null;
			}
			String transformedName = name;
			for (NameGenerator nameGenerator : nameGenerators) {
				transformedName = nameGenerator.generateName(transformedName);
			}
			return transformedName;
		}
	}
	
	public static class SystemUniqueTimestampNameGenerator implements NameGenerator {

		private final NameGenerator delegate;
		
		public SystemUniqueTimestampNameGenerator(Context context) {
			delegate = new CompositeNameGenerator(new TimestampNameGenerator(), new ConstantNameGenerator(UuidUtil.getInstallationId(context) + "_", ""));		
		}
		
		
		@Override
		public String generateName(String name) {
			return delegate.generateName(name);
		}		
	}
	
	public static class RequiredSuffixNameGenerator implements NameGenerator {

		private final String requiredSuffix;
		
		public RequiredSuffixNameGenerator(String requiredSuffix) {
			this.requiredSuffix = requiredSuffix;
		}
		
		@Override
		public String generateName(String name) {
			if (name != null && !name.toLowerCase().endsWith(requiredSuffix)) {
				name = name + requiredSuffix;
			}
			return name;
		}		
	}
}
