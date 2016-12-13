package com.kroger.rp.tests;

import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.junit.*;

import static com.kroger.rp.util.AppServiceCluster.makeHttpWebService;
import static com.kroger.rp.util.AppServiceCluster.makeHttpsWebService;
import static com.kroger.rp.util.call.CallUtility.make;
import static com.kroger.rp.util.call.TestRequest.getRequest;

public class ExampleTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);
    public static AppServiceCluster contentUpstreamApp = makeHttpWebService("Content_Cluster", 1);
    public static AppServiceCluster cartUpstreamApp = makeHttpsWebService("Cart_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = NginxRpBuilder.startNginxRpWithCluster(loginUpstreamApp, contentUpstreamApp, cartUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.stop();
    }

    @Test
    public void should_301_http_to_https() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andHasResponseHeader("Location", "https://www.nginx-test.com/")
                .expectResponseCode(301);
    }

    @Test
    public void should_properly_route_homepage() {
        make(getRequest("https://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectClusterName("Content_Cluster")
                .expectResponseCode(200);
    }

    @Test
    public void should_properly_route_login() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectClusterName("Login_Cluster")
                .expectResponseCode(200);
    }

    @Test
    public void should_properly_route_checkout() {
        make(getRequest("https://www.nginx-test.com/checkout").to(nginxReverseProxy))
                .andExpectClusterName("Cart_Cluster")
                .andExpectAppPath("/cart/checkout")
                .expectResponseCode(200);
    }
}
