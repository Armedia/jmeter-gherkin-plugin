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

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.LinkedHashMap;
import java.util.function.Function;

public class Environment extends LinkedHashMap<String, Object> {
	private static final long serialVersionUID = 1L;

	private static final <T extends Number> T toNumber(Class<T> k, Object o, Function<Number, T> extractor,
		Function<String, T> parser) {
		if (o == null) { return null; }
		if (k.isInstance(o)) { return k.cast(o); }
		if (Number.class.isInstance(o)) { return extractor.apply(Number.class.cast(o)); }
		return parser.apply(o.toString());
	}

	private static final Byte toByte(Object o) {
		return Environment.toNumber(Byte.class, o, Number::byteValue, Byte::valueOf);
	}

	private static final Short toShort(Object o) {
		return Environment.toNumber(Short.class, o, Number::shortValue, Short::valueOf);
	}

	private static final Integer toInteger(Object o) {
		return Environment.toNumber(Integer.class, o, Number::intValue, Integer::valueOf);
	}

	private static final Long toLong(Object o) {
		return Environment.toNumber(Long.class, o, Number::longValue, Long::valueOf);
	}

	private static final Float toFloat(Object o) {
		return Environment.toNumber(Float.class, o, Number::floatValue, Float::valueOf);
	}

	private static final Double toDouble(Object o) {
		return Environment.toNumber(Double.class, o, Number::doubleValue, Double::valueOf);
	}

	private static final BigInteger toBigInteger(Object o) {
		if (o == null) { return null; }
		if (Number.class.isInstance(o)) { return BigInteger.valueOf(Number.class.cast(o).longValue()); }
		return new BigInteger(o.toString());
	}

	private static final BigDecimal toBigDecimal(Object o) {
		if (o == null) { return null; }
		if (Number.class.isInstance(o)) { return BigDecimal.valueOf(Number.class.cast(o).doubleValue()); }
		return new BigDecimal(o.toString());
	}

	private static final String toString(Object o) {
		if (o == null) { return null; }
		return o.toString();
	}

	private final Function<String, Object> forwardResolver;

	Environment() {
		this(null);
	}

	Environment(Function<String, Object> forwardResolver) {
		this.forwardResolver = forwardResolver;
	}

	@Override
	public Object get(Object key) {
		Object ret = super.get(key);
		if ((ret == null) && (this.forwardResolver != null)) {
			ret = this.forwardResolver.apply(Environment.toString(key));
		}
		return ret;
	}

	@Override
	public Object getOrDefault(Object key, Object defaultValue) {
		Object ret = get(key);
		return (ret != null ? ret : defaultValue);
	}

	public <T> T get(String key, Function<Object, T> decoder) {
		Object ret = get(key);
		return decoder.apply(ret);
	}

	public <T> T getAs(String key, Class<T> klazz) {
		return get(key, (o) -> {
			if (klazz.isInstance(o)) { return klazz.cast(o); }
			return null;
		});
	}

	public String getAsString(String key) {
		return get(key, (o) -> (o != null ? o.toString() : null));
	}

	public Byte getAsByte(String key) {
		return get(key, Environment::toByte);
	}

	public Short getAsShort(String key) {
		return get(key, Environment::toShort);
	}

	public Integer getAsInt(String key) {
		return get(key, Environment::toInteger);
	}

	public Long getAsLong(String key) {
		return get(key, Environment::toLong);
	}

	public Float getAsFloat(String key) {
		return get(key, Environment::toFloat);
	}

	public Double getAsDouble(String key) {
		return get(key, Environment::toDouble);
	}

	public BigInteger getAsBigInteger(String key) {
		return get(key, Environment::toBigInteger);
	}

	public BigDecimal getAsBigDecimal(String key) {
		return get(key, Environment::toBigDecimal);
	}
}
