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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import net.markwalder.junit.mailserver.MailClient;
import net.markwalder.junit.mailserver.store.Mailbox;
import net.markwalder.junit.mailserver.MailboxProvider;
import net.markwalder.junit.mailserver.utils.DigestUtils;
import net.markwalder.junit.mailserver.utils.Hex;
import net.markwalder.junit.mailserver.utils.RandomStringUtils;

public class DigestMd5Authenticator implements Authenticator {

	private static final Charset CHARSET = StandardCharsets.ISO_8859_1;

	@Override
	public Credentials authenticate(String parameters, MailClient client, MailboxProvider store) throws IOException {

		// https://datatracker.ietf.org/doc/html/rfc2831

		if (parameters != null) {
			// DIGEST-MD5 does not accept parameters
			return null;
		}

		// step 1 --------------------------------------------------------------

		String realm = "localhost";
		String nonce = RandomStringUtils.randomAlphanumeric(16); // TODO: inject random number generator

		// send digest challenge to client
		String digestChallenge = generateDigestChallenge(realm, nonce);
		client.writeContinue(AuthUtils.encodeBase64(digestChallenge, CHARSET));

		// step 2 --------------------------------------------------------------

		// read digest response from client
		String digestResponse = AuthUtils.decodeBase64(client.readLine(), CHARSET);
		if (digestResponse == null) {
			return null;
		}

		// parse digest response
		Map<String, String> digestResponseMap = parseDigestResponse(digestResponse);

		// get username from response
		String username = digestResponseMap.get("username");
		if (username == null) {
			return null;
		}

		Mailbox mailbox = store.getMailbox(username);
		if (mailbox == null) {
			return null;
		}

		// check that realm is correct
		if (!realm.equals(digestResponseMap.get("realm"))) {
			return null;
		}

		// check that nonce is correct
		if (!nonce.equals(digestResponseMap.get("nonce"))) {
			return null;
		}

		String cnonce = digestResponseMap.get("cnonce");
		if (cnonce == null) {
			return null;
		}

		// check nonce count
		String nc = digestResponseMap.get("nc");
		if (!"00000001".equals(nc)) {
			return null;
		}

		String qop = digestResponseMap.get("qop");
		if (qop == null) {
			qop = "auth";
		}

		String digestUri = digestResponseMap.get("digest-uri");

		String response = digestResponseMap.get("response");
		if (response == null) {
			return null;
		}

		// TODO: use charset
		String charset = digestResponseMap.get("charset");
		if (charset == null) {
			charset = "ISO-8859-1";
		}

		String authzid = digestResponseMap.get("authzid");

		// get password from mailbox
		String password = mailbox.getSecret();

		String expectedResponse = calculateResponse(realm, username, password, Charset.forName(charset), nonce, nc, cnonce, qop, digestUri, authzid, true);
		if (!expectedResponse.equals(response)) {
			return null;
		}

		// step 3 --------------------------------------------------------------

		// create response value
		String responseValue = calculateResponse(realm, username, password, Charset.forName(charset), nonce, nc, cnonce, qop, digestUri, authzid, false);
		String responseAuth = "rspauth=" + responseValue;
		client.writeContinue(AuthUtils.encodeBase64(responseAuth, CHARSET));

		String line = client.readLine(); // TODO: must line be empty?
		if (line == null) {
			return null;
		}

		return new Credentials(username, password);
	}

	private String generateDigestChallenge(String realm, String nonce) {
		// TODO: support charset=utf-8
		return "realm=\"" + realm + "\",nonce=\"" + nonce + "\",qop=\"auth\",algorithm=md5-sess";
	}

	private static Map<String, String> parseDigestResponse(String digestResponse) {
		Map<String, String> map = new LinkedHashMap<>();

		// split response into key-value pairs
		String[] parts = digestResponse.split(",");
		for (String part : parts) {

			String[] pair = part.split("=", 2);
			String name = pair[0];
			String value = pair[1];

			// if value is quoted, remove quotes
			if (value.startsWith("\"") && value.endsWith("\"")) {
				value = value.substring(1, value.length() - 1);
			}

			map.put(name, value);
		}

		return map;
	}

	private static String calculateResponse(String realm, String username, String password, Charset charset, String nonce, String nc, String cnonce, String qop, String digestUri, String authzid, boolean isAuth) {

		ByteArrayOutputStream a1 = new ByteArrayOutputStream();
		a1.writeBytes(H(username + ":" + realm + ":" + password, charset));
		a1.writeBytes((":" + nonce + ":" + cnonce).getBytes(charset));
		if (authzid != null) {
			a1.writeBytes((":" + authzid).getBytes(charset));
		}
		byte[] A1 = a1.toByteArray();

		ByteArrayOutputStream a2 = new ByteArrayOutputStream();
		if (isAuth) {
			a2.writeBytes(("AUTHENTICATE:" + digestUri).getBytes(charset));
		} else {
			a2.writeBytes((":" + digestUri).getBytes(charset));
		}
		if (!qop.equals("auth")) {
			a2.writeBytes((":00000000000000000000000000000000").getBytes(charset));
		}
		byte[] A2 = a2.toByteArray();

		return HEX(KD(HEX(H(A1)), nonce + ":" + nc + ":" + cnonce + ":" + qop + ":" + HEX(H(A2))));
	}

	private static byte[] H(String data, Charset charset) {
		return H(data.getBytes(charset));
	}

	private static byte[] H(byte[] data) {
		return DigestUtils.md5(data);
	}

	private static byte[] KD(String k, String s) {
		return DigestUtils.md5(k + ":" + s, CHARSET);
	}

	private static String HEX(byte[] data) {
		return Hex.encode(data);
	}

}
