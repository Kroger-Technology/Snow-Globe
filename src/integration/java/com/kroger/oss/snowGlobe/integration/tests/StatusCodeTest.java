/*
 * Snow-Globe
 *
 * Copyright 2017 The Kroger Co.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kroger.oss.snowGlobe.integration.tests;

import com.kroger.oss.snowGlobe.AppServiceCluster;
import com.kroger.oss.snowGlobe.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.oss.snowGlobe.AppServiceCluster.makeHttpsWebService;
import static com.kroger.oss.snowGlobe.NginxRpBuilder.runNginxWithUpstreams;
import static com.kroger.oss.snowGlobe.call.CallUtility.make;
import static com.kroger.oss.snowGlobe.call.TestRequest.getRequest;

/**
 * This integration test verifies that the status code works correctly for an example setup.  The snow globe
 * configuration is located at the root of the project in "snow-globe.yml".  The yaml file references
 * the example configuration in the "src/integrationTestNginxConfig"
 */
public class StatusCodeTest {

    public static NginxRpBuilder nginxReverseProxy;
    public static AppServiceCluster loginUpstreamApp = makeHttpsWebService("Login_Cluster", 1);

    @BeforeClass
    public static void setup() {
        nginxReverseProxy = runNginxWithUpstreams(loginUpstreamApp);
    }

    @AfterClass
    public static void teardown() {
        nginxReverseProxy.outputNginxLogs();
    }

    @Test
    public void should_return_301_http_to_https() {
        make(getRequest("http://www.nginx-test.com").to(nginxReverseProxy))
                .andExpectResponseHeader("Location", "https://www.nginx-test.com/")
                .andExpectResponseCode(301);
    }

    @Test
    public void should_return_200_for_login() {
        make(getRequest("https://www.nginx-test.com/login").to(nginxReverseProxy))
                .andExpectResponseCode(200);
    }
}
