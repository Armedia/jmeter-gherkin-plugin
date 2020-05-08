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
package com.arkcase.commons.jmeter.gherkin.jbehave;

import java.io.PrintStream;
import java.io.Serializable;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.failures.FailingUponPendingStep;
import org.jbehave.core.i18n.LocalizedKeywords;
import org.jbehave.core.parsers.RegexStoryParser;
import org.jbehave.core.parsers.StoryParser;
import org.jbehave.core.parsers.gherkin.GherkinStoryParser;
import org.jbehave.core.reporters.HtmlOutput;
import org.jbehave.core.reporters.JsonOutput;
import org.jbehave.core.reporters.PrintStreamOutput;
import org.jbehave.core.reporters.StoryReporterBuilder;
import org.jbehave.core.reporters.TxtOutput;
import org.jbehave.core.reporters.XmlOutput;

public class JBehaveSettings implements Serializable, Cloneable {
	private static final long serialVersionUID = 1L;

	public static enum Syntax {
		//
		JBEHAVE(new RegexStoryParser()), //
		GHERKIN(new GherkinStoryParser()), //
		//
		;

		public final StoryParser parser;

		private Syntax(StoryParser parser) {
			this.parser = Objects.requireNonNull(parser, "Must provide a StoryParser instance");
		}
	}

	public static enum OutputFormat {
		//
		JSON("application/json") {
			@Override
			protected PrintStreamOutput newReporter(PrintStream out) {
				return new JsonOutput(out, new LocalizedKeywords());
			}
		},
		XML("application/xml") {
			@Override
			protected PrintStreamOutput newReporter(PrintStream out) {
				return new XmlOutput(out, new LocalizedKeywords());
			}
		},
		HTML("text/html") {
			@Override
			protected PrintStreamOutput newReporter(PrintStream out) {
				return new HtmlOutput(out, new LocalizedKeywords());
			}
		},
		TEXT("text/plain") {
			@Override
			protected PrintStreamOutput newReporter(PrintStream out) {
				return new TxtOutput(out, new LocalizedKeywords());
			}
		},
		NULL("application/octet-stream") {
			@Override
			protected PrintStreamOutput newReporter(PrintStream out) {
				return null;
			}

			@Override
			protected boolean ignore() {
				return true;
			}
		}
		//
		;

		public final String contentType;

		private OutputFormat(String contentType) {
			this.contentType = Objects.requireNonNull(contentType, "Must provide a non-null content type");
		}

		protected abstract PrintStreamOutput newReporter(PrintStream out);

		protected boolean ignore() {
			return false;
		}

		public final Configuration setReportBuilder(Configuration cfg, PrintStream out) {
			if (cfg == null) { return null; }
			if (!ignore()) {
				cfg.useStoryReporterBuilder(new ReportBuilder(out, this::newReporter));
			}
			return cfg;
		}
	}

	private static final class ReportBuilder extends StoryReporterBuilder {
		private final PrintStream out;
		private final Function<PrintStream, PrintStreamOutput> builder;

		private ReportBuilder(PrintStream out, Function<PrintStream, PrintStreamOutput> builder) {
			this.out = out;
			this.builder = builder;
		}

		@Override
		public PrintStreamOutput build(String name) {
			return this.builder.apply(this.out) //
				.doCompressFailureTrace(false) //
				.doReportFailureTrace(true) //
			;
		}
	}

	protected static final boolean DEFAULT_DRY_RUN = false;
	protected static final boolean DEFAULT_FAIL_ON_PENDING = false;
	protected static final Syntax DEFAULT_SYNTAX = Syntax.GHERKIN;
	protected static final OutputFormat DEFAULT_OUTPUT_FORMAT = OutputFormat.XML;

