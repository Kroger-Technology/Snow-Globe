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
 * This integration test verifies that the request sent to the upstream server has the given query param.  The snow globe
 * configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class QueryParamTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster searchCluster = makeHttpWebService("Search_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster(searchCluster);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_convert_path_to_query_param() {
        make(getRequest("https://www.nginx-test.com/search/milk").to(nginxReverseProxy))
                .andExpectToHaveQueryParam("q", "milk");
    }
}
