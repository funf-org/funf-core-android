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
