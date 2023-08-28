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

package net.markwalder.vtestmail.pop3;

import org.mockito.Mockito;

abstract class CommandTest {

	protected final Pop3Server server = Mockito.mock(Pop3Server.class);
	protected final Pop3Session session = Mockito.mock(Pop3Session.class);
	protected final Pop3Client client = Mockito.mock(Pop3Client.class);

}
