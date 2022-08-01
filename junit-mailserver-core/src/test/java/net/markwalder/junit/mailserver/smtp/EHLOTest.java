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
	void execute() throws SmtpException, IOException {

		// mock
		Mockito.doReturn("localhost").when(session).getServerAddress();
		Mockito.doReturn(true).when(server).isCommandEnabled("STARTTLS");
		Mockito.doReturn(true).when(server).isCommandEnabled("AUTH");
		List<String> authTypes = List.of("PLAIN", "LOGIN");
		Mockito.doReturn(authTypes).when(server).getAuthTypes();
		Mockito.doReturn(true).when(server).isCommandEnabled("VRFY");
		Mockito.doReturn(false).when(server).isCommandEnabled("EXPN");

		// prepare
		SmtpCommand command = new EHLO("localhost");

		// test
		command.execute(server, session, client);

		// verify
		Mockito.verify(session).getServerAddress();
		Mockito.verify(client).writeLine("250-localhost Hello localhost");
		Mockito.verify(server).isCommandEnabled("STARTTLS");
		Mockito.verify(server).isCommandEnabled("AUTH");
		Mockito.verify(server).getAuthTypes();
		Mockito.verify(server).isCommandEnabled("VRFY");
		Mockito.verify(server).isCommandEnabled("EXPN");
		Mockito.verify(client).writeLine("250-STARTTLS");
		Mockito.verify(client).writeLine("250-AUTH PLAIN LOGIN");
		Mockito.verify(client).writeLine("250-VRFY");
		Mockito.verify(client).writeLine("250-ENHANCEDSTATUSCODES");
		Mockito.verify(client).writeLine("250 OK");

		Mockito.verifyNoMoreInteractions(server, session, client);
	}

	@Test
	void execute_noAuthTypes() throws SmtpException, IOException {

		// mock
		Mockito.doReturn("localhost").when(session).getServerAddress();
		Mockito.doReturn(true).when(server).isCommandEnabled("STARTTLS");
		Mockito.doReturn(true).when(server).isCommandEnabled("AUTH");
		List<Object> authTypes = Collections.emptyList();
		Mockito.doReturn(authTypes).when(server).getAuthTypes();
		Mockito.doReturn(true).when(server).isCommandEnabled("VRFY");
		Mockito.doReturn(false).when(server).isCommandEnabled("EXPN");

		// prepare
		SmtpCommand command = new EHLO("localhost");

		// test
		command.execute(server, session, client);

		// verify
		Mockito.verify(session).getServerAddress();
		Mockito.verify(client).writeLine("250-localhost Hello localhost");
		Mockito.verify(server).isCommandEnabled("STARTTLS");
		Mockito.verify(server).isCommandEnabled("AUTH");
		Mockito.verify(server).getAuthTypes();
		Mockito.verify(server).isCommandEnabled("VRFY");
		Mockito.verify(server).isCommandEnabled("EXPN");
		Mockito.verify(client).writeLine("250-STARTTLS");
		Mockito.verify(client).writeLine("250-VRFY");
		Mockito.verify(client).writeLine("250-ENHANCEDSTATUSCODES");
		Mockito.verify(client).writeLine("250 OK");

		Mockito.verifyNoMoreInteractions(server, session, client);
	}

}