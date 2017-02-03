package com.kroger.snowGlobe.integration.tests;

import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.rp.util.AppServiceCluster.makeHttpWebService;
import static com.kroger.rp.util.AppServiceCluster.makeHttpsWebService;
import static com.kroger.rp.util.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.rp.util.call.CallUtility.make;
import static com.kroger.rp.util.call.TestRequest.getRequest;

/**
 * This integration test verifies that the cluster name works correctly for an example setup.  The snow globe
 * configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class ClusterNameTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);
    public static AppServiceCluster itemUpstreamApp = makeHttpWebService("Item_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(loginUpstreamApp, itemUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_route_login_request_to_login_cluster() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectClusterName("Login_Cluster");
    }

    @Test
    public void should_route_item_request_to_item_cluster() {
        make(getRequest("https://www.nginx-test.com/item").to(nginxReverseProxy))
                .andExpectClusterName("Item_Cluster");
    }
}
