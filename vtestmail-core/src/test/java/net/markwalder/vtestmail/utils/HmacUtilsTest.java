/*
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

import javax.crypto.Mac;
import org.junit.jupiter.api.Test;

class HmacUtilsTest {

	@Test
	void getMac() {

		// prepare
		byte[] key = "super-secret".getBytes();
		byte[] message = "This is a test message.".getBytes();

		// test
		Mac mac = HmacUtils.getMac("HmacMD5", key);

		// assert
		byte[] bytes = mac.doFinal(message);
		assertThat(bytes).asHexString().isEqualTo("B51F356E81FB6E7B427BDB3F807992AA");
	}

}