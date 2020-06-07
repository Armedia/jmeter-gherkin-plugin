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
package com.armedia.commons.jmeter.plugins.gherkin.config;

import java.io.IOException;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.stream.Stream;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.jmeter.config.ConfigTestElement;
import org.apache.jmeter.engine.event.LoopIterationEvent;
import org.apache.jmeter.engine.event.LoopIterationListener;
import org.apache.jmeter.testelement.ThreadListener;
import org.apache.jmeter.threads.JMeterContext;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.util.JMeterUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.jmeter.gherkin.Gherkin;
import com.armedia.commons.jmeter.gherkin.GherkinContext;
import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings.OutputFormat;
import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings.Syntax;
import com.armedia.commons.jmeter.tools.JSR223Script;
import com.armedia.commons.jmeter.tools.JSR223Script.CacheKey;

public class GherkinConfig extends ConfigTestElement implements LoopIterationListener, ThreadListener {
	private static final long serialVersionUID = 1L;

	private static final String PREFIX = "GherkinConfig";
	public static final String RESET_ON_LOOP = GherkinConfig.PREFIX + ".resetOnLoop";
	public static final String COMPILE_SCRIPT_IF_POSSIBLE = GherkinConfig.PREFIX + ".compileScriptIfPossible";
	public static final String DEFAULT_SCRIPT_LANGUAGE = "jexl3";
	public static final String SCRIPT_LANGUAGE = GherkinConfig.PREFIX + ".language";
	public static final String DRY_RUN = GherkinConfig.PREFIX + ".dryRun";
	public static final String FAIL_ON_PENDING = GherkinConfig.PREFIX + ".failOnPending";
	public static final String DEFAULT_SYNTAX = JBehaveEngine.DEFAULT_SYNTAX;
	public static final String SYNTAX = GherkinConfig.PREFIX + ".syntax";
	public static final String DEFAULT_OUTPUT_FORMAT = JBehaveEngine.DEFAULT_OUTPUT_FORMAT;
	public static final String OUTPUT_FORMAT = GherkinConfig.PREFIX + ".outputFormat";
	public static final String SCRIPT = GherkinConfig.PREFIX + ".script";
	public static final String COMPOSITES = GherkinConfig.PREFIX + ".composites";
	public static final String COMPOSITES_FILE = GherkinConfig.PREFIX + ".compositesFile";
	public static final String PACKAGES = GherkinConfig.PREFIX + ".packages";
	public static final String ENGINE = GherkinConfig.PREFIX + ".engine";

	public static final Set<String> OUTPUT_FORMATS;
	static {
		Set<String> s = new LinkedHashSet<>();
		Stream.of(OutputFormat.values()).map(Enum::name).forEach(s::add);
		OUTPUT_FORMATS = Collections.unmodifiableSet(s);
	}

	public static final Set<String> SYNTAXES;
	static {
		Set<String> s = new LinkedHashSet<>();
		Stream.of(Syntax.values()).map(Enum::name).forEach(s::add);
		SYNTAXES = Collections.unmodifiableSet(s);
	}

	public static class Script {
		private final String language;
		private final String script;
		private final LazyInitializer<String> hash = new LazyInitializer<String>() {
			@Override
			protected String initialize() {
				return Script.this.language + "#{" + DigestUtils.sha256Hex(Script.this.script) + "}";
			}
		};

		public Script(String language, String script) {
			this.language = language;
			this.script = script;
		}

		public String getLanguage() {
			return this.language;
		}

		public String getScript() {
			return this.script;
		}

		public String getHash() {
			try {
				return this.hash.get();
			} catch (ConcurrentException e) {
				throw new RuntimeException("Should not have failed to compute the hash", e);
			}
		}
	}

	private final ThreadLocal<GherkinEngine> engines = new ThreadLocal<>();

	public static Gherkin getGherkin(JMeterContext ctx) {
		return Gherkin.class.cast(ctx.getVariables().getObject(GherkinConfig.ENGINE));
	}

	protected final Logger log = LoggerFactory.getLogger(getClass());

	@Override
	public void iterationStart(LoopIterationEvent e) {
		// Don't do this on the first iteration to avoid waste
		if (isResetOnLoop() && (e.getIteration() > 1)) {
			GherkinContext.get().close();
		}
		getThreadContext().getVariables().putObject(GherkinConfig.ENGINE, this.engines.get());
	}

	private GherkinEngine newEngine() {
		GherkinEngine engine = createEngine();
		try {
			engine.init(this);
			configureEngine(engine);
		} catch (Exception e) {
			throw new RuntimeException("Failed to initialize the given engine" + engine, e);
		}
		this.engines.set(engine);
		return engine;
	}

	protected Bindings populateBindings(Bindings bindings) {
		final String label = getName();
		bindings.put("Label", label);
		bindings.put("log", LoggerFactory.getLogger(getClass().getCanonicalName() + "." + label));

		JMeterContext ctx = JMeterContextService.getContext();
		bindings.put("ctx", ctx);
		bindings.put("vars", ctx.getVariables());
		bindings.put("props", JMeterUtils.getJMeterProperties());
		bindings.put("OUT", System.out);
		bindings.put("ERR", System.err);

		bindings.put("gherkin", GherkinContext.get(ctx));
		return bindings;
	}

