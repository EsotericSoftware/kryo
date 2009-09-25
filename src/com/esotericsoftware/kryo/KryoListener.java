
package com.esotericsoftware.kryo;

/**
 * Provides notification of {@link Kryo} events.
 * @author Nathan Sweet <misc@n4te.com>
 */
public interface KryoListener {
	/**
	 * Called when a remote entity is no longer available. This allows, for example, a context to release any resources it may be
	 * storing for the entity.
	 * @see Context#getRemoteEntityID()
	 * @see Kryo#removeListener(KryoListener)
	 */
	public void remoteEntityRemoved (int id);
}
