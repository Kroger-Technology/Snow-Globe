/*
 * Nginx Snow Globe
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

import static org.junit.Assert.*;

public class TestFrameworkPropertiesTest {

    TestFrameworkProperties testFrameworkProperties;

    @Before
    public void init() {
        testFrameworkProperties = new TestFrameworkProperties();
        testFrameworkProperties.initProperties("snow-globe.yaml");
    }

    @Test
    public void shouldReturnCorrectFakeUpstreamImage() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus.com/nginx/fake-upstream-service:999", testFrameworkProperties.getFakeUpstreamImage());
    }

    @Test
    public void shouldLogContainerOutput() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(testFrameworkProperties.logContainerOutput());
    }

    @Test
    public void shouldNotPreserveTempFiles() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertFalse(testFrameworkProperties.preserveTempFiles());
    }

    @Test
    public void shouldDefineUpstreamZones() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(testFrameworkProperties.defineUpstreamZones());
    }

    @Test
    public void shouldGetNginxContainer() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus.com/library/nginx:999", testFrameworkProperties.getNginxImage());
    }

    @Test
    public void shouldGetDefaultUpstreamFilePath() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/etc/nginx/upstreams.conf", testFrameworkProperties.getUpstreamLocation(null));
    }

    @Test
    public void shouldGetUpstreamFilePathForEnvironment() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/bogus/etc/nginx/upstreams.conf", testFrameworkProperties.getUpstreamLocation("bogus-environment"));
    }

    @Test
    public void shouldGetDefaultNginxVolumes() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        List<String> vals = Stream.of("src/test/resources/nginx/nginx.conf:/etc/nginx/nginx.conf", "src/test/resources/nginx/dev/*:/etc/nginx/dev/").collect(Collectors.toList());
        assertTrue(testFrameworkProperties.getNginxVolumes(null).containsAll(vals));
    }

    @Test
    public void shouldGetNginxVolumesForEnvironment() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        List<String> vals = Stream.of("vol1:vol1", "vol2:vol2", "vol3:vol3", "vol4:vol4").collect(Collectors.toList());
        assertTrue(testFrameworkProperties.getNginxVolumes("bogus-environment").containsAll(vals));
    }

    @Test
    public void shouldGetNginxUrlPortMappings() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        Map<String, Object> sMap = new HashMap<>();
        sMap.put("pattern", "https:.*");
        sMap.put("port", 443);

        Map<String, Object> iMap = new HashMap<>();
        iMap.put("pattern", "http:.*");
        iMap.put("port", 80);

        List<Map<String, Object>> mapping = testFrameworkProperties.getNginxPortMapping();
        assertEquals(sMap, mapping.get(0).get("https"));
        assertEquals(iMap, mapping.get(1).get("http"));
    }

    @Test
    public void shouldGetNginxConfFilesToScan() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(testFrameworkProperties.getFilesToScan(null).contains("src/test/resources/nginx/nginx.conf"));

        List<String> env1 = Stream.of("src/test/resources/nginx/env1-nginx.conf", "src/test/resources/nginx/env1-routing.conf").collect(Collectors.toList());
        assertTrue(testFrameworkProperties.getFilesToScan("env1").containsAll(env1));

        List<String> env2 = Stream.of("src/test/resources/nginx/env2-nginx.conf", "src/test/resources/nginx/env2/").collect(Collectors.toList());
        assertTrue(testFrameworkProperties.getFilesToScan("env2").containsAll(env2));
    }

    @Test
    public void shouldGetSourceDirectory() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("src/test/resources/nginx/", testFrameworkProperties.getSourceDirectory());
    }

    @Test
    public void shouldGetDeployedDirectory() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/etc/nginx/", testFrameworkProperties.getDeployedDirectory());
    }

    @Test
    public void shouldGetDefaultStartCommand() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("nginx -g 'daemon off;'", testFrameworkProperties.getStartCommand(null));
    }

    @Test
    public void shouldGetStartCommandForEnvironment() {
        testFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus -a whee!!", testFrameworkProperties.getStartCommand("bogus"));
    }

}