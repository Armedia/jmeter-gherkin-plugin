/*******************************************************************************
 * #%L
 * Armedia JMeter Gherkin Plugin
 * %%
 * Copyright (C) 2020 Armedia, LLC
 * %%
 * This file is part of the Armedia JMeter Gherkin Plugin software.
 * 
 * If the software was purchased under a paid Armedia JMeter Gherkin Plugin
 * license, the terms of the paid license agreement will prevail.  Otherwise,
 * the software is provided under the following open source license terms:
 * 
 * Armedia JMeter Gherkin Plugin is free software: you can redistribute it
 * and/or modify it under the terms of the GNU Lesser General Public License
 * as published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 * 
 * Armedia JMeter Gherkin Plugin is distributed in the hope that it will be
 * useful, but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU Lesser
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Armedia JMeter Gherkin Plugin. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.commons.jmeter.gherkin;

import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

import org.jbehave.core.failures.BatchFailures;

public interface Gherkin {

	/**
	 * <p>
	 * Mark a type as a step provider.
	 * </p>
	 */
	@Retention(RUNTIME)
	@Inherited
	@Target(TYPE)
	public @interface Steps {
		// Disabled for now, opens a big can'o worms
		// boolean enabled() default true;
	}

	public static abstract class Result<STORY> {

		protected final STORY story;
		protected final String output;
		protected final Collection<Throwable> failures;

		public Result(STORY story, String output, BatchFailures failures) {
			this.story = story;
			this.output = output;
			this.failures = Collections.unmodifiableCollection(new ArrayList<>(failures.values()));
		}

		public abstract String getSource();

		public final STORY getStory() {
			return this.story;
		}

		public final String getOutput() {
			return this.output;
		}

		public final Collection<Throwable> getFailures() {
			return this.failures;
		}
	}

	public Result<?> runStory(String name, String story) throws Exception;

}
