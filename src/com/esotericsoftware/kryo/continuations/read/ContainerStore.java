package com.esotericsoftware.kryo.continuations.read;

/***
 * Placeholder for storing the serialized object in a container
 * @author Roman Levenstein <romxilev@gmail.com>
 *
 */
public class ContainerStore implements Store {
	final private Object[] container;
	private int idx;
	
	public ContainerStore(Object[] container, int idx) {
		this.container = container;
		this.idx = idx;
	}
	
	@Override
	public void store(Object o) {
		container[idx] = o;
	}

	public void setIdx(int idx) {
	    this.idx = idx;
    }
	
}
