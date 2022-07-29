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

package net.markwalder.junit.mailserver;

import java.util.Objects;

public abstract class MailCommand {

	protected final String line;

	public MailCommand(String line) {
		this.line = line;
	}

	public String getLine() {
		return line;
	}

	@Override
	public String toString() {
		return line;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MailCommand command = (MailCommand) obj;
		return line.equals(command.line);
	}

	@Override
	public int hashCode() {
		return Objects.hash(line);
	}

}
