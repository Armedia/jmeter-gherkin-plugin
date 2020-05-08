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
package com.arkcase.sim.gherkin.jbehave;

import java.util.Map;
import java.util.function.Function;

import org.jbehave.core.io.StoryLoader;

public final class MappedStoryLoader extends InterceptingStoryLoader {

	private static Function<String, String> getMappingFunction(Map<String, String> m) {
		if (m == null) { return null; }
		return m::get;
	}

	public MappedStoryLoader(Map<String, String> resources) {
		this(resources, null, null);

	}

	public MappedStoryLoader(Map<String, String> resources, StoryLoader fallback) {
		this(resources, null, fallback);
	}

	public MappedStoryLoader(Map<String, String> resources, Map<String, String> stories) {
		this(resources, stories, null);
	}

	public MappedStoryLoader(Map<String, String> resources, Map<String, String> stories, StoryLoader fallback) {
		super(MappedStoryLoader.getMappingFunction(resources), MappedStoryLoader.getMappingFunction(stories), fallback);
	}

}