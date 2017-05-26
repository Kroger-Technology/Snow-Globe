package com.kroger.snowGlobe.integration.tests;


import com.kroger.oss.snowGlobe.AppServiceCluster;
import com.kroger.oss.snowGlobe.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.oss.snowGlobe.AppServiceCluster.makeHttpWebService;
import static com.kroger.oss.snowGlobe.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.oss.snowGlobe.call.CallUtility.make;
import static com.kroger.oss.snowGlobe.call.TestRequest.getRequest;

public class IsolatedExecutionTest {

    public static NginxRpBuilder nginxReverseProxy1;
    public static NginxRpBuilder nginxReverseProxy2;
    public static AppServiceCluster contentCluster = makeHttpWebService("Content_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy1 = startNginxRpWithCluster(contentCluster);
        nginxReverseProxy2 = startNginxRpWithCluster(contentCluster);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy1.stop();
        nginxReverseProxy2.stop();
    }

    @Test
    public void should_be_able_to_run_two_environments_at_the_same_time_with_shared_app_service() {
        make(getRequest("https://www.nginx-test.com").to(nginxReverseProxy1))
                .andExpectResponseCode(200);
        make(getRequest("https://www.nginx-test.com/body").to(nginxReverseProxy2))
                .andExpectResponseCode(200);

    }
}
