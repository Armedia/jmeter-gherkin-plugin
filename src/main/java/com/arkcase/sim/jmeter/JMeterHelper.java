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
package com.arkcase.sim.jmeter;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.UUID;
import java.util.function.BiConsumer;
import java.util.function.Function;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterVariables;
import org.openqa.selenium.WebDriver;

import com.arkcase.sim.components.AngularHelper;
import com.arkcase.sim.components.WebDriverHelper;
import com.arkcase.sim.components.html.ButtonHelper;
import com.arkcase.sim.components.html.CheckboxHelper;
import com.arkcase.sim.components.html.DropDownHelper;
import com.arkcase.sim.components.html.ElementHelper;
import com.arkcase.sim.components.html.PageHelper;
import com.arkcase.sim.components.html.TextBoxHelper;
import com.arkcase.sim.components.html.WaitHelper;
import com.arkcase.sim.gherkin.jbehave.JBehaveRunner;

public class JMeterHelper {

	private static final String KEY = UUID.randomUUID().toString();
	private static final String WEB_DRIVER_KEY = "Browser";

	static {
		JBehaveRunner.init();
	}

	private static final Collection<Pair<String, Function<WebDriver, Object>>> INITIALIZERS = Collections
		.unmodifiableList(Arrays.asList( //
			Pair.of("AngularHelper", AngularHelper::new), //
			Pair.of("WebDriverHelper", WebDriverHelper::new), //
			Pair.of("ButtonHelper", ButtonHelper::new), //
			Pair.of("CheckboxHelper", CheckboxHelper::new), //
			Pair.of("DropDownHelper", DropDownHelper::new), //
			Pair.of("ElementHelper", ElementHelper::new), //
			Pair.of("PageHelper", PageHelper::new), //
			Pair.of("TextBoxHelper", TextBoxHelper::new), //
			Pair.of("WaitHelper", WaitHelper::new), //
			// For easier syntax
			null //
		));

	protected static void constructNew(WebDriver webDriver, BiConsumer<String, Object> consumer) {
		if (consumer == null) { return; }
		if (webDriver == null) { return; }
		for (Pair<String, Function<WebDriver, Object>> p : JMeterHelper.INITIALIZERS) {
			if (p != null) {
				String key = p.getKey();
				Object value = p.getValue().apply(webDriver);
				if ((key != null) && (value != null)) {
					consumer.accept(key, value);
				}
			}
		}
	}

	protected static WebDriver getWebDriver(JMeterContext context) {
		return JMeterHelper.getWebDriver(context.getVariables());
	}

	protected static WebDriver getWebDriver(JMeterVariables vars) {
		Object browserObj = vars.getObject(JMeterHelper.WEB_DRIVER_KEY);
		if (WebDriver.class.isInstance(browserObj)) { return WebDriver.class.cast(browserObj); }
		if (browserObj != null) {
			throw new ClassCastException(
				String.format("The object stored with the WebDriver's key (%s) is not an instance of WebDriver (%s)",
					JMeterHelper.WEB_DRIVER_KEY, browserObj.getClass().getCanonicalName()));
		}
		return null;
	}

	public static boolean init(JMeterContext context) {
		return JMeterHelper.init(context.getVariables());
	}

	public static boolean init(JMeterVariables vars) {
		WebDriver browser = Objects.requireNonNull(JMeterHelper.getWebDriver(vars), "No WebDriver could be found");
		Object existing = vars.getObject(JMeterHelper.KEY);
		boolean ret = false;
		if (existing == null) {
			vars.putObject(JMeterHelper.KEY, System.nanoTime());
			JMeterHelper.constructNew(browser, vars::putObject);
			ret = true;
		}
		return ret;
	}

	public static boolean isInitialized(JMeterContext context) {
		return JMeterHelper.isInitialized(context.getVariables());
	}

	public static boolean isInitialized(JMeterVariables vars) {
		return (vars.getObject(JMeterHelper.KEY) != null);
	}
}
