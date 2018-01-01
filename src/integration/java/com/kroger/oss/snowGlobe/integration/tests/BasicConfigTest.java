package com.kroger.oss.snowGlobe.integration.tests;

import com.kroger.oss.snowGlobe.NginxRpBuilder;
import org.junit.Before;
import org.junit.Test;

import static com.kroger.oss.snowGlobe.NginxRpBuilder.runNginxWithUpstreams;
import static com.kroger.oss.snowGlobe.call.CallUtility.make;
import static com.kroger.oss.snowGlobe.call.TestRequest.getRequest;

/**
 * Verifies that this works with a config that has no upstreams.
 */
public class BasicConfigTest {

    public NginxRpBuilder nginxReverseProxy;

    @Before
    public void setup() {
        nginxReverseProxy = runNginxWithUpstreams("src/integration/resources/snow-globe-basic.yml");
    }

    @Test
    public void should_handle_call_with_default_port_mapping() {
        // the snow-globe-basic.yml file doesn't use a default port mapping.
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseCode(200);
    }

    @Test
    public void should_fetch_basic_static_content() {
        try {

            make(getRequest("http://www.nginx-test.com/static.html").to(nginxReverseProxy))
                    .andExpectResponseCode(200);
        } finally {
            nginxReverseProxy.outputNginxLogs();
        }
    }
}
