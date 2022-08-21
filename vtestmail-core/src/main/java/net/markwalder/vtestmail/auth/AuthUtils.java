/*
 * Copyright 2022 Stephan Markwalder
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.markwalder.vtestmail.auth;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.Base64;
import javax.crypto.Mac;
import net.markwalder.vtestmail.utils.Hex;
import net.markwalder.vtestmail.utils.HmacUtils;

class AuthUtils {

	static String encodeBase64(String data, Charset charset) {
		byte[] bytes = data.getBytes(charset);
		Base64.Encoder encoder = Base64.getEncoder();
		return encoder.encodeToString(bytes);
	}

	static String decodeBase64(String data, Charset charset) {
		Base64.Decoder decoder = Base64.getDecoder();
		try {
			byte[] bytes = decoder.decode(data);
			return new String(bytes, charset);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
	}

	static String calculateHmacMD5Hex(String challenge, String password) {
		Mac mac = HmacUtils.getMac("HmacMD5", password.getBytes(UTF_8));
		byte[] bytes = mac.doFinal(challenge.getBytes(UTF_8));
		return Hex.encode(bytes);
	}

}
