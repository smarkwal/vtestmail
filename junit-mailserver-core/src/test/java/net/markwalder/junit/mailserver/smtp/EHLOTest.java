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

package net.markwalder.junit.mailserver.smtp;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class EHLOTest extends CommandTest {

	@Test
	void execute() throws ProtocolException, IOException {

		// mock
		List<String> authTypes = List.of("PLAIN", "LOGIN");
		Mockito.doReturn(authTypes).when(server).getAuthTypes();

		// prepare
		Command command = new EHLO();

		// test
		command.execute("EHLO", server, session, client);

		// verify
		Mockito.verify(server).getAuthTypes();
		Mockito.verify(client).writeLine("250-STARTTLS");
		Mockito.verify(client).writeLine("250-AUTH PLAIN LOGIN");
		Mockito.verify(client).writeLine("250-ENHANCEDSTATUSCODES");
		Mockito.verify(client).writeLine("250 OK");

		Mockito.verifyNoMoreInteractions(server, session, client);
	}

	@Test
	void execute_noAuthTypes() throws ProtocolException, IOException {

		// prepare
		List<Object> authTypes = Collections.emptyList();
		Mockito.doReturn(authTypes).when(server).getAuthTypes();
		Command command = new EHLO();

		// test
		command.execute("EHLO", server, session, client);

		// verify
		Mockito.verify(server).getAuthTypes();
		Mockito.verify(client).writeLine("250-STARTTLS");
		Mockito.verify(client).writeLine("250-ENHANCEDSTATUSCODES");
		Mockito.verify(client).writeLine("250 OK");

		Mockito.verifyNoMoreInteractions(server, session, client);
	}

}