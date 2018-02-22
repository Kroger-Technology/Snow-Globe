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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class FrameworkPropertiesTest {

    FrameworkProperties frameworkProperties;

    @Before
    public void init() {
        frameworkProperties = new FrameworkProperties();
        frameworkProperties.loadFile("snow-globe.yml");
    }

    @Test
    public void shouldReturnCorrectFakeUpstreamImage() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("bogus.com/nginx/upstream-bounce-service:999", frameworkProperties.getUpstreamBounceImage());
    }

    @Test
    public void shouldLogContainerOutput() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertTrue(frameworkProperties.logContainerOutput());
    }

    @Test
    public void shouldNotPreserveTempFiles() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertFalse(frameworkProperties.preserveTempFiles());
    }

    @Test
    public void shouldDefineUpstreamZones() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertTrue(frameworkProperties.defineUpstreamZones());
    }

    @Test
    public void shouldGetNginxContainer() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("bogus.com/library/nginx:999", frameworkProperties.getNginxImage());
    }

    @Test
    public void shouldGetDefaultUpstreamFilePath() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("/etc/nginx/upstreams.conf", frameworkProperties.getUpstreamLocation());
    }

    @Test
    public void shouldGetUpstreamFilePathForEnvironment() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("/etc/nginx/upstreams.conf", frameworkProperties.getUpstreamLocation());
    }

    @Test
    public void shouldGetDefaultNginxVolumes() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        List<String> vals = Stream.of("src/test/resources/nginx/nginx.conf:/etc/nginx/nginx.conf", "src/test/resources/nginx/dev/*:/etc/nginx/dev/").collect(Collectors.toList());
        assertTrue(frameworkProperties.getNginxVolumes().containsAll(vals));
    }

    @Test
    public void shouldGetNginxUrlPortMappings() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        Map<String, Object> sMap = new HashMap<>();
        sMap.put("pattern", "https:.*");
        sMap.put("port", 443);

        Map<String, Object> iMap = new HashMap<>();
        iMap.put("pattern", "http:.*");
        iMap.put("port", 80);

        List<Map<String, Object>> mapping = frameworkProperties.getNginxPortMapping();
        assertEquals(sMap, mapping.get(0).get("https"));
        assertEquals(iMap, mapping.get(1).get("http"));
    }

    @Test
    public void shouldGetNginxConfFilesToScan() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertTrue(frameworkProperties.getFilesToScan().contains("src/test/resources/nginx/nginx.conf"));

    }

    @Test
    public void shouldGetSourceDirectory() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("src/test/resources/nginx/", frameworkProperties.getSourceDirectory());
    }

    @Test
    public void shouldGetDeployedDirectory() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("/etc/nginx/", frameworkProperties.getDeployedDirectory());
    }

    @Test
    public void shouldGetDefaultStartCommand() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("nginx -g 'daemon off;'", frameworkProperties.getStartCommand());
    }

    @Test
    public void shouldGetStartCommandForEnvironment() {
        frameworkProperties.loadFile("src/test/resources/bogus-snow-globe-1.yml");
        assertEquals("nginx -g 'daemon off;'", frameworkProperties.getStartCommand());
    }

    @Test
    public void shouldBeAbleToOverrideProperties() {
        // This is different because if you set this before your test, then all parts that create the
        // FrameworkProperties object will use the same one.
        FrameworkProperties.setConfigFile("src/test/resources/bogus-snow-globe-1.yml");
        frameworkProperties = new FrameworkProperties();
        assertTrue(frameworkProperties.defineUpstreamZones());
        FrameworkProperties.setConfigFile(null);
    }

    @Test
    public void shouldBeAbleToGetDefaultUpsteamName() {
        assertThat(frameworkProperties.getUpstreamName(), is("upstream"));
    }
}