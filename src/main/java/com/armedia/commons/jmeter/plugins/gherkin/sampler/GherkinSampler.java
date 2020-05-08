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
package com.armedia.commons.jmeter.plugins.gherkin.sampler;

import java.io.IOException;
import java.nio.charset.Charset;

import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.samplers.AbstractSampler;
import org.apache.jmeter.samplers.Entry;
import org.apache.jmeter.samplers.SampleResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.armedia.commons.jmeter.gherkin.Gherkin;
import com.armedia.commons.jmeter.gherkin.GherkinTools;
import com.armedia.commons.jmeter.plugins.gherkin.config.GherkinConfig;

import kg.apc.jmeter.JMeterPluginsUtils;

/**
 * A Sampler that makes HTTP requests using a real browser (via. Selenium/WebDriver). It currently
 * provides a scripting mechanism via. Javascript to control the browser instance.
 */
public class GherkinSampler extends AbstractSampler {
	private static final long serialVersionUID = 1L;

	private static final String[] NO_PARAMS = {};
	private static final String PREFIX = "GherkinSampler";
	public static final String PARAMETERS = GherkinSampler.PREFIX + ".parameters";
	public static final String STORY = GherkinSampler.PREFIX + ".story";
	public static final String STORY_FILE = GherkinSampler.PREFIX + ".storyFile";

	private final Logger log = LoggerFactory.getLogger(GherkinSampler.class);

	public static final String DEFAULT_STORY = //
		"" + //
			"Feature: Sample Feature\n" + //
			"\n" + //
			"Scenario: Sample Scenario\n" + //
			"\n" + //
			"Given some prior condition\n" + //
			"When executing some action\n" + //
			"Then check for some consequence\n" + //
			"" //
	;

	@Override
	public SampleResult sample(Entry entry) {
		Gherkin runner = getRunner();
		if (runner == null) {
			throw new IllegalArgumentException(
				"The Gherkin Engine has not been configured.  Please ensure at least one GherkinConfig is created for this ThreadGroup.");
		}

		SampleResult sampleResult = new SampleResult();
		sampleResult.setSampleLabel(getName());
		sampleResult.setSamplerData(toString());
		sampleResult.setDataType(SampleResult.TEXT);

		// TODO: Set this as per the output format
		sampleResult.setContentType("text/xml");

		sampleResult.setDataEncoding("UTF-8");
		sampleResult.setSuccessful(false);

		this.log.debug("Current thread name: '" + getThreadName() + "', has runner: '" + runner + "'");

		Gherkin.Result<?> gherkinResult = null;
		try {
			final String story = getStory();
			sampleResult.setSamplerData(story);
			final boolean successful;
			if (StringUtils.isNotBlank(story)) {
				sampleResult.sampleStart();
				try {
					gherkinResult = runner.runStory(getName(), getStory());
				} finally {
					sampleResult.sampleEnd();
				}
				// TODO: Re-enable prettyprinting
				sampleResult.setResponseData(gherkinResult.getOutput(), Charset.defaultCharset().name());
				successful = gherkinResult.getFailures().isEmpty();
			} else {
				successful = false;
			}
			sampleResult.setSuccessful(successful);
			if (successful) {
				sampleResult.setResponseCode("200");
				sampleResult.setResponseMessageOK();
			} else {
				sampleResult.setResponseCode("500");
				sampleResult.setResponseMessage("Story execution failed");
			}
		} catch (Exception e) {
			sampleResult.setSamplerData(getRawStory());
			sampleResult.setResponseCode("500");
			if (this.log.isDebugEnabled()) {
				this.log.error("Exception caught processing a Gherkin story", e);
			} else {
				this.log.error(e.getMessage());
			}
			sampleResult.setResponseMessage(e.getMessage());
			sampleResult.setResponseData((e.toString() + "\r\n" + JMeterPluginsUtils.getStackTrace(e)),
				Charset.defaultCharset().name());
		}

		return sampleResult;
	}

	private String getRawStory() {
		return getPropertyAsString(GherkinSampler.STORY, GherkinSampler.DEFAULT_STORY);
	}

	private String getStoryFile() {
		return getPropertyAsString(GherkinSampler.STORY_FILE);
	}

	private String[] getParameters() {
		String params = getPropertyAsString(GherkinSampler.PARAMETERS);
		if (StringUtils.isBlank(params)) { return GherkinSampler.NO_PARAMS; }
		return params.split("\\s");
	}

	private String getStory() throws ScriptException, IOException {
		String story = GherkinTools.getOverridableText(getStoryFile(), this::getRawStory);
		return GherkinTools.interpolateText(story, getParameters());
	}

	public void setStory(String script) {
		setProperty(GherkinSampler.STORY, script);
	}

	private Gherkin getRunner() {
		return GherkinConfig.getGherkin(getThreadContext());
	}
}
