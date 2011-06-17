package edu.mit.media.hd.funf.storage;


/**
 * Responsible for storing a representation of the object.
 *
 * @param <T>
 */
public interface Archive<T> {

	/**
	 * Adds the item to the archive
	 * @param item
	 * @return true if archive was successful
	 */
	public boolean add(T item);
	
	/**
	 * Removes the item from the archive if it exists
	 * @param item
	 * @return true if item exists in archive and was succesfully removed
	 */
	public boolean remove(T item);
	
	/**
	 * @param item
	 * @return true if item exists in archive
	 */
	public boolean contains(T item);
	
	
	/**
	 * @return All items in the archive
	 */
	public T[] getAll();
	
	

}