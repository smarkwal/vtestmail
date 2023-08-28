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

import net.markwalder.vtestmail.core.MailClient;
import net.markwalder.vtestmail.store.MailboxProvider;
import org.mockito.Mockito;

abstract class AuthenticatorTest {

	protected final MailClient client = Mockito.mock(MailClient.class);
	protected final MailboxProvider store = Mockito.mock(MailboxProvider.class);

}
