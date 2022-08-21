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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class DigestUtilsTest {

	@Test
	void md5Hex() {

		// prepare
		String message = "This is a test message.";

		// test
		String result = DigestUtils.md5Hex(message, UTF_8);

		// assert
		assertThat(result).isEqualTo("f8900247f0d5874f453318549411c6fa");

		// test
		result = DigestUtils.md5Hex(message.getBytes(UTF_8));

		// assert
		assertThat(result).isEqualTo("f8900247f0d5874f453318549411c6fa");
	}

	@Test
	void md5() {

		// prepare
		String message = "This is a test message.";

		// test
		byte[] result = DigestUtils.md5(message, UTF_8);

		// assert
		assertThat(result).asHexString().isEqualTo("F8900247F0D5874F453318549411C6FA");

		// test
		result = DigestUtils.md5(message.getBytes(UTF_8));

		// assert
		assertThat(result).asHexString().isEqualTo("F8900247F0D5874F453318549411C6FA");
	}

}