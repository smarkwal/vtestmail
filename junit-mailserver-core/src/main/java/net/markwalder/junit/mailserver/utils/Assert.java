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

package net.markwalder.junit.mailserver.utils;

public class Assert {

	public static void isNotNull(Object argument, String name) {
		if (argument == null) {
			throw new IllegalArgumentException(name + " must not be null");
		}
	}

	public static void isNotEmpty(String argument, String name) {
		if (argument == null || argument.isEmpty()) {
			throw new IllegalArgumentException(name + " must not be null or empty");
		}
	}

	public static void isInRange(int value, int min, int max, String name) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
		}
	}

	public static void isInRange(long value, long min, long max, String name) {
		if (value < min || value > max) {
			throw new IllegalArgumentException(name + " must be between " + min + " and " + max);
		}
	}

	public static void isTrue(boolean condition, String message) {
		if (!condition) {
			throw new IllegalArgumentException(message);
		}
	}

	public static void isFalse(boolean condition, String message) {
		if (condition) {
			throw new IllegalArgumentException(message);
		}
	}

}