	private void configureEngine(GherkinEngine engine) {
		final Script script = getScript();
		try {
			// Execute the configuration script!
			Object ret = new JSR223Script //
				.Builder() //
					.withLanguage(script.getLanguage()) //
					.withSource(script.getScript()) //
					.build() //
					.execute(this::populateBindings) //
			;
			this.log.debug("JSR223Script returned the value {}", ret);
		} catch (IOException | ScriptException e) {
			this.log.error("Problem in JSR223 script {}, message: {}", getName(), e, e);
		}
	}

	@Override
	public void threadStarted() {
		newEngine();
	}

	@Override
	public void threadFinished() {
		closeEngine();
		Script script = getScript();
		CacheKey cacheKey;
		try {
			cacheKey = JSR223Script.getCacheKey(script.getLanguage(), script.getScript());
			JSR223Script.purge(cacheKey);
		} catch (ScriptException e) {
			this.log.warn("ScriptException trying to clean out the cached script", e);
		}
	}

	protected String currentThreadName() {
		return Thread.currentThread().getName();
	}

	public final Gherkin getRunner() {
		return getEngine();
	}

	protected GherkinEngine createEngine() {
		GherkinEngine engine = getEngine();
		if (engine != null) {
			this.log.warn("Thread: " + currentThreadName() + " already has a GherkinEngine instance(" + getEngine()
				+ ") associated with it. Each ThreadGroup can only contain a single GherkinConfig.");
			return engine;
		}

		@SuppressWarnings("resource")
		JBehaveEngine jbe = new JBehaveEngine() //
			// TODO: Allow configuration of which engine to instantiate, and how to configure it
			.setOutputFormat(getOutputFormat()) //
			.setDryRun(isDryRun()) //
			.setFailOnPending(isFailOnPending()) //
		;
		engine = jbe;
		return engine;
	}

	protected void closeEngine() {
		GherkinEngine engine = getEngine();
		if (engine == null) { return; }
		try {
			engine.close();
		} catch (Exception e) {
			this.log.warn("Attempting to quit browser instance that has already exited.");
		} finally {
			this.engines.remove();
		}
	}

	protected GherkinEngine getEngine() {
		return this.engines.get();
	}

	public boolean isResetOnLoop() {
		return getPropertyAsBoolean(GherkinConfig.RESET_ON_LOOP);
	}

	public void setResetOnLoop(boolean reset) {
		setProperty(GherkinConfig.RESET_ON_LOOP, reset);
	}

	public boolean isCompileIfPossible() {
		return getPropertyAsBoolean(GherkinConfig.COMPILE_SCRIPT_IF_POSSIBLE);
	}

	public void setCompileIfPossible(boolean recreate) {
		setProperty(GherkinConfig.COMPILE_SCRIPT_IF_POSSIBLE, recreate);
	}

	public boolean isDryRun() {
		return getPropertyAsBoolean(GherkinConfig.DRY_RUN);
	}

	public void setDryRun(boolean dryRun) {
		setProperty(GherkinConfig.DRY_RUN, dryRun);
	}

	public boolean isFailOnPending() {
		return getPropertyAsBoolean(GherkinConfig.FAIL_ON_PENDING);
	}

	public void setFailOnPending(boolean failOnPending) {
		setProperty(GherkinConfig.FAIL_ON_PENDING, failOnPending);
	}

	public String getOutputFormat() {
		return getPropertyAsString(GherkinConfig.OUTPUT_FORMAT, GherkinConfig.DEFAULT_OUTPUT_FORMAT);
	}

	public void setOutputFormat(String outputFormat) {
		setProperty(GherkinConfig.OUTPUT_FORMAT, outputFormat);
	}

	public String getSyntax() {
		return getPropertyAsString(GherkinConfig.SYNTAX, GherkinConfig.DEFAULT_SYNTAX);
	}

	public void setSyntax(String syntax) {
		setProperty(GherkinConfig.SYNTAX, syntax);
	}

	public String getComposites() {
		return getPropertyAsString(GherkinConfig.COMPOSITES);
	}

	public void setComposites(String composites) {
		setProperty(GherkinConfig.COMPOSITES, composites);
	}

	public String getCompositesFile() {
		return getPropertyAsString(GherkinConfig.COMPOSITES_FILE);
	}

	public void setCompositesFile(String composites) {
		setProperty(GherkinConfig.COMPOSITES_FILE, composites);
	}

	public String getPackages() {
		return getPropertyAsString(GherkinConfig.PACKAGES);
	}

	public void setPackages(String packages) {
		setProperty(GherkinConfig.PACKAGES, packages);
	}

	public Script getScript() {
		String script = getPropertyAsString(GherkinConfig.SCRIPT);
		String language = getPropertyAsString(GherkinConfig.SCRIPT_LANGUAGE);
		return new Script(language, script);
	}

	public void setScript(String language, String script) {
		setScript(new Script(language, script));
	}

	public void setScript(Script script) {
		setProperty(GherkinConfig.SCRIPT_LANGUAGE, script.getLanguage());
		setProperty(GherkinConfig.SCRIPT, script.getScript());
	}
}
