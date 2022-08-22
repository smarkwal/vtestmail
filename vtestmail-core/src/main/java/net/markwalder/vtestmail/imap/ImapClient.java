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

package net.markwalder.vtestmail.imap;

import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import net.markwalder.vtestmail.core.MailClient;

public class ImapClient extends MailClient {

	protected ImapClient(Socket socket, StringBuilder log) throws IOException {
		// TODO: why is the charset not UTF-8 as mentioned in IMAP RFC 9051?
		super(socket, StandardCharsets.ISO_8859_1, "+", log);
	}

}