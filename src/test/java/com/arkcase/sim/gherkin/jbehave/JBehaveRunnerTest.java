/*******************************************************************************
 * #%L
 * Armedia JMeter Gherkin Plugin
 * %%
 * Copyright (C) 2020 Armedia, LLC
 * %%
 * This file is part of the ArkCase software.
 *
 * If the software was purchased under a paid ArkCase license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 *
 * ArkCase is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * ArkCase is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with ArkCase. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.arkcase.sim.gherkin.jbehave;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.jbehave.core.io.LoadFromClasspath;
import org.junit.jupiter.api.Test;

import com.arkcase.sim.gherkin.jbehave.JBehaveRunner.Result;

public class JBehaveRunnerTest {
	private static final String TEST1 = "" + //
		"Feature: First Feature\n" + //
		"\n" + //
		"Scenario: First Scenario\n" + //
		"\n" + //
		"Given this is the first test\n" + //
		"When we want to debug the first test\n" + //
		"Then start debugging the first test\n" + //
		"\n" + //
		"" //
	;
	private static final String TEST2 = "" + //
		"Feature: Second Feature\n" + //
		"\n" + //
		"Scenario: Second Scenario\n" + //
		"\n" + //
		"Given this is the second test\n" + //
		"When we want to debug the second test\n" + //
		"Then start debugging the second test\n" + //
		"\n" + //
		"" //
	;

	@Test
	public void test() {

		String example = new LoadFromClasspath().loadStoryAsText("com/arkcase/sim/scripts/example.story");

		List<Pair<String, ?>> tests = Arrays.asList(Pair.of("Test1", JBehaveRunnerTest.TEST1), //
			Pair.of("Test2", JBehaveRunnerTest.TEST2), //
			Pair.of("Example", example) //
		);

		JBehaveRunner runner = new JBehaveRunner(Collections.singletonList(JBehaveRunner.class.getPackage().getName()));
		Stream<Result> results = runner.run(tests.stream());
		results.forEach((s) -> {
			String k = s.getStory().getName();
			Collection<Throwable> v = s.getFailures();
			if (!v.isEmpty()) {
				System.err.printf("%d error(s) caught on story [%s]%n", v.size(), k);
				System.err.printf("%s%n", s.getOutput());
			}
			v.forEach((t) -> t.printStackTrace(System.err));
		});
	}

}
