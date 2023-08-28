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

package net.markwalder.vtestmail;

import java.nio.charset.Charset;
import java.util.Locale;
import java.util.TimeZone;
import org.junit.jupiter.api.Test;

class BuildEnvironmentTest {

	@Test
	void test() {
		System.out.println("Build Environment");
		System.out.println("-----------------");
		System.out.println("Java    : " + System.getProperty("java.vendor") + " " + System.getProperty("java.version"));
		System.out.println("          " + System.getProperty("java.runtime.name") + " " + System.getProperty("java.runtime.version"));
		System.out.println("          " + System.getProperty("java.home"));
		System.out.println("System  : " + System.getProperty("os.name") + " " + System.getProperty("os.version") + " " + System.getProperty("os.arch"));
		System.out.println("Charset : " + Charset.defaultCharset());
		System.out.println("Locale  : " + Locale.getDefault());
		System.out.println("Timezone: " + TimeZone.getDefault().getID());
	}

}
