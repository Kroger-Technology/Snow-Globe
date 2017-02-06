package com.kroger.snowGlobe.integration.tests;

import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.stream.IntStream;

import static com.kroger.rp.util.AppServiceCluster.makeHttpsWebService;
import static com.kroger.rp.util.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.rp.util.call.CallUtility.make;
import static com.kroger.rp.util.call.TestRequest.getRequest;
import static java.util.stream.IntStream.range;

/**
 * This integration test verifies that the request is returned by a specific instance of the upstream server.  The snow globe
 * configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class ClusterNumberTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 10);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(loginUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_round_robin_each_request_to_each_upstream_instance() {
        range(0,10).forEach(clusterNumber ->
                make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectClusterNumber(clusterNumber));
    }
}
