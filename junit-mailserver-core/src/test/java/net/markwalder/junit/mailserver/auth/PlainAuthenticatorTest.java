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

package net.markwalder.junit.mailserver.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class PlainAuthenticatorTest extends AuthenticatorTest {

	private static final String NULL = "\u0000";

	private final PlainAuthenticator authenticator = new PlainAuthenticator();

	@Test
	void authenticate_withInitialResponse() throws IOException {

		// prepare
		String parameters = AuthUtils.encodeBase64("alice@localhost" + NULL + "alice" + NULL + "password123");

		// test
		Credentials credentials = authenticator.authenticate(parameters, client, store);

		// assert
		assertThat(credentials).isNotNull();
		assertThat(credentials.getUsername()).isEqualTo("alice");
		assertThat(credentials.getSecret()).isEqualTo("password123");

		// verify
		Mockito.verifyNoMoreInteractions(client, store);
	}

	@Test
	void authenticate_withoutInitialResponse() throws IOException {

		// mock
		String parameters = AuthUtils.encodeBase64("alice@localhost" + NULL + "alice" + NULL + "password123");
		Mockito.doReturn(parameters).when(client).readLine();

		// test
		Credentials credentials = authenticator.authenticate(null, client, store);

		// assert
		assertThat(credentials).isNotNull();
		assertThat(credentials.getUsername()).isEqualTo("alice");
		assertThat(credentials.getSecret()).isEqualTo("password123");

		// verify
		Mockito.verify(client).writeLine("334");
		Mockito.verify(client).readLine();
		Mockito.verifyNoMoreInteractions(client, store);
	}

	@Test
	void authenticate_withoutAuthorizationIdentity() throws IOException {

		// prepare
		String parameters = AuthUtils.encodeBase64(NULL + "alice" + NULL + "password123");

		// test
		Credentials credentials = authenticator.authenticate(parameters, client, store);

		// assert
		assertThat(credentials).isNotNull();
		assertThat(credentials.getUsername()).isEqualTo("alice");
		assertThat(credentials.getSecret()).isEqualTo("password123");

		// verify
		Mockito.verifyNoMoreInteractions(client, store);
	}

	@Test
	void authenticate_withEmptyPassword() throws IOException {

		// prepare
		String parameters = AuthUtils.encodeBase64("alice@localhost" + NULL + "alice" + NULL);

		// test
		Credentials credentials = authenticator.authenticate(parameters, client, store);

		// assert
		assertThat(credentials).isNotNull();
		assertThat(credentials.getUsername()).isEqualTo("alice");
		assertThat(credentials.getSecret()).isEmpty();

		// verify
		Mockito.verifyNoMoreInteractions(client, store);
	}

	@Test
	void authenticate_returnsNull_forInvalidResponse() throws IOException {

		// prepare: invalid response
		String parameters = AuthUtils.encodeBase64("alice" + NULL + "password123");

		// test
		Credentials credentials = authenticator.authenticate(parameters, client, store);

		// assert
		assertThat(credentials).isNull();

		// verify
		Mockito.verifyNoMoreInteractions(client, store);
	}

	@Test
	void authenticate_returnsNull_forInvalidBase64() throws IOException {

		// prepare: invalid response
		String parameters = "invalid base64";

		// test
		Credentials credentials = authenticator.authenticate(parameters, client, store);

		// assert
		assertThat(credentials).isNull();

		// verify
		Mockito.verifyNoMoreInteractions(client, store);
	}

}