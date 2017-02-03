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
 * This integration test verifies that the path that was sent on the upstream path works correctly for an example setup.
 * The snow globe configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class RequestHeaderTest {

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
    public void should_add_x_proto_header_to_login_request() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectRequestHeaderToApplicationMatching("x-forwarded-proto", "https")
                .andExpectRequestHeaderToApplicationMatching("host", "www.nginx-test.com");
    }
}
