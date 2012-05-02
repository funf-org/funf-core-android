package edu.mit.media.funf.json;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonPrimitive;

/**
 * An immutable JsonArray implementation, which contains no mutation functions
 *  and only contains immutable JsonElements in the tree.
 * 
 * @author Alan Gardner
 */
public class IJsonArray extends JsonElement {

	private final List<JsonElement> elements;

	public IJsonArray(JsonArray jsonArray) {
		if (jsonArray == null) {
			throw new IllegalStateException("Cannot create null IJsonArray");
		}
		List<JsonElement> list = new ArrayList<JsonElement>();
		for (JsonElement element : jsonArray) {
			list.add(JsonUtils.immutable(element));
		}
		elements = Collections.unmodifiableList(list);
	}
	
	@Override
	public boolean isJsonArray() {
		return true;
	}

	@Override
	public JsonArray getAsJsonArray() {
		JsonArray jsonArray = new JsonArray();
		for (JsonElement element : elements) {
			jsonArray.add(element);
		}
		return jsonArray;
	}

	/**
	 * Returns the number of elements in the array.
	 * 
	 * @return the number of elements in the array.
	 */
	public int size() {
		return elements.size();
	}

	/**
	 * Returns an iterator to navigate the elements of the array. Since the
	 * array is an ordered list, the iterator navigates the elements in the
	 * order they were inserted.
	 * 
	 * @return an iterator to navigate the elements of the array.
	 */
	public Iterator<JsonElement> iterator() {
		return elements.iterator();
	}

	/**
	 * Returns the ith element of the array.
	 * 
	 * @param i
	 *            the index of the element that is being sought.
	 * @return the element present at the ith index.
	 * @throws IndexOutOfBoundsException
	 *             if i is negative or greater than or equal to the
	 *             {@link #size()} of the array.
	 */
	public JsonElement get(int i) {
		return elements.get(i);
	}

	/**
	 * convenience method to get this array as a {@link Number} if it contains a
	 * single element.
	 * 
	 * @return get this element as a number if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid Number.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public Number getAsNumber() {
		if (elements.size() == 1) {
			return elements.get(0).getAsNumber();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a {@link String} if it contains a
	 * single element.
	 * 
	 * @return get this element as a String if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid String.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public String getAsString() {
		if (elements.size() == 1) {
			return elements.get(0).getAsString();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a double if it contains a single
	 * element.
	 * 
	 * @return get this element as a double if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid double.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public double getAsDouble() {
		if (elements.size() == 1) {
			return elements.get(0).getAsDouble();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a {@link BigDecimal} if it
	 * contains a single element.
	 * 
	 * @return get this element as a {@link BigDecimal} if it is single element
	 *         array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             .
	 * @throws NumberFormatException
	 *             if the element at index 0 is not a valid {@link BigDecimal}.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 * @since 1.2
	 */
	@Override
	public BigDecimal getAsBigDecimal() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBigDecimal();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a {@link BigInteger} if it
	 * contains a single element.
	 * 
	 * @return get this element as a {@link BigInteger} if it is single element
	 *         array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             .
	 * @throws NumberFormatException
	 *             if the element at index 0 is not a valid {@link BigInteger}.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 * @since 1.2
	 */
	@Override
	public BigInteger getAsBigInteger() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBigInteger();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a float if it contains a single
	 * element.
	 * 
	 * @return get this element as a float if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid float.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public float getAsFloat() {
		if (elements.size() == 1) {
			return elements.get(0).getAsFloat();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a long if it contains a single
	 * element.
	 * 
	 * @return get this element as a long if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid long.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public long getAsLong() {
		if (elements.size() == 1) {
			return elements.get(0).getAsLong();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as an integer if it contains a
	 * single element.
	 * 
	 * @return get this element as an integer if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid integer.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public int getAsInt() {
		if (elements.size() == 1) {
			return elements.get(0).getAsInt();
		}
		throw new IllegalStateException();
	}

	@Override
	public byte getAsByte() {
		if (elements.size() == 1) {
			return elements.get(0).getAsByte();
		}
		throw new IllegalStateException();
	}

	@Override
	public char getAsCharacter() {
		if (elements.size() == 1) {
			return elements.get(0).getAsCharacter();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a primitive short if it contains
	 * a single element.
	 * 
	 * @return get this element as a primitive short if it is single element
	 *         array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid short.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public short getAsShort() {
		if (elements.size() == 1) {
			return elements.get(0).getAsShort();
		}
		throw new IllegalStateException();
	}

	/**
	 * convenience method to get this array as a boolean if it contains a single
	 * element.
	 * 
	 * @return get this element as a boolean if it is single element array.
	 * @throws ClassCastException
	 *             if the element in the array is of not a {@link JsonPrimitive}
	 *             and is not a valid boolean.
	 * @throws IllegalStateException
	 *             if the array has more than one element.
	 */
	@Override
	public boolean getAsBoolean() {
		if (elements.size() == 1) {
			return elements.get(0).getAsBoolean();
		}
		throw new IllegalStateException();
	}

	@Override
	public boolean equals(Object o) {
		return (o == this) || (o instanceof IJsonArray && ((IJsonArray) o).elements.equals(elements));
	}

	@Override
	public int hashCode() {
		return elements.hashCode();
	}
	
	private String toStringCache = null;
	@Override
	public String toString() {
		// Since this is immutable, the string result can be cached
		// Does not need to be synchronized, last one is kept
		if (toStringCache == null) {
			toStringCache = super.toString();
		}
		return toStringCache;
	}
}
