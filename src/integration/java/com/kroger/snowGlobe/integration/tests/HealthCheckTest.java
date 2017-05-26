/*
 * Nginx Snow Globe
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
                .andExpectSuccessfulHealthCheck();
    }
}
