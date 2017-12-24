package com.kroger.oss.snowGlobe.integration.tests;

import com.kroger.oss.snowGlobe.NginxRpBuilder;
import com.kroger.oss.snowGlobe.TestFrameworkProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.oss.snowGlobe.AppServiceCluster.makeHttpsWebService;
import static com.kroger.oss.snowGlobe.NginxRpBuilder.runNginxWithUpstreams;
import static com.kroger.oss.snowGlobe.call.CallUtility.make;
import static com.kroger.oss.snowGlobe.call.TestRequest.getRequest;

/**
 * Verifies that this works with a config that has no upstreams.
 */
public class BasicConfigTest {

    public static NginxRpBuilder nginxReverseProxy;

    @BeforeClass
    public static void setup() {
        TestFrameworkProperties.setConfigFile("src/integration/resources/snow-globe-basic.yml");
        nginxReverseProxy = runNginxWithUpstreams();
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
        TestFrameworkProperties.setConfigFile(null);
    }

    @Test
    public void should_work_even_if_nginx_is_slow_to_start() {
        make(getRequest("http://www.nginx-test.com/static.html").to(nginxReverseProxy))
                .andExpectResponseCode(200);
    }
}
