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

public class Hex {

	private static final char[] DIGITS = { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

	public static String encode(byte[] data) {
		Assert.isNotNull(data, "data");
		int len = data.length;
		char[] chars = new char[len * 2];
		for (int i = 0; i < len; i++) {
			int val1 = (0xF0 & data[i]) >>> 4;
			int val2 = 0x0F & data[i];
			chars[i * 2] = DIGITS[val1];
			chars[i * 2 + 1] = DIGITS[val2];
		}
		return new String(chars);
	}

}
