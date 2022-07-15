package net.markwalder.junit.mailserver.auth;

import java.nio.charset.StandardCharsets;
import java.util.Base64;

class AuthUtils {

	static String encodeBase64(String data) {
		byte[] bytes = data.getBytes(StandardCharsets.UTF_8);
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(bytes);
	}

	static String decodeBase64(String data) {
		Base64.Decoder decoder = Base64.getDecoder();
		try {
			byte[] bytes = decoder.decode(data);
			return new String(bytes, StandardCharsets.UTF_8);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

}
