package edu.mit.media.hd.funf.storage;

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
			return name == null ? null : System.currentTimeMillis() + "_" + name;
		}
	}
}
