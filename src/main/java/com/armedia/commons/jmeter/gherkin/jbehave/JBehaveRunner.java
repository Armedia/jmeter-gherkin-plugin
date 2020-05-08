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
package com.armedia.commons.jmeter.gherkin.jbehave;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.net.URL;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;

import org.apache.commons.lang3.tuple.Pair;
import org.codehaus.plexus.util.IOUtil;
import org.jbehave.core.annotations.Given;
import org.jbehave.core.configuration.Configuration;
import org.jbehave.core.configuration.MostUsefulConfiguration;
import org.jbehave.core.configuration.UnmodifiableConfiguration;
import org.jbehave.core.embedder.MetaFilter;
import org.jbehave.core.embedder.PerformableTree;
import org.jbehave.core.embedder.PrintStreamEmbedderMonitor;
import org.jbehave.core.failures.BatchFailures;
import org.jbehave.core.io.LoadFromClasspath;
import org.jbehave.core.io.StoryLoader;
import org.jbehave.core.model.ExamplesTableFactory;
import org.jbehave.core.model.Story;
import org.jbehave.core.model.TableTransformers;
import org.jbehave.core.steps.AbstractStepsFactory;
import org.jbehave.core.steps.ParameterConverters;
import org.reflections.Reflections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.jmeter.gherkin.Gherkin;
import com.armedia.commons.jmeter.gherkin.jbehave.JBehaveSettings.Syntax;

public class JBehaveRunner {
	private static final Logger LOG = LoggerFactory.getLogger(JBehaveRunner.class);

	public static final class Result extends Gherkin.Result<Story> {

		private Result(Story story, String output, BatchFailures failures) {
			super(story, output, failures);
		}

		@Override
		public String getSource() {
			// TODO: do better!
			return this.story.toString();
		}
	}

	private static final StoryLoader LOADER = new LoadFromClasspath(JBehaveRunner.class);
	private static final ExamplesTableFactory EXAMPLES_TABLE_FACTORY = new ExamplesTableFactory(JBehaveRunner.LOADER,
		new TableTransformers());
	private static final ParameterConverters PARAMETER_CONVERTERS = new ParameterConverters().addConverters( //
		new ParameterConverters.BooleanConverter(), //
		new ParameterConverters.BooleanListConverter(), //
		new ParameterConverters.CurrencyConverter(), //
		new ParameterConverters.NumberConverter(), //
		new ParameterConverters.NumberListConverter(), //
		new ParameterConverters.EnumConverter(), //
		new ParameterConverters.EnumListConverter(), //
		new ParameterConverters.FluentEnumConverter(), //
		new ParameterConverters.DateConverter(), //
		new ParameterConverters.StringConverter(), //
		new ParameterConverters.StringListConverter(), //
		new ParameterConverters.ExamplesTableConverter(JBehaveRunner.EXAMPLES_TABLE_FACTORY), //
		new ParameterConverters.ExamplesTableParametersConverter(JBehaveRunner.EXAMPLES_TABLE_FACTORY), //
		new ParameterConverters.FileConverter(), //
		// new ParameterConverters.FunctionalParameterConverter(), //
		new ParameterConverters.JsonConverter(), //
		// new ParameterConverters.MethodReturningConverter(), //
		new ParameterConverters.PatternConverter(), //
		new ParameterConverters.VerbatimConverter() //
	);

	private static final class StepsFactory extends AbstractStepsFactory {

		private static final Package ANNOTATION_PACKAGE = Given.class.getPackage();

		private final Map<Class<?>, Constructor<?>> constructors;

		private static Constructor<?> getConstructor(Class<?> c) {
			// Skip interfaces
			if (Modifier.isInterface(c.getModifiers())) {
				JBehaveRunner.LOG.debug("Class {} is an interface, skipping it", c.getCanonicalName());
				return null;
			}
			// Skip abstract classes
			if (Modifier.isAbstract(c.getModifiers())) {
				JBehaveRunner.LOG.debug("Class {} is abstract, skipping it", c.getCanonicalName());
				return null;
			}

			// Does it have any JBehave annotations?
			boolean include = false;
			for (Method m : c.getMethods()) {
				for (Annotation a : m.getAnnotations()) {
					Class<? extends Annotation> type = a.annotationType();
					if (Objects.equals(StepsFactory.ANNOTATION_PACKAGE, type.getPackage())) {
						include = true;
						break;
					}
				}
			}

			if (!include) {
				JBehaveRunner.LOG.debug("Class {} doesn't have any JBehave method annotations", c.getCanonicalName());
				return null;
			}

			// Is it a constructors object?
			try {
				return c.getDeclaredConstructor();
			} catch (NoSuchMethodException e) {
				JBehaveRunner.LOG.warn("No default constructor found for {}, can't use its steps",
					c.getCanonicalName());
				return null;
			} catch (Throwable t) {
				JBehaveRunner.LOG.warn("Failed to access the default constructor for {}", c.getCanonicalName(), t);
				return null;
			}
		}

		private StepsFactory(Collection<String> prefixes, Configuration configuration) {
			super(configuration);
			Map<Class<?>, Constructor<?>> constructors = new HashMap<>();
			Set<String> finalPrefixes = new LinkedHashSet<>();
			// Add this always, for now...
			finalPrefixes.add(JBehaveRunner.class.getPackage().getName());
			finalPrefixes.addAll(prefixes);

			for (Class<?> klazz : new Reflections(finalPrefixes).getTypesAnnotatedWith(Gherkin.Steps.class)) {
				Constructor<?> constructor = StepsFactory.getConstructor(klazz);
				if (constructor != null) {
					constructors.put(klazz, constructor);
				}
			}

			this.constructors = Collections.unmodifiableMap(constructors);
		}

