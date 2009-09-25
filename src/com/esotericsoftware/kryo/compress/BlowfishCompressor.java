
package com.esotericsoftware.kryo.compress;

import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;

import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;

import com.esotericsoftware.kryo.Compressor;
import com.esotericsoftware.kryo.Context;
import com.esotericsoftware.kryo.SerializationException;
import com.esotericsoftware.kryo.Serializer;

/**
 * Encrypts data using the blowfish cipher.
 * @author Nathan Sweet <misc@n4te.com>
 */
public class BlowfishCompressor extends Compressor {
	private SecretKeySpec keySpec;

	public BlowfishCompressor (Serializer serializer, byte[] key) {
		this(serializer, key, 2048);
	}

	public BlowfishCompressor (Serializer serializer, byte[] key, int bufferSize) {
		super(serializer, bufferSize);
		keySpec = new SecretKeySpec(key, "Blowfish");
	}

	public void compress (ByteBuffer inputBuffer, Object object, ByteBuffer outputBuffer) {
		Context context = getContext();
		Cipher encrypt = (Cipher)context.get("blowfishEncryptCipher");
		try {
			if (encrypt == null) {
				encrypt = Cipher.getInstance("Blowfish");
				encrypt.init(Cipher.ENCRYPT_MODE, keySpec);
				context.put("blowfishEncryptCipher", encrypt);
			}
			encrypt.doFinal(inputBuffer, outputBuffer);
		} catch (GeneralSecurityException ex) {
			throw new SerializationException(ex);
		}
	}

	public void decompress (ByteBuffer inputBuffer, Class type, ByteBuffer outputBuffer) {
		Context context = getContext();
		Cipher decrypt = (Cipher)context.get("blowfishEncryptCipher");
		try {
			if (decrypt == null) {
				decrypt = Cipher.getInstance("Blowfish");
				decrypt.init(Cipher.DECRYPT_MODE, keySpec);
				context.put("blowfishEncryptCipher", decrypt);
			}
			decrypt.doFinal(inputBuffer, outputBuffer);
		} catch (GeneralSecurityException ex) {
			throw new SerializationException(ex);
		}
	}
}
