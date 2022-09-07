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

import java.security.SecureRandom;

public class RandomStringUtils {

	private static final SecureRandom RANDOM = new SecureRandom();

	private RandomStringUtils() {
		// utility class
	}

	public static String randomAscii(int count) {
		Assert.isInRange(count, 1, Integer.MAX_VALUE, "count");
		StringBuilder buffer = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			int value = 33 + RANDOM.nextInt(95);
			buffer.append((char) value);
		}
		return buffer.toString();
	}

	public static String randomAlphanumeric(int count) {
		Assert.isInRange(count, 1, Integer.MAX_VALUE, "count");
		StringBuilder buffer = new StringBuilder(count);
		for (int i = 0; i < count; i++) {
			int value = RANDOM.nextInt(62);
			if (value < 26) {
				value = 'A' + value;
			} else if (value < 52) {
				value = 'a' - 26 + value;
			} else {
				value = '0' - 52 + value;
			}
			buffer.append((char) value);
		}
		return buffer.toString();
	}

}
