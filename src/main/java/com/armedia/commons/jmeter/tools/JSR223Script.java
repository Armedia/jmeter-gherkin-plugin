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
package com.armedia.commons.jmeter.tools;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Serializable;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import javax.script.Bindings;
import javax.script.Compilable;
import javax.script.CompiledScript;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineFactory;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.concurrent.ConcurrentException;
import org.apache.commons.lang3.concurrent.ConcurrentInitializer;
import org.apache.commons.lang3.concurrent.ConcurrentUtils;
import org.apache.commons.lang3.concurrent.LazyInitializer;
import org.apache.commons.lang3.tuple.Pair;

public final class JSR223Script {

	public static final Map<String, String> LANGUAGES;
	private static final Map<String, ScriptEngineFactory> SCRIPT_ENGINE_FACTORIES;
	static {
		Map<String, String> languages = new LinkedHashMap<>();
		Map<String, ScriptEngineFactory> factories = new TreeMap<>();
		for (ScriptEngineFactory factory : new ScriptEngineManager().getEngineFactories()) {
			for (String name : factory.getNames()) {
				factories.putIfAbsent(StringUtils.lowerCase(name), factory);
			}
		}
		factories.forEach((n, f) -> languages.put(n, JSR223Script.describeLanguage(f)));
		SCRIPT_ENGINE_FACTORIES = Collections.unmodifiableMap(new LinkedHashMap<>(factories));
		LANGUAGES = Collections.unmodifiableMap(languages);
	}

	private static String describeLanguage(ScriptEngineFactory factory) {
		return factory.getLanguageName() + " " + factory.getLanguageVersion() //
			+ " / " //
			+ factory.getEngineName() + " " + factory.getEngineVersion() //
		;
	}

	private static final ConcurrentMap<CacheKey, JSR223Script> CACHE = new ConcurrentHashMap<>();

	public static class Builder {

		private String language = null;
		private Object source = null;
		private Charset charset = null;
		private boolean allowCompilation = true;

		public Builder withLanguage(String language) {
			this.language = language;
			return this;
		}

		public Builder withAllowCompilation(boolean allowCompilation) {
			this.allowCompilation = allowCompilation;
			return this;
		}

		public Builder withSource(String script) {
			this.source = script;
			return this;
		}

		public Builder withSource(Reader source) {
			this.source = source;
			return this;
		}

		public Builder withSource(InputStream source, Charset charset) {
			this.source = source;
			this.charset = JSR223Script.sanitize(charset);
			return this;
		}

		public Builder withSource(Path source) {
			this.source = source;
			return this;
		}

		public Builder withSource(File source) {
			this.source = source;
			return this;
		}

		public JSR223Script build() throws ScriptException, IOException {
			this.language = JSR223Script.sanitize(this.language);
			Objects.requireNonNull(this.source, "Must provide a non-null source for the script");
			if (String.class.isInstance(this.source)) {
				return JSR223Script.getInstance(this.allowCompilation, this.language, this.source.toString());
			}
			if (InputStream.class.isInstance(this.source)) {
				this.charset = JSR223Script.sanitize(this.charset);
				this.source = new InputStreamReader(InputStream.class.cast(this.source), this.charset);
			}
			if (Reader.class.isInstance(this.source)) {
				return JSR223Script.getInstance(this.allowCompilation, this.language, Reader.class.cast(this.source));
			}

			this.charset = JSR223Script.sanitize(this.charset);
			if (File.class.isInstance(this.source)) {
				this.source = File.class.cast(this.source).toPath();
			}
			return JSR223Script.getInstance(this.allowCompilation, this.language, Path.class.cast(this.source),
				this.charset);
		}
	}

	public static class CacheKey implements Serializable {
		private static final long serialVersionUID = 1L;

		private final String hash;

		private CacheKey(String hash) {
			this.hash = hash;
		}

		public String getHash() {
			return this.hash;
		}

		@Override
		public int hashCode() {
			return Objects.hash(this.hash);
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj) { return true; }
			if (obj == null) { return false; }
			if (getClass() != obj.getClass()) { return false; }
			CacheKey other = CacheKey.class.cast(obj);
			if (!StringUtils.equalsIgnoreCase(this.hash, other.hash)) { return false; }
			return true;
		}

