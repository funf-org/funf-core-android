package edu.mit.media.funf;

public interface DataNormalizer<T> {

	public T normalize(T data);
	
	public class EmailNormalizer implements DataNormalizer<String> {

		@Override
		public String normalize(String data) {
			return data == null ? null : data.trim().toLowerCase();
		}
		
	}
	
	public class PhoneNumberNormalizer implements DataNormalizer<String> {

		@Override
		public String normalize(String numberString) {
			numberString = numberString.replaceAll("[^0-9]","");
			int i = numberString.length();
			if (i <= 10)
				return numberString;
			else
				return numberString.substring(i - 10);
		}
		
	}
}
