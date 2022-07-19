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
import org.mockito.Mock;
import org.mockito.Mockito;

class RSETTest extends CommandTest {

	@Test
	void execute() throws ProtocolException, IOException {

		// prepare
		Command command = new RSET();

		// test
		command.execute("RSET", server, client);

		// verify
		Mockito.verify(server).reset();
		Mockito.verify(client).writeLine("250 OK");

		Mockito.verifyNoMoreInteractions(server);
		Mockito.verifyNoMoreInteractions(client);
	}

}