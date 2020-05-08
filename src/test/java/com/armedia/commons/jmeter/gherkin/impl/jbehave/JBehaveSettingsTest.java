/*-
 * #%L
 * Armedia ArkCase JMeter Selenium Helpers
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
 */
package com.armedia.commons.jmeter.gherkin.impl.jbehave;

import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings;
import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings.OutputFormat;
import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings.Syntax;

class JBehaveSettingsTest {

	@BeforeAll
	static void setUpBeforeClass() throws Exception {
	}

	@AfterAll
	static void tearDownAfterClass() throws Exception {
	}

	@Test
	void testSafe() {
		JBehaveSettings settings = null;
		Assertions.assertNotNull(JBehaveSettings.safe(null));
		settings = new JBehaveSettings();
		Assertions.assertNotNull(JBehaveSettings.safe(settings));
		Assertions.assertSame(settings, JBehaveSettings.safe(settings));
	}

	@Test
	void testJBehaveSettings() {
		JBehaveSettings a = new JBehaveSettings();
		Assertions.assertEquals(JBehaveSettings.DEFAULT_DRY_RUN, a.isDryRun());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_FAIL_ON_PENDING, a.isFailOnPending());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, a.getOutputFormat());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, a.getSyntax());

		a = new JBehaveSettings(null);
		Assertions.assertEquals(JBehaveSettings.DEFAULT_DRY_RUN, a.isDryRun());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_FAIL_ON_PENDING, a.isFailOnPending());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, a.getOutputFormat());
		Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, a.getSyntax());

		boolean[] dryRun = {
			false, true
		};
		boolean[] failOnPending = {
			false, true
		};
		OutputFormat[] outputFormat = OutputFormat.values();
		Syntax[] syntax = Syntax.values();

		for (boolean d : dryRun) {
			a.setDryRun(d);
			for (boolean f : failOnPending) {
				a.setFailOnPending(f);
				for (OutputFormat of : outputFormat) {
					a.setOutputFormat(of);
					for (Syntax s : syntax) {
						a.setSyntax(s);

						JBehaveSettings b = new JBehaveSettings(a);
						Assertions.assertEquals(a.isDryRun(), b.isDryRun());
						Assertions.assertEquals(a.isFailOnPending(), b.isFailOnPending());
						Assertions.assertEquals(a.getOutputFormat(), b.getOutputFormat());
						Assertions.assertEquals(a.getSyntax(), b.getSyntax());
					}
				}
			}
		}
	}

	@Test
	void testCopyFrom() {
		JBehaveSettings a = new JBehaveSettings();
		JBehaveSettings b = new JBehaveSettings();

		boolean[] dryRun = {
			false, true
		};
		boolean[] failOnPending = {
			false, true
		};
		OutputFormat[] outputFormat = OutputFormat.values();
		Syntax[] syntax = Syntax.values();

		for (boolean d : dryRun) {
			a.setDryRun(d);
			for (boolean f : failOnPending) {
				a.setFailOnPending(f);
				for (OutputFormat of : outputFormat) {
					a.setOutputFormat(of);
					for (Syntax s : syntax) {
						a.setSyntax(s);

						b.setDefaults();
						Assertions.assertEquals(JBehaveSettings.DEFAULT_DRY_RUN, b.isDryRun());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_FAIL_ON_PENDING, b.isFailOnPending());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, b.getOutputFormat());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, b.getSyntax());

						b.copyFrom(a);
						Assertions.assertEquals(a.isDryRun(), b.isDryRun());
						Assertions.assertEquals(a.isFailOnPending(), b.isFailOnPending());
						Assertions.assertEquals(a.getOutputFormat(), b.getOutputFormat());
						Assertions.assertEquals(a.getSyntax(), b.getSyntax());
					}
				}
			}
		}
	}

	@Test
	void testSetDefaults() {
		JBehaveSettings a = new JBehaveSettings();

		boolean[] dryRun = {
			false, true
		};
		boolean[] failOnPending = {
			false, true
		};
		OutputFormat[] outputFormat = OutputFormat.values();
		Syntax[] syntax = Syntax.values();

		for (boolean d : dryRun) {
			a.setDryRun(d);
			for (boolean f : failOnPending) {
				a.setFailOnPending(f);
				for (OutputFormat of : outputFormat) {
					a.setOutputFormat(of);
					for (Syntax s : syntax) {
						a.setSyntax(s);

						JBehaveSettings b = new JBehaveSettings(a);

						Assertions.assertEquals(d, b.isDryRun());
						Assertions.assertEquals(f, b.isFailOnPending());
						Assertions.assertEquals(of, b.getOutputFormat());
						Assertions.assertEquals(s, b.getSyntax());

						b.setDefaults();
						Assertions.assertEquals(JBehaveSettings.DEFAULT_DRY_RUN, b.isDryRun());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_FAIL_ON_PENDING, b.isFailOnPending());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, b.getOutputFormat());
						Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, b.getSyntax());
					}
				}
			}
		}
	}

	@Test
	void testDryRun() {
		JBehaveSettings a = new JBehaveSettings();

		boolean[] dryRun = {
			false, true
		};

		Assertions.assertEquals(JBehaveSettings.DEFAULT_DRY_RUN, a.isDryRun());

		for (boolean d : dryRun) {
			Assertions.assertSame(a, a.setDryRun(d));
			Assertions.assertEquals(d, a.isDryRun());
		}
	}

	@Test
	void testFailOnPending() {
		JBehaveSettings a = new JBehaveSettings();

		boolean[] failOnPending = {
			false, true
		};

		Assertions.assertEquals(JBehaveSettings.DEFAULT_FAIL_ON_PENDING, a.isFailOnPending());

		for (boolean f : failOnPending) {
			Assertions.assertSame(a, a.setFailOnPending(f));
			Assertions.assertEquals(f, a.isFailOnPending());
		}
	}

	@Test
	void testSyntax() {
		JBehaveSettings a = new JBehaveSettings();

		Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, a.getSyntax());

		for (Syntax s : Syntax.values()) {
			Assertions.assertSame(a, a.setSyntax(s));
			Assertions.assertEquals(s, a.getSyntax());
		}

		Assertions.assertSame(a, a.setSyntax(null));
		Assertions.assertEquals(JBehaveSettings.DEFAULT_SYNTAX, a.getSyntax());
	}

	@Test
	void testOutputFormat() {
		JBehaveSettings a = new JBehaveSettings();

		Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, a.getOutputFormat());

		for (OutputFormat of : OutputFormat.values()) {
			Assertions.assertSame(a, a.setOutputFormat(of));
			Assertions.assertEquals(of, a.getOutputFormat());
		}

		Assertions.assertSame(a, a.setOutputFormat(null));
		Assertions.assertEquals(JBehaveSettings.DEFAULT_OUTPUT_FORMAT, a.getOutputFormat());
	}

	@Test
	void testHashCodeEquals() {
		boolean[] dryRun = {
			false, true
		};
		boolean[] failOnPending = {
			false, true
		};
		OutputFormat[] outputFormat = OutputFormat.values();
		Syntax[] syntax = Syntax.values();

		List<JBehaveSettings> A = new ArrayList<>();
		List<JBehaveSettings> B = new ArrayList<>();

		for (boolean d : dryRun) {
			for (boolean f : failOnPending) {
				for (OutputFormat of : outputFormat) {
					for (Syntax s : syntax) {
						JBehaveSettings a = new JBehaveSettings();
						a.setDryRun(d);
						a.setFailOnPending(f);
						a.setSyntax(s);
						a.setOutputFormat(of);
						A.add(a);
						B.add(new JBehaveSettings(a));
					}
				}
			}
		}

		for (int a = 0; a < A.size(); a++) {
			JBehaveSettings sA = A.get(a);
			Assertions.assertFalse(sA.equals(null));
			Assertions.assertFalse(sA.equals(this));

			for (int b = 0; b < B.size(); b++) {
				JBehaveSettings sB = B.get(b);
				if (a == b) {
					Assertions.assertEquals(sA, sB);
					Assertions.assertEquals(sA.hashCode(), sB.hashCode());
				} else {
					Assertions.assertNotEquals(sA, sB);
					Assertions.assertNotEquals(sA.hashCode(), sB.hashCode());
				}

			}
		}
	}

	@Test
	void testApply() {
	}

}