	private static final LazyInitializer<JBehaveSettings> DEFAULTS = new LazyInitializer<JBehaveSettings>() {

		@Override
		protected JBehaveSettings initialize() {
			return new JBehaveSettings() {
				private static final long serialVersionUID = 1L;

				private JBehaveSettings fail() {
					throw new UnsupportedOperationException("Can't modify this JBehaveSettings instance");
				}

				@Override
				public JBehaveSettings setDryRun(boolean dryRun) {
					return fail();
				}

				@Override
				public JBehaveSettings setFailOnPending(boolean failOnPending) {
					return fail();
				}

				@Override
				public JBehaveSettings setSyntax(Syntax syntax) {
					return fail();
				}

				@Override
				public JBehaveSettings setOutputFormat(OutputFormat outputFormat) {
					return fail();
				}

			};
		}
	};

	public static JBehaveSettings defaults() {
		try {
			return JBehaveSettings.DEFAULTS.get();
		} catch (ConcurrentException e) {
			throw new RuntimeException("Failed to initialize the default settings", e);
		}
	}

	private boolean dryRun = JBehaveSettings.DEFAULT_DRY_RUN;
	private boolean failOnPending = JBehaveSettings.DEFAULT_FAIL_ON_PENDING;
	private Syntax syntax = JBehaveSettings.DEFAULT_SYNTAX;
	private OutputFormat outputFormat = JBehaveSettings.DEFAULT_OUTPUT_FORMAT;

	static JBehaveSettings safe(JBehaveSettings settings) {
		return (settings != null ? settings : JBehaveSettings.defaults());
	}

	public JBehaveSettings() {
	}

	public JBehaveSettings(JBehaveSettings other) {
		copyFrom(other);
	}

	public JBehaveSettings copyFrom(JBehaveSettings other) {
		if (other == null) {
			other = JBehaveSettings.defaults();
		}
		this.dryRun = other.dryRun;
		this.failOnPending = other.failOnPending;
		this.syntax = other.syntax;
		this.outputFormat = other.outputFormat;
		return this;
	}

	public JBehaveSettings setDefaults() {
		return copyFrom(null);
	}

	public boolean isDryRun() {
		return this.dryRun;
	}

	public JBehaveSettings setDryRun(boolean dryRun) {
		this.dryRun = dryRun;
		return this;
	}

	public boolean isFailOnPending() {
		return this.failOnPending;
	}

	public JBehaveSettings setFailOnPending(boolean failOnPending) {
		this.failOnPending = failOnPending;
		return this;
	}

	public Syntax getSyntax() {
		return this.syntax;
	}

	public JBehaveSettings setSyntax(Syntax syntax) {
		this.syntax = Optional.ofNullable(syntax).orElse(JBehaveSettings.DEFAULT_SYNTAX);
		return this;
	}

	public OutputFormat getOutputFormat() {
		return this.outputFormat;
	}

	public JBehaveSettings setOutputFormat(OutputFormat outputFormat) {
		this.outputFormat = Optional.ofNullable(outputFormat).orElse(JBehaveSettings.DEFAULT_OUTPUT_FORMAT);
		return this;
	}

	Configuration apply(Configuration configuration, PrintStream out) {
		configuration = configuration.doDryRun(this.dryRun);
		if (this.failOnPending) {
			configuration = configuration //
				.usePendingStepStrategy(new FailingUponPendingStep());
		}
		return this.outputFormat.setReportBuilder(configuration, out);
	}

	@Override
	public int hashCode() {
		return Objects.hash(this.dryRun, this.failOnPending, this.outputFormat, this.syntax);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) { return true; }
		if (obj == null) { return false; }
		if (getClass() != obj.getClass()) { return false; }
		JBehaveSettings other = JBehaveSettings.class.cast(obj);
		if (this.dryRun != other.dryRun) { return false; }
		if (this.failOnPending != other.failOnPending) { return false; }
		if (this.outputFormat != other.outputFormat) { return false; }
		if (this.syntax != other.syntax) { return false; }
		return true;
	}

	@Override
	public String toString() {
		return String.format("JBehaveSettings [dryRun=%s, failOnPending=%s, syntax=%s, outputFormat=%s]", this.dryRun,
			this.failOnPending, this.syntax, this.outputFormat);
	}
}