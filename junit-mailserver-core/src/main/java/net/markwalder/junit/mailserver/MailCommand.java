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

	protected final String name;
	protected final String parameters;

	public MailCommand(String parameters) {
		this.name = this.getClass().getSimpleName();
		this.parameters = parameters;
	}

	public String getName() {
		return name;
	}

	public String getParameters() {
		return parameters;
	}

	@Override
	public String toString() {
		if (parameters == null) {
			return name;
		} else {
			return name + " " + parameters;
		}
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) return true;
		if (obj == null || getClass() != obj.getClass()) return false;
		MailCommand command = (MailCommand) obj;
		return name.equals(command.name) && Objects.equals(parameters, command.parameters);
	}

	@Override
	public int hashCode() {
		return Objects.hash(name, parameters);
	}

}
