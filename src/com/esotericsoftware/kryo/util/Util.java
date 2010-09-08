
package com.esotericsoftware.kryo.util;

public class Util {
	static public boolean isAndroid;
	static {
		try {
			Class.forName("android.os.Process");
			isAndroid = true;
		} catch (Exception ex) {
			isAndroid = false;
		}
	}
}