		@Override
		public Object createInstanceOfType(Class<?> klazz) {
			Constructor<?> c = this.constructors
				.get(Objects.requireNonNull(klazz, "Must provide a class for the new instance"));
			if (c == null) {
				throw new IllegalArgumentException(
					"Class " + klazz.getCanonicalName() + " is not registered as a step provider");
			}
			try {
				return c.newInstance();
			} catch (Throwable t) {
				throw new RuntimeException("Failed to create an instance of " + klazz.getCanonicalName(), t);
			}
		}

		@Override
		protected List<Class<?>> stepsTypes() {
			return new ArrayList<>(this.constructors.keySet());
		}
	}

	public static Story parse(String name, Object source) throws Exception {
		return JBehaveRunner.parse(JBehaveSettings.defaults().getSyntax(), name, source);
	}

	public static Story parse(Syntax syntax, String name, Object source) throws Exception {
		Objects.requireNonNull(syntax, "Must provide a syntax to parse with");
		Objects.requireNonNull(source, "Must provide a story to parse");
		if (Story.class.isInstance(source)) { return Story.class.cast(source); }
		if (InputStream.class.isInstance(source)) {
			source = IOUtil.toString(InputStream.class.cast(source));
		}
		if (Reader.class.isInstance(source)) {
			source = IOUtil.toString(Reader.class.cast(source));
		}
		if (URL.class.isInstance(source)) {
			try (InputStream in = URL.class.cast(source).openStream()) {
				source = IOUtil.toString(in);
			}
		}
		return syntax.parser.parseStory(String.valueOf(source), name);
	}

	public static void init() {
		// This will cause everything to be initialized
	}

	private final Configuration configuration;
	private final StepsFactory stepsFactory;
	private final Map<String, String> composites;

	public JBehaveRunner(Collection<String> searchScopes) {
		this(searchScopes, null);
	}

	public JBehaveRunner(Collection<String> searchScopes, Map<String, String> composites) {
		if ((composites != null) && !composites.isEmpty()) {
			this.composites = Collections.unmodifiableMap(new LinkedHashMap<>(composites));
		} else {
			this.composites = Collections.emptyMap();
		}
		this.configuration = new UnmodifiableConfiguration( //
			new MostUsefulConfiguration() //
				.useCompositePaths(this.composites.keySet()) //
				.useStoryLoader(new MappedStoryLoader(composites, composites, JBehaveRunner.LOADER)) //
				.useParameterConverters(JBehaveRunner.PARAMETER_CONVERTERS) //
		);

		// By using this class, we ensure that we can share state between steps used within a
		// story such that they don't interfere across stories (new instances where applicable)
		this.stepsFactory = new StepsFactory(searchScopes, this.configuration);
	}

	public Result run(Story story) {
		return run(story, null);
	}

	private Result run(Story story, JBehaveSettings settings) {
		Objects.requireNonNull(story, "Must provide a non-null Story to run");
		settings = new JBehaveSettings(settings); // Ensure we have sane values
		final Charset charset = StandardCharsets.UTF_8;
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
			BatchFailures failures = new BatchFailures();
			try (final PrintStream out = new PrintStream(baos, false, charset.name())) {
				Configuration configuration = new MostUsefulConfiguration() //
					.useParameterConverters(this.configuration.parameterConverters()) //
				//
				;
				configuration = settings.apply(configuration, out);

				PerformableTree tree = new PerformableTree();
				PerformableTree.RunContext ctx = tree.newRunContext(configuration,
					this.stepsFactory.createCandidateSteps(), new PrintStreamEmbedderMonitor(), new MetaFilter(),
					failures);
				List<Story> stories = Collections.singletonList(story);
				tree.addStories(ctx, stories);
				for (Story s : stories) {
					tree.perform(ctx, s);
				}
				out.flush();
			}
			baos.flush();
			return new Result(story, baos.toString(charset.name()), failures);
		} catch (IOException e) {
			throw new UncheckedIOException("Unexpected IOException writing to memory", e);
		}
	}

	public Result run(String name, Object source) throws Exception {
		return run(name, source, null);
	}

	public Result run(String name, Object source, JBehaveSettings settings) throws Exception {
		settings = JBehaveSettings.safe(settings);
		return run(JBehaveRunner.parse(settings.getSyntax(), name, source), settings);
	}

	public Collection<Result> run(Map<String, ?> stories) throws Exception {
		return run(stories, null);
	}

	public Collection<Result> run(Map<String, ?> stories, JBehaveSettings settings) throws Exception {
		Collection<Story> s = new ArrayList<>(stories.size());
		for (String k : stories.keySet()) {
			Object v = stories.get(k);
			s.add(JBehaveRunner.parse(JBehaveSettings.safe(settings).getSyntax(), k, v));
		}
		return runStories(s, settings);
	}

	public Stream<Result> run(Stream<Pair<String, ?>> stories) {
		return run(stories, null);
	}

	public Stream<Result> run(Stream<Pair<String, ?>> stories, JBehaveSettings settings) {
		return stories.filter(Objects::nonNull).map((p) -> {
			try {
				return run(p.getKey(), p.getValue(), settings);
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
		});
	}

	public Collection<Result> runStories(Collection<Story> stories) {
		return runStories(stories, null);
	}

	public Collection<Result> runStories(Collection<Story> stories, JBehaveSettings settings) {
		final Collection<Result> results = new ArrayList<>(stories.size());
		stories.forEach((s) -> results.add(run(s, settings)));
		return results;
	}

	public Stream<Result> runStories(Stream<Story> stories) {
		return runStories(stories, null);
	}

	public Stream<Result> runStories(Stream<Story> stories, JBehaveSettings settings) {
		return stories.filter(Objects::nonNull).map((s) -> run(s, settings));
	}
}
