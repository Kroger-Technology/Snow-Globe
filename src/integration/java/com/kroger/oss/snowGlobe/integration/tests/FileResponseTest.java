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

package com.kroger.oss.snowGlobe.integration.tests;

import com.kroger.oss.snowGlobe.NginxRpBuilder;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import static com.kroger.oss.snowGlobe.NginxRpBuilder.startNginxRpWithCluster;
import static com.kroger.oss.snowGlobe.call.CallUtility.make;
import static com.kroger.oss.snowGlobe.call.TestRequest.getRequest;

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
                .andExpectResponseBodyMatchesFileContents("src/integration/resources/static/static.html");
    }

    @Test
    public void should_have_response_match_string() {
        make(getRequest("https://www.nginx-test.com/body").to(nginxReverseProxy))
                .andExpectResponseBodyContent("<html><head>Head</head><body><div>shoulders</div><div>knees</div></body><footer>toes</footer></html>");
    }
}
