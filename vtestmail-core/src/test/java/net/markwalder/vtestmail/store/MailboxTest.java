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

package net.markwalder.vtestmail.store;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class MailboxTest {

	private final Mailbox mailbox = new Mailbox("alice", "password123", "alice@localhost");

	@Test
	void getUsername() {
		assertThat(mailbox.getUsername()).isEqualTo("alice");
	}

	@Test
	void getSecret() {
		assertThat(mailbox.getSecret()).isEqualTo("password123");
	}

	@Test
	void getEmail() {
		assertThat(mailbox.getEmail()).isEqualTo("alice@localhost");
	}

	// TODO: add tests for folders

}