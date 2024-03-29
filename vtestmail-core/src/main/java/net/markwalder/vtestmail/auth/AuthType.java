/*
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

package net.markwalder.vtestmail.auth;

public final class AuthType {

	private AuthType() {
		// constants only
	}

	public static final String LOGIN = "LOGIN";
	public static final String PLAIN = "PLAIN";
	public static final String CRAM_MD5 = "CRAM-MD5";
	public static final String DIGEST_MD5 = "DIGEST-MD5";
	public static final String XOAUTH2 = "XOAUTH2";

}
