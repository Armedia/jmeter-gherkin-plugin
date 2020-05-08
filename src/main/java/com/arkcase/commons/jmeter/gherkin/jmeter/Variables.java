package com.arkcase.commons.jmeter.gherkin.jmeter;

import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;

import org.apache.jmeter.threads.JMeterVariables;

public class Variables {

	private final JMeterVariables vars;

	public Variables(JMeterVariables vars) {
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