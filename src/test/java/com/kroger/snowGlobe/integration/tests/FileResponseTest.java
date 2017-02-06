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
 * This integration test verifies that a file is sent back in the body of the response  The snow globe
 * configuration is located at the root of the project in "snow-globe.yaml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class FileResponseTest {

    public static NginxRpBuilder nginxReverseProxy;

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = startNginxRpWithCluster();
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_have_response_match_file() {
        make(getRequest("https://www.nginx-test.com/body").to(nginxReverseProxy))
                .andExpectResponseBodyMatchesFileContents("src/integrationTestNginxConfig/static/static.html");
    }

    @Test
    public void should_have_response_match_string() {
        make(getRequest("https://www.nginx-test.com/body").to(nginxReverseProxy))
                .andExpectResponseBodyContent("<html><head>Head</head><body><div>shoulders</div><div>knees</div></body><footer>toes</footer></html>");
    }
}
