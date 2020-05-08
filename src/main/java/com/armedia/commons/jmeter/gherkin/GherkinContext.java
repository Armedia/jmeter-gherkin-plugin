/*******************************************************************************
 * #%L
 * Armedia JMeter Gherkin Plugin
 * %%
 * Copyright (C) 2020 Armedia, LLC
 * %%
 * This file is part of the Armedia JMeter Gherkin Plugin software.
 * 
 * If the software was purchased under a paid Armedia JMeter Gherkin Plugin license, the terms of
 * the paid license agreement will prevail.  Otherwise, the software is
 * provided under the following open source license terms:
 * 
 * Armedia JMeter Gherkin Plugin is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 * 
 * Armedia JMeter Gherkin Plugin is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Lesser General Public License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with Armedia JMeter Gherkin Plugin. If not, see <http://www.gnu.org/licenses/>.
 * #L%
 *******************************************************************************/
package com.armedia.commons.jmeter.gherkin;

import java.io.Closeable;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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

	public static final class Variables {

		private final JMeterVariables vars;

		private Variables(JMeterVariables vars) {
			this.vars = Objects.requireNonNull(vars);
		}

		public String getThreadName() {
			return this.vars.getThreadName();
		}

		public int getIteration() {
			return this.vars.getIteration();
		}

		public void incIteration() {
			this.vars.incIteration();
		}

		public Object remove(String key) {
			return this.vars.remove(key);
		}

		public void put(String key, String value) {
			this.vars.put(key, value);
		}

		public void putObject(String key, Object value) {
			this.vars.putObject(key, value);
		}

		public void putAll(Map<String, ?> vars) {
			this.vars.putAll(vars);
		}

		public void putAll(JMeterVariables vars) {
			this.vars.putAll(vars);
		}

		public String get(String key) {
			return this.vars.get(key);
		}

		public Object getObject(String key) {
			return this.vars.getObject(key);
		}

		public Iterator<Entry<String, Object>> getIterator() {
			return this.vars.getIterator();
		}

		public Set<Entry<String, Object>> entrySet() {
			return this.vars.entrySet();
		}

		public boolean isSameUserOnNextIteration() {
			return this.vars.isSameUserOnNextIteration();
		}
	}

	private final Variables vars;
	private final Environment env = new Environment(this::getVariable);

	private GherkinContext(JMeterContext ctx) {
		ctx = Optional.ofNullable(ctx).orElseGet(JMeterContextService::getContext);
		this.vars = new Variables(ctx.getVariables());
	}

	public Variables getVars() {
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
