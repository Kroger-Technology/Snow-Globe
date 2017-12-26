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

package com.kroger.oss.snowGlobe;

import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AppServiceClusterTest {

    TestFrameworkProperties testFrameworkProperties;

    @Before
    public void init() {
        testFrameworkProperties = new TestFrameworkProperties();
    }

    @Test
    public void shouldBuildSingleHttpCluster() {
        String clusterName = "clusterName1";
        AppServiceCluster cluster = AppServiceCluster.makeHttpWebService(clusterName);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.isUseHttps(), is(false));
    }


    @Test
    public void shouldBuildSingleHttpsCluster() {
        String clusterName = "clusterName3";
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.isUseHttps(), is(true));
    }

    @Test
    public void shouldBuildClusterWithCustomResponseCode() {
        String clusterName = "clusterName";
        int customCode = 413;
        AppServiceCluster cluster = AppServiceCluster
                .makeHttpsWebService(clusterName)
                .withHttpResponses(customCode);
        assertThat(cluster.getHttpResponseCode(), is(customCode));
    }

    @Test
    public void shouldBuildClusterWithCustomResponseHeader() {
        String clusterName = "clusterName";
        String key = "key";
        String value = "value";
        AppServiceCluster cluster = AppServiceCluster
                .makeHttpsWebService(clusterName)
                .withResponseHeader(key, value);
        assertThat(cluster.getResponseHeaders(), hasEntry(key, value));
    }

    @Test
    public void shouldBuildClusterWithCustomExpectedPaths() {
        String clusterName = "clusterName";
        String expectedPath1 = "expectedPath1";
        String expectedPath2 = "expectedPath2";
        AppServiceCluster cluster = AppServiceCluster
                .makeHttpsWebService(clusterName)
                .withExpectedPaths(expectedPath1, expectedPath2);
        assertThat(cluster.getMatchingPaths(), is(expectedPath1 + "|" + expectedPath2));
    }

    @Test
    public void shouldBuildHttpsEnvironmentList() {
        String clusterName = "clusterName";
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName);
        List<String> environmentList = cluster.buildEnvironmentList();
        assertThat("INSTANCE_NUMBER=1", is(in(environmentList)));
        assertThat("CLUSTER_NAME=" + clusterName, is(in(environmentList)));
        assertThat("APP_PATHS=*", is(in(environmentList)));
        assertThat("RESPONSE_CODE=200", is(in(environmentList)));
        assertThat("USE_HTTPS=https", is(in(environmentList)));
    }

    @Test
    public void shouldBuildHttpEnvironmentList() {
        String clusterName = "clusterName";
        AppServiceCluster cluster = AppServiceCluster.makeHttpWebService(clusterName);
        List<String> environmentList = cluster.buildEnvironmentList();
        assertThat("INSTANCE_NUMBER=1", is(in(environmentList)));
        assertThat("CLUSTER_NAME=" + clusterName, is(in(environmentList)));
        assertThat("APP_PATHS=*", is(in(environmentList)));
        assertThat("RESPONSE_CODE=200", is(in(environmentList)));
        assertThat("USE_HTTPS=http", is(in(environmentList)));
    }

}