package com.kroger.rp.util;

import org.junit.Before;
import org.junit.Test;

import java.util.stream.IntStream;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;

public class PortMapperTest {

    private PortMapper portMapper;

    @Before
    public void init() {
        portMapper = new PortMapper();
        TestFrameworkProperties.initPropertiesFromFile("src/test/resources/test-snow-globe.yaml");
    }

    @Test
    public void shouldConsistentlyAssignCorrectPortToSecureAndInsecureUrls() {
        portMapper.initMapping();
        int securePort = portMapper.getMappedPortForUrl("https://secureurl.com");
        int inSecurePort = portMapper.getMappedPortForUrl("http://insecureurl.com");
        assertNotEquals(securePort, inSecurePort);

        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(securePort, (int) portMapper.getMappedPortForUrl("https://" + i + ".com")));
        IntStream.rangeClosed(1, 1000).forEach(i -> assertEquals(inSecurePort, (int) portMapper.getMappedPortForUrl("http://" + i + ".com")));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnBadUrlFormat() {
        portMapper.initMapping();
        portMapper.getMappedPortForUrl("badurl.com");
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowExceptionOnUnitializedPortMappings() {
        portMapper.getMappedPortForUrl("http://url.com");
    }
}