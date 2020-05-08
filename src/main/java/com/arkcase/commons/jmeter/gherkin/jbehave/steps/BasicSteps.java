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
package com.arkcase.commons.jmeter.gherkin.jbehave.steps;

import java.time.Duration;

import org.jbehave.core.annotations.Given;
import org.jbehave.core.annotations.Named;
import org.jbehave.core.annotations.Then;
import org.jbehave.core.annotations.When;

import com.arkcase.commons.jmeter.gherkin.Gherkin;

@Gherkin.Steps
public class BasicSteps {

	@When("sleeping for $s seconds")
	@Then("sleep for $s seconds")
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

}