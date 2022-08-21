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

package net.markwalder.junit.mailserver.core;

import java.util.Objects;

public abstract class MailCommand {

	@Override
	public abstract String toString();

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MailCommand command = (MailCommand) obj;
		return Objects.equals(this.toString(), command.toString());
	}

	@Override
	public int hashCode() {
		return Objects.hash(toString());
	}

	@FunctionalInterface
	public interface Parser<T extends MailCommand, E extends MailException> {
		T parse(String command) throws E;
	}

}
