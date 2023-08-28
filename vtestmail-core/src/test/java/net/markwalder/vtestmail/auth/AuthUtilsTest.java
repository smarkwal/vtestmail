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

package net.markwalder.vtestmail.auth;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuthUtilsTest {

	@Test
	void calculateHmacMD5Hex() {

		// prepare
		String challenge = "MNJPy=~Pdh10)o)e";
		String password = "password!123";

		// test
		String result = AuthUtils.calculateHmacMD5Hex(challenge, password);

		// assert
		assertThat(result).isEqualTo("589a60b62700673c95644c05fcc89393");
	}

}