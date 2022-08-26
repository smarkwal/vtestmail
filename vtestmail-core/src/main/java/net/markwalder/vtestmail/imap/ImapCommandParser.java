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
import java.io.PushbackReader;
import java.io.StringReader;

class ImapCommandParser {

	private static final int CR = 0x0A;
	private static final int LF = 0x0D;
	private static final int SP = 0x20;
	private static final char DQUOTE = '"';

	private final PushbackReader reader;

	ImapCommandParser(String parameters) {
		reader = new PushbackReader(new StringReader(parameters));
	}

	String readMailbox() throws ImapException {
		// mailbox = "INBOX" / astring
		return readAString();
	}

	void assertNoMoreArguments() throws ImapException {
		int chr = read();
		if (!isEndOfStream(chr)) {
			throw ImapException.SyntaxError();
		}
	}

	void assertMoreArguments() throws ImapException {
		int chr = read();
		if (chr != SP) {
			throw ImapException.SyntaxError();
		}
	}

	// -------------------------------------------------------------------------

	private String readAString() throws ImapException {

		// astring = 1*ASTRING-CHAR / string
		// string  = quoted / literal

		int chr = read();
		if (chr == DQUOTE) {

			// quoted
			unread(chr);
			return readQuoted();

		} else if (chr == '{') {

			// literal
			unread(chr);
			return readLiteral();

		} else {

			// 1*ASTRING-CHAR
			unread(chr);
			return readAStringChars();

		}
	}

	private String readQuoted() throws ImapException {

		// quoted = DQUOTE *QUOTED-CHAR DQUOTE

		// read first DQUOTE
		int chr = read();
		if (chr != DQUOTE) {
			throw ImapException.SyntaxError();
		}

		StringBuilder buffer = new StringBuilder();
		while (true) {
			chr = read();

			if (isEndOfStream(chr)) {
				throw ImapException.SyntaxError();
			}

			if (chr == DQUOTE) {
				break; // end DQUOTE found
			}

			// QUOTED-CHAR = <any TEXT-CHAR except quoted-specials> / "\" quoted-specials / UTF8-2 / UTF8-3 / UTF8-4
			if (isTextChar(chr) && !isQuotedSpecials(chr)) {
				buffer.append((char) chr);
			} else if (chr == '\\') {
				chr = read();
				if (isQuotedSpecials(chr)) {
					buffer.append((char) chr);
				} else {
					throw ImapException.SyntaxError();
				}
			} else {
				// TODO: UTF8-2 / UTF8-3 / UTF8-4
				// UTF8-2 = %xC2-DF UTF8-tail
				// UTF8-3 = %xE0 %xA0-BF UTF8-tail / %xE1-EC 2( UTF8-tail ) / %xED %x80-9F UTF8-tail / %xEE-EF 2( UTF8-tail )
				// UTF8-4 = %xF0 %x90-BF 2( UTF8-tail ) / %xF1-F3 3( UTF8-tail ) / %xF4 %x80-8F 2( UTF8-tail )
				// UTF8-tail = %x80-BF
				buffer.append((char) chr);
			}
		}

		return buffer.toString();
	}

	private String readLiteral() throws ImapException {
		// TODO: support literal
		// literal = "{" number64 ["+"] "}" CRLF *CHAR8
		//		   ; <number64> represents the number of CHAR8s.
		//		   ; A non-synchronizing literal is distinguished
		//		   ; from a synchronizing literal by the presence of
		//		   ; "+" before the closing "}".
		//		   ; Non-synchronizing literals are not allowed when
		//		   ; sent from server to the client.
		//	number64 = 1*DIGIT ; Unsigned 63-bit integer ; (0 <= n <= 9,223,372,036,854,775,807)
		//	DIGIT =  %x30-39
		throw ImapException.SyntaxError();
	}

	private String readAStringChars() throws ImapException {

		// 1*ASTRING-CHAR

		StringBuilder buffer = new StringBuilder();
		while (true) {

			int chr = read();
			if (isEndOfStream(chr)) {
				break;
			}

			if (isAStringChar(chr)) {
				buffer.append((char) chr);
			} else {
				unread(chr);
				break;
			}
		}
		return buffer.toString();
	}

	private static boolean isEndOfStream(int chr) {
		return chr < 0;
	}

	private static boolean isAStringChar(int chr) {
		// ASTRING-CHAR = ATOM-CHAR / resp-specials
		return isAtomChar(chr) || isRespSpecials(chr);
	}

	private static boolean isAtomChar(int chr) {
		// ATOM-CHAR = <any CHAR except atom-specials>
		return isChar(chr) && !isAtomSpecial(chr);
	}

	private static boolean isTextChar(int chr) {
		// TEXT-CHAR = <any CHAR except CR and LF>
		return isChar(chr) && chr != CR && chr != LF;
	}

	private static boolean isChar(int chr) {
		// CHAR = %x01-7F ; any 7-bit US-ASCII character, excluding NUL
		return chr >= 0x01 && chr <= 0x7F;
	}

	private static boolean isAtomSpecial(int chr) {
		// atom-specials   = "(" / ")" / "{" / SP / CTL / list-wildcards / quoted-specials / resp-specials
		return chr == '(' || chr == ')' || chr == '{' || chr == SP || isCTL(chr) || isListWildcards(chr) || isQuotedSpecials(chr) || isRespSpecials(chr);
	}

	private static boolean isCTL(int chr) {
		// CTL =  %x00-1F / %x7F
		return chr >= 0x00 && chr <= 0x1F || chr == 0x7F;
	}

	private static boolean isListWildcards(int chr) {
		// list-wildcards = "%" / "*"
		return chr == '%' || chr == '*';
	}

	private static boolean isQuotedSpecials(int chr) {
		// quoted-specials = DQUOTE / "\"
		return chr == DQUOTE || chr == '\\';
	}

	private static boolean isRespSpecials(int chr) {
		// resp-specials = "]"
		return chr == ']';
	}

	// -------------------------------------------------------------------------

	private int read() throws ImapException {
		try {
			return reader.read();
		} catch (IOException e) {
			throw ImapException.SyntaxError();
		}
	}

	private void unread(int chr) throws ImapException {
		try {
			reader.unread(chr);
		} catch (IOException e) {
			throw ImapException.SyntaxError();
		}
	}

}
