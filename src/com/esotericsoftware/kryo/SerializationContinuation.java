package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/***
 * This is base class for all classes implementing serialization by means of
 * continuations. This approach follows a Continuation Passing Style (CPS) and
 * allows for serialization of very deeply nested data structures.
 * 
 * @author Roman Levenstein <romixlev@gmail.com>
 * 
 */
public class SerializationContinuation {
//	protected Output out;
//	protected Input in;
	protected SerializationContinuation prev;

	public SerializationContinuation(Input in) {
//		this.in = in;
	}

	public SerializationContinuation(Output out) {
//		this.out = out;
	}

	/***
	 *  Write the remaining part by means of this continuation
	 * @param kryo
	 * @return next continuation to be executed
	 */
	public SerializationContinuation processWrite(Kryo kryo, Output out, boolean popCont) {
		return null;
	}

	/***
	 *  Write the remaining part by means of this continuation
	 * @param kryo
	 * @return next continuation to be executed
	 */
	public Object processRead(Kryo kryo, Input in, boolean popCont) {
		return null;
	}
	
	public void setPrevious(SerializationContinuation prev) {
		this.prev = prev;
	}
	
	public SerializationContinuation getPrevious() {
		return prev;
	}
}
