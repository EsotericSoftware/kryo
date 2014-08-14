package com.esotericsoftware.kryo;

import com.esotericsoftware.kryo.util.DefaultClassResolver;
import com.esotericsoftware.kryo.util.DefaultStreamFactory;
import com.esotericsoftware.kryo.util.FastestStreamFactory;
import com.esotericsoftware.kryo.util.MapReferenceResolver;
import java.lang.ref.WeakReference;
import junit.framework.TestCase;

/**
 * Tests for detecting PermGen memory leaks.
 * 
 * @author serverperformance
 */
public class GarbageCollectionTest extends TestCase {

	public void testDefaultStreamFactory () {
		final DefaultStreamFactory strongRefToStreamFactory = new DefaultStreamFactory();
		Kryo kryo = new Kryo(new DefaultClassResolver(), new MapReferenceResolver(), strongRefToStreamFactory);
		WeakReference<Kryo> kryoWeakRef = new WeakReference<Kryo>(kryo);
		kryo = null; // remove strong ref, now kryo is only weak-reachable
		reclaim(kryoWeakRef);
	}

	public void testFastestStreamFactory () {
		final FastestStreamFactory strongRefToStreamFactory = new FastestStreamFactory();
		Kryo kryo = new Kryo(new DefaultClassResolver(), new MapReferenceResolver(), strongRefToStreamFactory);
		WeakReference<Kryo> kryoWeakRef = new WeakReference<Kryo>(kryo);
		kryo = null; // remove strong ref, now kryo is only weak-reachable
		reclaim(kryoWeakRef);
	}
	
	private void reclaim (WeakReference<Kryo> kryoWeakRef) {
		// Forces GC
		System.gc();
		// Waits for recaim the weaked-reachable kryo instance
		int times = 0;
		while (kryoWeakRef.get() != null && times < 30) { // limit 3 seconds
			try { Thread.sleep(100); } catch (InterruptedException ignored) {}
			times++;
		}
		assertNull(kryoWeakRef.get());
	}
}
