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

package net.markwalder.vtestmail.utils;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DigestUtils {

	private DigestUtils() {
		// utility class
	}

	public static String md5Hex(String data, Charset charset) {
		return Hex.encode(md5(data, charset));
	}

	public static String md5Hex(byte[] data) {
		return Hex.encode(md5(data));
	}

	public static byte[] md5(String data, Charset charset) {
		Assert.isNotNull(data, "data");
		Assert.isNotNull(charset, "charset");
		return md5(data.getBytes(charset));
	}

	public static byte[] md5(byte[] data) {
		Assert.isNotNull(data, "data");
		MessageDigest digest = getMd5Digest();
		return digest.digest(data);
	}

	private static MessageDigest getMd5Digest() {
		try {
			return MessageDigest.getInstance("MD5");
		} catch (NoSuchAlgorithmException e) {
			throw new IllegalArgumentException(e);
		}
	}

}
