package com.kroger.rp.util;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.Assert.*;

public class TestFrameworkPropertiesTest {

    @After
    public void cleanup() {
        TestFrameworkProperties.initProperties("snow-globe.yaml");
    }

    @Test
    public void shouldReturnCorrectFakeUpstreamImage() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus.com/nginx/fake-upstream-service:999", TestFrameworkProperties.getFakeUpstreamImage());
    }

    @Test
    public void shouldLogContainerOutput() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(TestFrameworkProperties.logContainerOutput());
    }

    @Test
    public void shouldNotPreserveTempFiles() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertFalse(TestFrameworkProperties.preserveTempFiles());
    }

    @Test
    public void shouldDefineUpstreamZones() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(TestFrameworkProperties.defineUpstreamZones());
    }

    @Test
    public void shouldGetNginxContainer() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus.com/library/nginx:999", TestFrameworkProperties.getNginxImage());
    }

    @Test
    public void shouldGetDefaultUpstreamFilePath() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/etc/nginx/upstreams.conf", TestFrameworkProperties.getUpstreamLocation(null));
    }

    @Test
    public void shouldGetUpstreamFilePathForEnvironment() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/bogus/etc/nginx/upstreams.conf", TestFrameworkProperties.getUpstreamLocation("bogus-environment"));
    }

    @Test
    public void shouldGetDefaultNginxVolumes() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        List<String> vals = Stream.of("src/test/resources/nginx/nginx.conf:/etc/nginx/nginx.conf", "src/test/resources/nginx/dev/*:/etc/nginx/dev/").collect(Collectors.toList());
        assertTrue(TestFrameworkProperties.getNginxVolumes(null).containsAll(vals));
    }

    @Test
    public void shouldGetNginxVolumesForEnvironment() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        List<String> vals = Stream.of("vol1:vol1", "vol2:vol2", "vol3:vol3", "vol4:vol4").collect(Collectors.toList());
        assertTrue(TestFrameworkProperties.getNginxVolumes("bogus-environment").containsAll(vals));
    }

    @Test
    public void shouldGetNginxUrlPortMappings() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        Map<String, Object> sMap = new HashMap<>();
        sMap.put("pattern", "https:.*");
        sMap.put("port", 443);

        Map<String, Object> iMap = new HashMap<>();
        iMap.put("pattern", "http:.*");
        iMap.put("port", 80);

        List<Map<String, Object>> mapping = TestFrameworkProperties.getNginxPortMapping();
        assertEquals(sMap, mapping.get(0).get("https"));
        assertEquals(iMap, mapping.get(1).get("http"));
    }

    @Test
    public void shouldGetNginxConfFilesToScan() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertTrue(TestFrameworkProperties.getFilesToScan(null).contains("src/test/resources/nginx/nginx.conf"));

        List<String> env1 = Stream.of("src/test/resources/nginx/env1-nginx.conf", "src/test/resources/nginx/env1-routing.conf").collect(Collectors.toList());
        assertTrue(TestFrameworkProperties.getFilesToScan("env1").containsAll(env1));

        List<String> env2 = Stream.of("src/test/resources/nginx/env2-nginx.conf", "src/test/resources/nginx/env2/").collect(Collectors.toList());
        assertTrue(TestFrameworkProperties.getFilesToScan("env2").containsAll(env2));
    }

    @Test
    public void shouldGetSourceDirectory() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("src/test/resources/nginx/", TestFrameworkProperties.getSourceDirectory());
    }

    @Test
    public void shouldGetDeployedDirectory() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("/etc/nginx/", TestFrameworkProperties.getDeployedDirectory());
    }

    @Test
    public void shouldGetDefaultStartCommand() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("nginx -g 'daemon off;'", TestFrameworkProperties.getStartCommand(null));
    }

    @Test
    public void shouldGetStartCommandForEnvironment() {
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/bogus-snow-globe-1.yaml");
        assertEquals("bogus -a whee!!", TestFrameworkProperties.getStartCommand("bogus"));
    }

}