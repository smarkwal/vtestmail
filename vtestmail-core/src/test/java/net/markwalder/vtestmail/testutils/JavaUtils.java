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

package net.markwalder.vtestmail.testutils;

import net.markwalder.vtestmail.utils.StringUtils;

public class JavaUtils {

	public static int getJavaVersion() {
		String version = System.getProperty("java.version");
		String[] digits = StringUtils.split(version, ".");
		if (version.startsWith("1.")) { // Java 1.1 - 1.8
			return Integer.parseInt(digits[1]);
		} else { // Java 9 and greater
			return Integer.parseInt(digits[0]);
		}
	}

}
