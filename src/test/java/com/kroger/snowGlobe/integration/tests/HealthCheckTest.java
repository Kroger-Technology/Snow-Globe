package com.kroger.snowGlobe.integration.tests;

import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.rp.util.AppServiceCluster.makeHttpWebService;
import static com.kroger.rp.util.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.rp.util.call.CallUtility.make;
import static com.kroger.rp.util.call.TestRequest.getRequest;

/**
 * Created by rh40037 on 3/21/17.
 */
public class HealthCheckTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster contentCluster = makeHttpWebService("Content_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(contentCluster);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_have_successful_health_check() {
        make(getRequest("https://www.nginx-test.com")
                .withHealthCheck("/healthcheck")
                .to(nginxReverseProxy))
                .andExpectClusterName("Content_Cluster")
                .expectSuccessfulHealthCheck();
    }
}
