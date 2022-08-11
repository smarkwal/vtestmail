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
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class STARTTLSTest extends CommandTest {

	@Test
	void execute() throws SmtpException, IOException {

		// mock
		Mockito.doReturn("TLSv1.2").when(server).getSSLProtocol();

		// prepare
		SmtpCommand command = new STARTTLS();

		// test
		command.execute(server, session, client);

		// verify
		Mockito.verify(client).writeLine("220 Ready to start TLS");
		Mockito.verify(server).getSSLProtocol();
		Mockito.verify(client).startTLS("TLSv1.2", session);

		Mockito.verifyNoMoreInteractions(server, session, client);
	}

}