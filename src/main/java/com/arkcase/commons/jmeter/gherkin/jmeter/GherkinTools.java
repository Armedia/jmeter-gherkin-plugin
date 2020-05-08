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
package com.arkcase.commons.jmeter.gherkin.jmeter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

import javax.script.Bindings;
import javax.script.ScriptException;

import org.apache.commons.lang3.StringUtils;
import org.apache.jmeter.threads.JMeterContextService;
import org.apache.jmeter.threads.JMeterVariables;
import org.codehaus.plexus.util.IOUtil;

import com.arkcase.commons.jmeter.tools.JSR223Script;

public class GherkinTools {

	private static final String[] NO_PARAMS = {};

	public static String getOverridableText(String sourceFile, Supplier<String> sourceText) throws IOException {
		String content = null;

		if (!StringUtils.isEmpty(sourceFile)) {
			File f = new File(sourceFile);
			try (FileReader r = new FileReader(f)) {
				content = IOUtil.toString(r);
			}
		} else if (sourceText != null) {
			content = sourceText.get();
		}

		return content;
	}

	public static String interpolateText(String content) throws ScriptException {
		return GherkinTools.interpolateText(content, null);
	}

	public static String interpolateText(String content, String[] parameters) throws ScriptException {
		if (StringUtils.isBlank(content)) { return content; }

		// Quote the backticks
		content = content.replaceAll("`", "\\`");

		// Wrap in backticks
		content = "`" + content + "`";

		// Interpolate using JEXL
		final JMeterVariables vars = JMeterContextService.getContext().getVariables();

		final JSR223Script script;
		try {
			script = new JSR223Script.Builder().withLanguage("jexl3").withSource(content).build();
		} catch (IOException e) {
			throw new UncheckedIOException(e);
		}

		Consumer<Bindings> c = (b) -> vars.getIterator().forEachRemaining((e) -> b.put(e.getKey(), e.getValue()));
		c = c.andThen((b) -> b.put("args", Optional.ofNullable(parameters).orElse(GherkinTools.NO_PARAMS)));
		try {
			Object result = script.execute(c);
			return (result != null ? result.toString() : null);
		} catch (IOException e) {
			throw new UncheckedIOException("Unexpected IOException reading from memory", e);
		}
	}

}
