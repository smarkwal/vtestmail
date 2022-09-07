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

class HexTest {

	@Test
	void encode_emptyArray() {

		// prepare
		byte[] data = new byte[0];

		// test
		String result = Hex.encode(data);

		// assert
		assertThat(result).isEmpty();
	}

	@Test
	void encode_singleByte() {
		for (int i = 0; i < 256; i++) {

			// prepare
			byte[] data = { (byte) i };

			// test
			String result = Hex.encode(data);

			// assert
			assertThat(result).hasSize(2);
			assertThat(result).isEqualTo(toHex(i));
		}
	}

	@Test
	void encode_allBytes() {

		// prepare
		byte[] data = new byte[256];
		for (int i = 0; i < 256; i++) {
			data[i] = (byte) i;
		}

		// test
		String result = Hex.encode(data);

		// assert
		assertThat(result)
				.hasSize(512)
				.isEqualTo("000102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f202122232425262728292a2b2c2d2e2f303132333435363738393a3b3c3d3e3f404142434445464748494a4b4c4d4e4f505152535455565758595a5b5c5d5e5f606162636465666768696a6b6c6d6e6f707172737475767778797a7b7c7d7e7f808182838485868788898a8b8c8d8e8f909192939495969798999a9b9c9d9e9fa0a1a2a3a4a5a6a7a8a9aaabacadaeafb0b1b2b3b4b5b6b7b8b9babbbcbdbebfc0c1c2c3c4c5c6c7c8c9cacbcccdcecfd0d1d2d3d4d5d6d7d8d9dadbdcdddedfe0e1e2e3e4e5e6e7e8e9eaebecedeeeff0f1f2f3f4f5f6f7f8f9fafbfcfdfeff");
	}

	private static String toHex(int i) {
		String hex = Integer.toHexString(i);
		if (hex.length() < 2) {
			hex = "0" + hex;
		}
		return hex;
	}

}