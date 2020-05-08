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
package com.armedia.commons.jmeter.gherkin.impl.jbehave;

import java.util.function.Function;

import org.easymock.EasyMock;
import org.jbehave.core.io.StoryLoader;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.armedia.commons.jmeter.gherkin.jbehave.InterceptingStoryLoader;

public class InterceptingStoryLoaderTest {

	@Test
	public void testInterceptingStoryLoader() {
		StoryLoader nullSL = null;
		StoryLoader sl = EasyMock.createStrictMock(StoryLoader.class);
		Function<String, String> nullF = null;
		Function<String, String> sf = EasyMock.createStrictMock(Function.class);
		Function<String, String> rf = EasyMock.createStrictMock(Function.class);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, sl);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, sl);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, nullF);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, sf);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, nullF);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, sf);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, nullF, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, nullF, sl);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, sf, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(nullF, sf, sl);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, nullF, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, nullF, sl);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, sf, nullSL);
		EasyMock.verify(sl, sf, rf);

		EasyMock.reset(sl, sf, rf);
		EasyMock.replay(sl, sf, rf);
		new InterceptingStoryLoader(rf, sf, sl);
		EasyMock.verify(sl, sf, rf);
	}

	@Test
	public void testGetFallback() {
		StoryLoader sl = EasyMock.createStrictMock(StoryLoader.class);

		InterceptingStoryLoader isl = null;

		isl = new InterceptingStoryLoader(null, null, null);
		Assertions.assertNull(isl.getFallback());

		EasyMock.reset(sl);
		EasyMock.replay(sl);
		isl = new InterceptingStoryLoader(null, sl);
		Assertions.assertSame(sl, isl.getFallback());
		EasyMock.verify(sl);
	}

	@Test
	public void testLoadResourceAsText() {
		StoryLoader sl = EasyMock.createStrictMock(StoryLoader.class);

		Function<String, String> rf = EasyMock.createStrictMock(Function.class);
		InterceptingStoryLoader isl = new InterceptingStoryLoader(rf, sl);

		for (int i = 0; i < 99; i++) {
			String fbKey = String.format("FBKey-#%02d", i);
			String fbValue = String.format("FBValue-#%02d", i);
			String noKey = String.format("NoKey-#%02d", i);
			String key = String.format("Key-#%02d", i);
			String value = String.format("Value-#%02x", i);
			EasyMock.reset(sl, rf);
			EasyMock.expect(rf.apply(key)).andReturn(value).atLeastOnce();
			EasyMock.expect(rf.apply(EasyMock.eq(fbKey))).andReturn(null).atLeastOnce();
			EasyMock.expect(rf.apply(noKey)).andReturn(null).atLeastOnce();
			EasyMock.expect(sl.loadResourceAsText(EasyMock.eq(fbKey))).andReturn(fbValue).atLeastOnce();
			EasyMock.expect(sl.loadResourceAsText(EasyMock.eq(noKey))).andReturn(null).atLeastOnce();
			EasyMock.replay(sl, rf);
			Assertions.assertSame(sl, isl.getFallback());
			Assertions.assertEquals(value, isl.loadResourceAsText(key));
			Assertions.assertEquals(value, isl.loadResourceAsText(key));
			Assertions.assertEquals(value, isl.loadResourceAsText(key));
			Assertions.assertEquals(value, isl.loadResourceAsText(key));
			Assertions.assertEquals(fbValue, isl.loadResourceAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadResourceAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadResourceAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadResourceAsText(fbKey));
			Assertions.assertNull(isl.loadResourceAsText(noKey));
			Assertions.assertNull(isl.loadResourceAsText(noKey));
			Assertions.assertNull(isl.loadResourceAsText(noKey));
			Assertions.assertNull(isl.loadResourceAsText(noKey));
			EasyMock.verify(sl, rf);
		}
	}

	@Test
	public void testLoadStoryAsText() {
		StoryLoader sl = EasyMock.createStrictMock(StoryLoader.class);

		Function<String, String> sf = EasyMock.createStrictMock(Function.class);
		InterceptingStoryLoader isl = new InterceptingStoryLoader(null, sf, sl);

		// Test the interception
		for (int i = 0; i < 99; i++) {
			String fbKey = String.format("FBKey-#%02d", i);
			String fbValue = String.format("FBValue-#%02d", i);
			String noKey = String.format("NoKey-#%02d", i);
			String key = String.format("Key-#%02d", i);
			String value = String.format("Value-#%02x", i);
			EasyMock.reset(sl, sf);
			EasyMock.expect(sf.apply(key)).andReturn(value).atLeastOnce();
			EasyMock.expect(sf.apply(EasyMock.eq(fbKey))).andReturn(null).atLeastOnce();
			EasyMock.expect(sf.apply(noKey)).andReturn(null).atLeastOnce();
			EasyMock.expect(sl.loadStoryAsText(EasyMock.eq(fbKey))).andReturn(fbValue).atLeastOnce();
			EasyMock.expect(sl.loadStoryAsText(EasyMock.eq(noKey))).andReturn(null).atLeastOnce();
			EasyMock.replay(sl, sf);
			Assertions.assertSame(sl, isl.getFallback());
			Assertions.assertEquals(value, isl.loadStoryAsText(key));
			Assertions.assertEquals(value, isl.loadStoryAsText(key));
			Assertions.assertEquals(value, isl.loadStoryAsText(key));
			Assertions.assertEquals(value, isl.loadStoryAsText(key));
			Assertions.assertEquals(fbValue, isl.loadStoryAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadStoryAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadStoryAsText(fbKey));
			Assertions.assertEquals(fbValue, isl.loadStoryAsText(fbKey));
			Assertions.assertNull(isl.loadStoryAsText(noKey));
			Assertions.assertNull(isl.loadStoryAsText(noKey));
			Assertions.assertNull(isl.loadStoryAsText(noKey));
			Assertions.assertNull(isl.loadStoryAsText(noKey));
			EasyMock.verify(sl, sf);
		}
	}
}
