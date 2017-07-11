/*
 * Snow-Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.oss.snowGlobe;

import org.junit.Before;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PortMapperTest {

    private PortMapper portMapper;
    TestFrameworkProperties testFrameworkProperties;

    @Before
    public void init() {
        portMapper = new PortMapper();
        testFrameworkProperties = new TestFrameworkProperties();
        testFrameworkProperties.loadFile("src/test/resources/test-snow-globe.yml");
    }

    @Test
    public void shouldConsistentlyAssignCorrectPortToSecureAndInsecureUrls() {
        portMapper.initMapping(testFrameworkProperties);
        int subdomainPort = portMapper.getMappedPortForUrl("https://sub.secureurl.com");
        int securePort = portMapper.getMappedPortForUrl("https://secureurl.com");
        int inSecurePort = portMapper.getMappedPortForUrl("http://insecureurl.com");
        assertNotEquals(securePort, inSecurePort);

        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(securePort, (int) portMapper.getMappedPortForUrl("https://" + i + ".com")));
        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(inSecurePort, (int) portMapper.getMappedPortForUrl("http://" + i + ".com")));
        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(subdomainPort, (int) portMapper.getMappedPortForUrl("https://sub." + i + ".com")));
        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(inSecurePort, (int) portMapper.getMappedPortForUrl("http://lower." + i + ".com")));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnBadUrlFormat() {
        portMapper.initMapping(testFrameworkProperties);
        portMapper.getMappedPortForUrl("badurl.com");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnUnitializedPortMappings() {
        portMapper.getMappedPortForUrl("http://url.com");
    }
}