package com.kroger.rp.util;

import org.junit.Test;

import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonList;
import static org.hamcrest.collection.IsIn.in;
import static org.hamcrest.collection.IsIn.isIn;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class AppServiceClusterTest {

    @Test
    public void shouldBuildSingleHttpCluster() {
        String clusterName = "clusterName1";
        AppServiceCluster cluster = AppServiceCluster.makeHttpWebService(clusterName);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.getInstances(), is(1));
        assertThat(cluster.isUseHttps(), is(false));
    }

    @Test
    public void shouldBuildMultipleHttpCluster() {
        String clusterName = "clusterName2";
        int instances = 10;
        AppServiceCluster cluster = AppServiceCluster.makeHttpWebService(clusterName, instances);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.getInstances(), is(instances));
        assertThat(cluster.isUseHttps(), is(false));
    }

    @Test
    public void shouldBuildSingleHttpsCluster() {
        String clusterName = "clusterName3";
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.getInstances(), is(1));
        assertThat(cluster.isUseHttps(), is(true));
    }

    @Test
    public void shouldBuildMultipleHttpsCluster() {
        String clusterName = "clusterName4";
        int instances = 10;
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName, instances);
        assertThat(cluster.getClusterName(), is(clusterName));
        assertThat(cluster.getInstances(), is(instances));
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
    public void shouldBuildComposeMap() {
        String clusterName = "clusterName";
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName);
        Map<String, Object> composeMap = (Map<String, Object>) cluster.buildComposeMap()
                .get(cluster.buildContainerId(0));
        assertThat(composeMap, hasEntry("container_name", cluster.buildContainerId(0)));
        assertThat(composeMap, hasEntry("expose", singletonList(3000)));
    }

    @Test
    public void shouldBuildHttpsEnvironmentList() {
        String clusterName = "clusterName";
        AppServiceCluster cluster = AppServiceCluster.makeHttpsWebService(clusterName);
        List<String> environmentList = cluster.buildEnvironmentList(1);
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
        List<String> environmentList = cluster.buildEnvironmentList(1);
        assertThat("INSTANCE_NUMBER=1", is(in(environmentList)));
        assertThat("CLUSTER_NAME=" + clusterName, is(in(environmentList)));
        assertThat("APP_PATHS=*", is(in(environmentList)));
        assertThat("RESPONSE_CODE=200", is(in(environmentList)));
        assertThat("USE_HTTPS=http", is(in(environmentList)));
    }

}