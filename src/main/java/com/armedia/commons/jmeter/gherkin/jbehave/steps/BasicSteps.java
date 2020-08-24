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
package com.armedia.commons.jmeter.gherkin.jbehave.steps;

import java.time.Duration;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.engine.StandardJMeterEngine;
import org.jbehave.core.annotations.Alias;
import org.jbehave.core.annotations.Aliases;
import org.jbehave.core.annotations.BeforeScenario;
import org.jbehave.core.annotations.BeforeStories;
import org.jbehave.core.annotations.BeforeStory;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.armedia.commons.jmeter.gherkin.Gherkin;

@Gherkin.Steps
public class BasicSteps {

	@BeforeStory
	@BeforeStories
	public void beforeStory() {
		// TODO: Check to see if our interruption mode is story-level
	}

	@BeforeScenario
	public void beforeScenario() {
		// TODO: Check to see if our interruption mode is on scenario
	}

	@Then("sleep for $s seconds")
	@Aliases(values = {
		"sleep for 1 second", //
		"sleep for a second", //
		"sleep for $s secs", //
		"sleep for 1 sec", //
		"sleep for a sec", //
	})
	public void sleep(@Named("s") long seconds) throws InterruptedException {
		if (seconds > 0) {
			Thread.sleep(Duration.ofSeconds(seconds).toMillis());
		}
	}

	@Given("debug")
	@When("debugging")
	@Then("debug")
	public void debug() {
		// This exists solely to put an easy-access breakpoint for debuggers
		Thread.currentThread().hashCode();
	}

	@Then("fail")
	public void raiseException() {
		raiseException(null);
	}

	@Then("fail with the message [$message]")
	public void raiseException(@Named("message") String message) {
		throw (StringUtils.isBlank(message) ? new RuntimeException() : new RuntimeException(message));
	}

	@Then("stop the test")
	public void stopTest() {
		StandardJMeterEngine.stopEngine();
	}

	@Then("stop the test now")
	@Alias("stop the test immediately")
	public void stopTestNow() {
		StandardJMeterEngine.stopEngineNow();
	}

}