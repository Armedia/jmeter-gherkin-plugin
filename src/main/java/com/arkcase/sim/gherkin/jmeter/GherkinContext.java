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
package com.arkcase.sim.gherkin.jmeter;

import java.io.Closeable;
import java.util.Optional;

import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;

public class GherkinContext implements Closeable {

	private static final String PREFIX = "GherkinContext";
	private static final String GHERKIN_CONTEXT = GherkinContext.PREFIX + ".instance";

	public static GherkinContext get() {
		return GherkinContext.get(null);
	}

	public static GherkinContext get(JMeterContext ctx) {
		if (ctx == null) {
			ctx = JMeterContextService.getContext();
		}
		JMeterVariables vars = ctx.getVariables();
		GherkinContext gherkinContext = GherkinContext.class.cast(vars.getObject(GherkinContext.GHERKIN_CONTEXT));
		if (gherkinContext == null) {
			gherkinContext = new GherkinContext(ctx);
			vars.putObject(GherkinContext.GHERKIN_CONTEXT, gherkinContext);
		}
		return gherkinContext;
	}

	private final JMeterVariables vars;
	private final Environment env = new Environment(this::getVariable);

	private GherkinContext(JMeterContext ctx) {
		ctx = Optional.ofNullable(ctx).orElseGet(JMeterContextService::getContext);
		this.vars = ctx.getVariables();
	}

	public JMeterVariables getVars() {
		return this.vars;
	}

	private Object getVariable(String key) {
		return this.vars.getObject(key);
	}

	public Environment getEnv() {
		return this.env;
	}

	@Override
	public void close() {
		this.env.clear();
		this.vars.remove(GherkinContext.GHERKIN_CONTEXT);
	}
}
