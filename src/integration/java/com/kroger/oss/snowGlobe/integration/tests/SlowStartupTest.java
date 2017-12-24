package com.kroger.oss.snowGlobe.integration.tests;

import com.kroger.oss.snowGlobe.AppServiceCluster;
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
 * This goal of this test suite is to make sure that the tests run even if the upstreams or nginx container are slow
 * to start.  There have been race conditions with slower configurations and also with parallel running tests.
 */
public class SlowStartupTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp;

    @BeforeClass
    public static void setup() {
        TestFrameworkProperties.setConfigFile("src/integration/resources/snow-globe-slow-start.yml");
        loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);
        nginxReverseProxy = runNginxWithUpstreams(loginUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
        TestFrameworkProperties.setConfigFile(null);
    }

    @Test
    public void should_work_even_if_nginx_is_slow_to_start() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseHeader("Location", "https://www.nginx-test.com/")
                .andExpectResponseCode(301);
    }
}
