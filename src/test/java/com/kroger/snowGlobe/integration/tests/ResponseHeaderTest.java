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
 * This integration test verifies response headers are returned.  The snow globe configuration is located at the root
 * of the project in "snow-globe.yaml".  The yaml file references the example configuration in the
 * "src/integrationTestNginxConfig"
 */
public class ResponseHeaderTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster cartUpstreamApp = makeHttpsWebService("Cart_Cluster", 1)
            .withResponseHeader("got-cart", "success")
            .withResponseHeader("internal-secret-key", "42");

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(cartUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_return_response_headers() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
                .andExpectResponseHeader("got-cart", "success")
                .andExpectResponseHeader("rp-response-header", "true");
    }

    @Test
    public void should_not_return_secret_header() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
                .andExpectMissingResponseHeader("internal-secret_key");
    }
}