		@Override
		public String toString() {
			return "CacheKey[" + this.hash + "]";
		}
	}

	private static ScriptEngineFactory getFactory(String language) {
		language = StringUtils.lowerCase(language);
		if (StringUtils.isBlank(language)) { return null; }
		return JSR223Script.SCRIPT_ENGINE_FACTORIES.get(language);
	}

	private static Charset sanitize(Charset charset) {
		return (charset != null ? charset : Charset.defaultCharset());
	}

	private static String sanitize(String language) throws ScriptException {
		language = StringUtils.lowerCase(language);
		if ((language == null) || !JSR223Script.SCRIPT_ENGINE_FACTORIES.containsKey(language)) {
			throw new ScriptException("Unsupported script language [" + language + "]");
		}
		return language;
	}

	private static Pair<CacheKey, String> computeKey(String language, Object source)
		throws ScriptException, IOException {
		language = JSR223Script.sanitize(language);
		if (String.class.isInstance(source)) {
			return Pair.of(new CacheKey(language + "#" + DigestUtils.sha256Hex(source.toString())), source.toString());
		}
		if (Path.class.isInstance(source)) {
			return Pair.of(new CacheKey(language + "@" + Path.class.cast(source).toRealPath().toUri()), null);
		}

		// Must be a reader, recurse after reading the contents
		Reader r = Reader.class.cast(source);
		String contents = IOUtils.toString(r);
		// We've read the contents, so recurse with the string
		Pair<CacheKey, String> p = JSR223Script.computeKey(language, contents);
		return Pair.of(p.getKey(), contents);
	}

	private static IOException handle(ConcurrentException e) {
		Throwable cause = e.getCause();
		if (IOException.class.isInstance(cause)) { return IOException.class.cast(cause); }
		return new IOException("Failed to read read the sourceCode's contents", e);
	}

	public static Map<String, String> getLanguages() {
		return JSR223Script.LANGUAGES;
	}

	public static CacheKey getCacheKey(String language, String script) throws ScriptException {
		try {
			return JSR223Script.computeKey(language, script).getKey();
		} catch (IOException e) {
			throw new UncheckedIOException("IOException caught while working in memory", e);
		}
	}

	public static void purge(CacheKey cacheKey) {
		if (cacheKey != null) {
			JSR223Script.CACHE.remove(cacheKey);
		}
	}

	public static JSR223Script getInstance(CacheKey cacheKey) {
		if (cacheKey == null) { return null; }
		return JSR223Script.CACHE.get(cacheKey);
	}

	private static JSR223Script getInstance(boolean allowCompilation, String language, final String script)
		throws ScriptException {
		final Pair<CacheKey, String> key;
		try {
			key = JSR223Script.computeKey(language, script);
		} catch (IOException e) {
			throw new RuntimeException("Unexpected IOException reading from memory", e);
		}

		try {
			return ConcurrentUtils.createIfAbsent(JSR223Script.CACHE, key.getKey(),
				new ConcurrentInitializer<JSR223Script>() {
					@Override
					public JSR223Script get() throws ConcurrentException {
						return new JSR223Script(allowCompilation, key.getKey(), language, key.getValue());
					}
				});
		} catch (ConcurrentException e) {
			throw new RuntimeException("Unexpected exception working in memory", e);
		}
	}

	private static JSR223Script getInstance(boolean allowCompilation, String language, final Reader r)
		throws ScriptException, IOException {
		final Pair<CacheKey, String> key = JSR223Script.computeKey(language, r);
		try {
			return ConcurrentUtils.createIfAbsent(JSR223Script.CACHE, key.getKey(),
				new ConcurrentInitializer<JSR223Script>() {
					@Override
					public JSR223Script get() throws ConcurrentException {
						return new JSR223Script(allowCompilation, key.getKey(), language, key.getValue());
					}
				});
		} catch (ConcurrentException e) {
			// Nothing will have happened here, since computeKey() did all the I/O
			throw new RuntimeException("Unexpected exception working in memory", e);
		}
	}

	private static JSR223Script getInstance(boolean allowCompilation, String language, Path source,
		final Charset charset) throws ScriptException, IOException {
		final Pair<CacheKey, String> key = JSR223Script.computeKey(language, source);
		try {
			return ConcurrentUtils.createIfAbsent(JSR223Script.CACHE, key.getKey(),
				new ConcurrentInitializer<JSR223Script>() {
					@Override
					public JSR223Script get() throws ConcurrentException {
						return new JSR223Script(allowCompilation, key.getKey(), language, source, charset);
					}
				});
		} catch (ConcurrentException e) {
			throw new RuntimeException("Unexpected exception working in memory", e);
		}
	}

	private class CompilationResult {
		private final CompiledScript compiled;
		private final ScriptException scriptException;
		private final IOException ioException;

		private CompilationResult(CompiledScript compiled) {
			this.compiled = compiled;
			this.scriptException = null;
			this.ioException = null;
		}

		private CompilationResult(Throwable thrown) {
			this.compiled = null;
			if (IOException.class.isInstance(thrown)) {
				this.ioException = IOException.class.cast(thrown);
				this.scriptException = null;
			} else if (ScriptException.class.isInstance(thrown)) {
				this.ioException = null;
				this.scriptException = ScriptException.class.cast(thrown);
			} else if (ConcurrentException.class.isInstance(thrown)) {
				Throwable cause = thrown.getCause();
				if (ScriptException.class.isInstance(cause)) {
					this.scriptException = ScriptException.class.cast(cause);
					this.ioException = null;
				} else {
					if (IOException.class.isInstance(cause)) {
						this.ioException = IOException.class.cast(cause);
					} else {
						this.ioException = new IOException("Unexpected exception", cause);
					}
					this.scriptException = null;
				}
			} else {
				throw new IllegalArgumentException(
					"Unsupported exception type " + thrown.getClass().getCanonicalName());
			}
		}

		private void throwCaught() throws IOException, ScriptException {
			if (this.ioException != null) { throw this.ioException; }
			if (this.scriptException != null) { throw this.scriptException; }
		}
	}

	private final boolean allowCompilation;
	private final CacheKey cacheKey;
	private final String language;
	private final ScriptEngineFactory factory;
	private final LazyInitializer<String> sourceCode;
	private final LazyInitializer<ScriptEngine> engine = new LazyInitializer<ScriptEngine>() {
		@Override
		protected ScriptEngine initialize() {
			return JSR223Script.this.factory.getScriptEngine();
		}
	};

	private volatile CompilationResult compilationResult = null;

	private JSR223Script(boolean allowCompilation, CacheKey cacheKey, String language, final String script) {
		this.allowCompilation = allowCompilation;
		this.cacheKey = cacheKey;
		this.language = StringUtils.lowerCase(language);
		this.factory = JSR223Script.getFactory(language);
		this.sourceCode = new LazyInitializer<String>() {
			@Override
			protected String initialize() {
				return script;
			}
		};
	}

	private JSR223Script(boolean allowCompilation, CacheKey cacheKey, String language, Path source, Charset charset) {
		this.allowCompilation = allowCompilation;
		this.cacheKey = cacheKey;
		this.language = StringUtils.lowerCase(language);
		this.factory = JSR223Script.getFactory(language);
		if (this.factory == null) { throw new IllegalArgumentException("Unsupported language [" + language + "]"); }
		this.sourceCode = new LazyInitializer<String>() {
			@Override
			protected String initialize() throws ConcurrentException {
				try (Reader r = Files.newBufferedReader(source, charset)) {
					return IOUtils.toString(r);
				} catch (IOException e) {
					throw new ConcurrentException(e);
				}
			}
		};
	}

	public String getLanguage() {
		return this.language;
	}

	public void compile() throws ScriptException, IOException {
		ScriptEngine engine;
		try {
			engine = this.engine.get();
		} catch (ConcurrentException e) {
			throw new RuntimeException("Unexpected exception while instantiating the ScriptEngine", e);
		}
		getCompiledScript(engine);
	}

	private CompiledScript getCompiledScript(ScriptEngine engine) throws ScriptException, IOException {
		if (!this.allowCompilation || !Compilable.class.isInstance(engine)) { return null; }

		CompilationResult result = this.compilationResult;
		if (result == null) {
			synchronized (this) {
				result = this.compilationResult;
				if (result == null) {
					try {
						String source = this.sourceCode.get();
						CompiledScript compiled = Compilable.class.cast(engine).compile(source);
						result = new CompilationResult(compiled);
						this.compilationResult = result;
					} catch (ConcurrentException | ScriptException e) {
						result = new CompilationResult(e);
						this.compilationResult = result;
					} finally {
						result.throwCaught();
					}
				}
			}
		}
		// If we have an exception, rethrow it...
		// TODO: how well will this work?
		result.throwCaught();
		return result.compiled;
	}

	public Object execute(Consumer<Bindings> initializer) throws ScriptException, IOException {
		return execute((initializer != null ? (b) -> {
			initializer.accept(b);
			return true;
		} : null));
	}

	public Object execute(Predicate<Bindings> initializer) throws ScriptException, IOException {
		ScriptEngine engine;
		try {
			engine = this.engine.get();
		} catch (ConcurrentException e) {
			throw new RuntimeException("Unexpected exception while instantiating the ScriptEngine", e);
		}

		final Bindings bindings = engine.createBindings();
		if ((initializer != null) && !initializer.test(bindings)) {
			throw new ScriptException("Binding initialization failed");
		}

		final CompiledScript compiledScript = getCompiledScript(engine);
		if (compiledScript != null) { return compiledScript.eval(bindings); }

		try {
			return engine.eval(this.sourceCode.get(), bindings);
		} catch (ConcurrentException e) {
			throw JSR223Script.handle(e);
		}
	}

	public CacheKey getCacheKey() {
		return this.cacheKey;
	}

	public void dispose() {
		JSR223Script.purge(this.cacheKey);
	}
}
