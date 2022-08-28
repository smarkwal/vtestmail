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
import java.math.BigInteger;

class ImapCommandParser {

	private static final int CR = 0x0D;
	private static final int LF = 0x0A;
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

	public String readUserId() throws ImapException {
		// userid = astring
		return readAString();
	}

	public String readPassword() throws ImapException {
		// password = astring
		return readAString();
	}

	public String readSequenceSet() throws ImapException {

		// TODO: implement parsing of sequence set
		// sequence-set = (seq-number / seq-range) ["," sequence-set]
		//                       ; set of seq-number values, regardless of order.
		//                       ; Servers MAY coalesce overlaps and/or execute
		//                       ; the sequence in any order.
		//                       ; Example: a message sequence number set of
		//                       ; 2,4:7,9,12:* for a mailbox with 15 messages is
		//                       ; equivalent to 2,4,5,6,7,9,12,13,14,15
		//                       ; Example: a message sequence number set of
		//                       ; *:4,5:7 for a mailbox with 10 messages is
		//                       ; equivalent to 10,9,8,7,6,5,4,5,6,7 and MAY
		//                       ; be reordered and overlap coalesced to be
		//                       ; 4,5,6,7,8,9,10.
		// seq-number      = nz-number / "*"
		//                       ; message sequence number (COPY, FETCH, STORE
		//                       ; commands) or unique identifier (UID COPY,
		//                       ; UID FETCH, UID STORE commands).
		//                       ; * represents the largest number in use.  In
		//                       ; the case of message sequence numbers, it is
		//                       ; the number of messages in a non-empty mailbox.
		//                       ; In the case of unique identifiers, it is the
		//                       ; unique identifier of the last message in the
		//                       ; mailbox or, if the mailbox is empty, the
		//                       ; mailbox's current UIDNEXT value.
		//                       ; The server should respond with a tagged BAD
		//                       ; response to a command that uses a message
		//                       ; sequence number greater than the number of
		//                       ; messages in the selected mailbox.  This
		//                       ; includes "*" if the selected mailbox is empty.
		// seq-range       = seq-number ":" seq-number
		//                       ; two seq-number values and all values between
		//                       ; these two regardless of order.
		//                       ; Example: 2:4 and 4:2 are equivalent and
		//                       ; indicate values 2, 3, and 4.
		//                       ; Example: a unique identifier sequence range of
		//                       ; 3291:* includes the UID of the last message in
		//                       ; the mailbox, even if that value is less than
		//                       ; 3291.
		// nz-number = digit-nz *DIGIT
		//                       ; Non-zero unsigned 32-bit integer
		//                       ; (0 < n < 4,294,967,296)
		// digit-nz = %x31-39
		//                       ; 1-9
		// sequence-set =/ seq-last-command ; Allow for "result of the last command" indicator.
		// seq-last-command = "$"

		return readArgument();
	}

	public String[] readStoreAttFlags() throws ImapException {

		// TODO: implement parsing of store att flags
		// store-att-flags = (["+" / "-"] "FLAGS" [".SILENT"]) SP (flag-list / (flag *(SP flag)))
		// flag-list = "(" [flag *(SP flag)] ")"
		// flag = "\Answered" / "\Flagged" / "\Deleted" / "\Seen" / "\Draft" / flag-keyword / flag-extension ; Does not include "\Recent"
		// flag-keyword = "$MDNSent" / "$Forwarded" / "$Junk" / "$NotJunk" / "$Phishing" / atom
		// flag-extension = "\" atom
		// atom = 1*ATOM-CHAR

		String[] result = new String[2];
		result[0] = readArgument();
		assertMoreArguments();
		result[1] = readToEnd();
		return result;
	}

	// -------------------------------------------------------------------------

	private String readArgument() throws ImapException {
		StringBuilder buffer = new StringBuilder();
		while (true) {
			int chr = read();
			if (isEndOfStream(chr)) {
				break;
			} else if (chr == SP) {
				unread(chr);
				break;
			}
			buffer.append((char) chr);
		}
		return buffer.toString();
	}

	private String readToEnd() throws ImapException {
		StringBuilder buffer = new StringBuilder();
		while (true) {
			int chr = read();
			if (isEndOfStream(chr)) {
				break;
			}
			buffer.append((char) chr);
		}
		return buffer.toString();
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

		// literal = "{" number64 ["+"] "}" CRLF *CHAR8
		//		   ; <number64> represents the number of CHAR8s.
		//		   ; A non-synchronizing literal is distinguished
		//		   ; from a synchronizing literal by the presence of
		//		   ; "+" before the closing "}".
		//	number64 = 1*DIGIT ; Unsigned 63-bit integer ; (0 <= n <= 9,223,372,036,854,775,807)

		BigInteger number64 = readLiteralNumber();

		// check that number is between 0 and 9223372036854775807
		BigInteger minNumber = BigInteger.ZERO;
		BigInteger maxNumber = new BigInteger("9223372036854775807");
		if (number64.compareTo(minNumber) < 0) {
			throw ImapException.SyntaxError();
		} else if (number64.compareTo(maxNumber) > 0) {
			throw ImapException.SyntaxError();
		}

		// technical limitation:
		// maximum size of a literal is 2 GB
		BigInteger maxInteger = BigInteger.valueOf(Integer.MAX_VALUE);
		if (number64.compareTo(maxInteger) > 0) {
			throw ImapException.SyntaxError();  // TODO: use a different exception
		}
		int number = number64.intValue();

		// CRLF
		readCRFL();

		// *CHAR8
		return readChar8s(number);
	}

	private BigInteger readLiteralNumber() throws ImapException {

		int chr = read();
		if (chr != '{') {
			throw ImapException.SyntaxError();
		}

		StringBuilder buffer = new StringBuilder();

		while (true) {

			chr = read();
			if (isEndOfStream(chr)) {
				throw ImapException.SyntaxError();
			}

			if (chr == '}') {
				break;
			} else if (chr == '+') {
				chr = read();
				if (chr == '}') {
					break;
				} else {
					throw ImapException.SyntaxError();
				}
			}

			if (isDigit(chr)) {
				buffer.append((char) chr);
			} else {
				throw ImapException.SyntaxError();
			}
		}

		return new BigInteger(buffer.toString());
	}

	private void readCRFL() throws ImapException {
		if (read() != CR || read() != LF) {
			throw ImapException.SyntaxError();
		}
	}

	private String readChar8s(int number) throws ImapException {

		StringBuilder buffer = new StringBuilder();
		for (long i = 0; i < number; i++) {

			int chr = read();
			if (isEndOfStream(chr)) {
				throw ImapException.SyntaxError();
			}

			if (isChar8(chr)) {
				buffer.append((char) chr);
			} else {
				throw ImapException.SyntaxError();
			}
		}

		return buffer.toString();
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

	private boolean isChar8(int chr) {
		// CHAR8 = %x01-ff ; any OCTET except NUL, %x00
		return chr >= 0x01 && chr <= 0xFF;
	}

	private static boolean isDigit(int chr) {
		// DIGIT = %x30-39
		return chr >= '0' && chr <= '9';
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
