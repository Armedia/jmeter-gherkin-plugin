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
package com.armedia.commons.jmeter.gherkin.jbehave;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

import org.jbehave.core.io.StoryLoader;

public class InterceptingStoryLoader implements StoryLoader {
	private final StoryLoader fallback;
	private final Collection<Function<String, String>> resourceFinders;
	private final Collection<Function<String, String>> storyFinders;

	public InterceptingStoryLoader(Function<String, String> resourceFinder) {
		this(resourceFinder, null, null);

	}

	public InterceptingStoryLoader(Function<String, String> resourceFinder, StoryLoader fallback) {
		this(resourceFinder, null, fallback);
	}

	public InterceptingStoryLoader(Function<String, String> resourceFinder, Function<String, String> storyFinder) {
		this(resourceFinder, storyFinder, null);
	}

	public InterceptingStoryLoader(Function<String, String> resourceFinder, Function<String, String> storyFinder,
		StoryLoader fallback) {
		this.fallback = fallback;
		List<Function<String, String>> l = null;
		l = new ArrayList<>(2);
		if (resourceFinder != null) {
			l.add(resourceFinder);
		}
		if (fallback != null) {
			l.add(fallback::loadResourceAsText);
		}
		l.removeIf(Objects::isNull);
		this.resourceFinders = Collections.unmodifiableCollection(l);

		l = new ArrayList<>(2);
		if (storyFinder != null) {
			l.add(storyFinder);
		}
		if (fallback != null) {
			l.add(fallback::loadStoryAsText);
		}
		l.removeIf(Objects::isNull);
		this.storyFinders = Collections.unmodifiableCollection(l);
	}

	public final StoryLoader getFallback() {
		return this.fallback;
	}

	private final String intercept(String key, Collection<Function<String, String>> candidates) {
		for (Function<String, String> f : candidates) {
			String result = f.apply(key);
			if (result != null) { return result; }
		}
		return null;
	}

	@Override
	public final String loadResourceAsText(String resourcePath) {
		return intercept(resourcePath, this.resourceFinders);
	}

	@Override
	public final String loadStoryAsText(String storyPath) {
		return intercept(storyPath, this.storyFinders);
	}

}
