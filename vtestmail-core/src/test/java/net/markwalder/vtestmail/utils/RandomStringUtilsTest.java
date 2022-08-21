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

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class RandomStringUtilsTest {

	@Test
	void randomAscii() {

		for (int i = 1; i < 100; i++) {

			// test
			String result = RandomStringUtils.randomAscii(i);

			// assert
			assertThat(result).hasSize(i);
			assertThat(result).matches("^[\\x21-\\x7F]*$");
		}
	}

	@Test
	void randomAlphanumeric() {

		for (int i = 1; i < 100; i++) {

			// test
			String result = RandomStringUtils.randomAlphanumeric(i);

			// assert
			assertThat(result).hasSize(i);
			assertThat(result).matches("^[a-zA-Z0-9]*$");
		}
	}

}