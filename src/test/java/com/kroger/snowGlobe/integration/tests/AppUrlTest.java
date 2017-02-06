package com.kroger.snowGlobe.integration.tests;

import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.rp.util.AppServiceCluster.makeHttpsWebService;
import static com.kroger.rp.util.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.rp.util.call.CallUtility.make;
import static com.kroger.rp.util.call.TestRequest.getRequest;

/**
 * This integration test verifies that the verification of the app url works correctly for an example setup.  The snow globe
 * configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class AppUrlTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(loginUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_properly_pass_url_fields() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectAppUrl("https://www.nginx-test.com/login-path");
    }
}
