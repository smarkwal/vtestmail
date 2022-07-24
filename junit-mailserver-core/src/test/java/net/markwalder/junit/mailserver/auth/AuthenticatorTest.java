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

import net.markwalder.junit.mailserver.Client;
import net.markwalder.junit.mailserver.MailboxStore;
import org.mockito.Mockito;

abstract class AuthenticatorTest {

	protected final Client client = Mockito.mock(Client.class);
	protected final MailboxStore store = Mockito.mock(MailboxStore.class);

}
