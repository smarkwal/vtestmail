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

package net.markwalder.vtestmail.pop3;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import net.markwalder.vtestmail.store.MailboxMessage;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

class RETRTest extends CommandTest {

	private final MailboxMessage message = Mockito.mock(MailboxMessage.class);

	@Test
	void parse() throws Pop3Exception {

		// test
		RETR command = RETR.parse("1");

		// assert
		assertThat(command).hasToString("RETR 1");
	}

	@Test
	void parse_throwsException_ifNoParameters() {

		// test
		Exception exception = assertThrows(Pop3Exception.class, () -> RETR.parse(null));

		// assert
		assertThat(exception).hasMessage("Syntax error");
	}

	@Test
	void parse_throwsException_ifParameterIsNotANumber() {

		// test
		Exception exception = assertThrows(Pop3Exception.class, () -> RETR.parse("first"));

		// assert
		assertThat(exception).hasMessage("Syntax error");
	}

	@Test
	void parse_throwsException_ifParameterIsNegativeNumber() {

		// test
		Exception exception = assertThrows(Pop3Exception.class, () -> RETR.parse("-13"));

		// assert
		assertThat(exception).hasMessage("Syntax error");
	}

	@Test
	void parse_throwsException_ifParameterNumberWithPlusSign() {

		// test
		Exception exception = assertThrows(Pop3Exception.class, () -> RETR.parse("+13"));

		// assert
		assertThat(exception).hasMessage("Syntax error");
	}

	@Test
	void testToString() {

		// prepare
		Pop3Command command = new RETR(2);

		// test
		String result = command.toString();

		// assert
		assertThat(result).isEqualTo("RETR 2");

	}

	@Test
	void execute() throws Pop3Exception, IOException {

		// mock
		Mockito.doReturn(message).when(session).getMessage(1);
		Mockito.doReturn(false).when(message).isDeleted();
		Mockito.doReturn(false).when(session).isDeleted(1);
		Mockito.doReturn(20).when(message).getSize();
		Mockito.doReturn("Subject: Test\r\n\r\nThis is a test message.").when(message).getContent();

		// prepare
		Pop3Command command = new RETR(1);

		// test
		command.execute(server, session, client);

		// verify
		Mockito.verify(session).assertState(State.TRANSACTION);
		Mockito.verify(session).getMessage(1);
		Mockito.verify(message).isDeleted();
		Mockito.verify(session).isDeleted(1);
		Mockito.verify(message).getSize();
		Mockito.verify(message).getContent();
		Mockito.verify(client).writeLine("+OK 20 octets");
		Mockito.verify(client).writeMultiLines("Subject: Test\r\n\r\nThis is a test message.");

		Mockito.verifyNoMoreInteractions(server, session, client, message);
	}

	@Test
	void execute_throwsException_ifMessageIsNotFound() throws Pop3Exception {

		// mock
		Mockito.doReturn(null).when(session).getMessage(2);

		// prepare
		Pop3Command command = new RETR(2);

		// test and assert
		Exception exception = assertThrows(Pop3Exception.class, () -> command.execute(server, session, client));
		assertThat(exception).hasMessage("No such message");

		// verify
		Mockito.verify(session).assertState(State.TRANSACTION);
		Mockito.verify(session).getMessage(2);

		Mockito.verifyNoMoreInteractions(server, session, client, message);
	}

	@Test
	void execute_throwsException_ifMessageIsDeleted_inSession() throws Pop3Exception {

		// mock
		Mockito.doReturn(message).when(session).getMessage(3);
		Mockito.doReturn(false).when(message).isDeleted();
		Mockito.doReturn(true).when(session).isDeleted(3);

		// prepare
		Pop3Command command = new RETR(3);

		// test and assert
		Exception exception = assertThrows(Pop3Exception.class, () -> command.execute(server, session, client));
		assertThat(exception).hasMessage("No such message");

		// verify
		Mockito.verify(session).assertState(State.TRANSACTION);
		Mockito.verify(session).getMessage(3);
		Mockito.verify(message).isDeleted();
		Mockito.verify(session).isDeleted(3);

		Mockito.verifyNoMoreInteractions(server, session, client, message);
	}

	@Test
	void execute_throwsException_ifMessageIsDeleted_inMailbox() throws Pop3Exception {

		// mock
		Mockito.doReturn(message).when(session).getMessage(3);
		Mockito.doReturn(true).when(message).isDeleted();
		Mockito.doReturn(false).when(session).isDeleted(3);

		// prepare
		Pop3Command command = new RETR(3);

		// test and assert
		Exception exception = assertThrows(Pop3Exception.class, () -> command.execute(server, session, client));
		assertThat(exception).hasMessage("No such message");

		// verify
		Mockito.verify(session).assertState(State.TRANSACTION);
		Mockito.verify(session).getMessage(3);
		Mockito.verify(message).isDeleted();

		Mockito.verifyNoMoreInteractions(server, session, client, message);
	}

}
