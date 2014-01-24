
package com.esotericsoftware.kryo.serializers;

import java.io.IOException;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.spec.SecretKeySpec;

import com.esotericsoftware.kryo.Kryo;
import com.esotericsoftware.kryo.KryoException;
import com.esotericsoftware.kryo.Serializer;
import com.esotericsoftware.kryo.io.Input;
import com.esotericsoftware.kryo.io.Output;

/** Encrypts data using the blowfish cipher.
 * @author Nathan Sweet <misc@n4te.com> */
public class BlowfishSerializer extends Serializer {
	private final Serializer serializer;
	static private SecretKeySpec keySpec;

	public BlowfishSerializer (Serializer serializer, byte[] key) {
		this.serializer = serializer;
		keySpec = new SecretKeySpec(key, "Blowfish");
	}

	public void write (Kryo kryo, Output output, Object object) {
		Cipher cipher = getCipher(Cipher.ENCRYPT_MODE);
		CipherOutputStream cipherStream = new CipherOutputStream(output, cipher);
		Output cipherOutput = new Output(cipherStream, 256) {
			public void close () throws KryoException {
				// Don't allow the CipherOutputStream to close the output.
			}
		};
		serializer.write(kryo, cipherOutput, object);
		cipherOutput.flush();
		try {
			cipherStream.close();
		} catch (IOException ex) {
			throw new KryoException(ex);
		}
	}

	public Object read (Kryo kryo, Input input, Class type) {
		Cipher cipher = getCipher(Cipher.DECRYPT_MODE);
		CipherInputStream cipherInput = new CipherInputStream(input, cipher);
		return serializer.read(kryo, new Input(cipherInput, 256), type); 
	}

	public Object copy (Kryo kryo, Object original) {
		return serializer.copy(kryo, original);
	}

	static private Cipher getCipher (int mode) {
		try {
			Cipher cipher = Cipher.getInstance("Blowfish");
			cipher.init(mode, keySpec);
			return cipher;
		} catch (Exception ex) {
			throw new KryoException(ex);
		}
	}
}
