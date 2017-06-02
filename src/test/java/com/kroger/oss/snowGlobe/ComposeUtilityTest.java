package com.kroger.oss.snowGlobe;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

public class ComposeUtilityTest {

    private final String upstreamBounceApp = "UPSTREAM_BOUNCE_IMAGE";
    @Mock
    TestFrameworkProperties testFrameworkProperties;
    NginxRpBuilder nginxRpBuilder;
    AppServiceCluster[] appClusters;

    ComposeUtility composeUtility;
    private List<AppServiceCluster> clusters = new ArrayList<>();

    @Before
    public void setup() {
        initMocks(this);
        clusters.add(new AppServiceCluster("cluster1", 1, false));
        appClusters = clusters.toArray(new AppServiceCluster[1]);
        nginxRpBuilder = new NginxRpBuilder(appClusters);
        composeUtility = new ComposeUtility(nginxRpBuilder, testFrameworkProperties,appClusters);
        when(testFrameworkProperties.getUpstreamBounceImage()).thenReturn(upstreamBounceApp);
    }

    @Test
    public void shouldBuildComposeServicesCorrectly() {
        Map<String, Object> composeMap = composeUtility.buildServicesMap();
        assertThat(composeMap.get(clusters.get(0).buildContainerId(0)), is(notNullValue()));
        assertThat(composeMap.get(nginxRpBuilder.buildStartupContainerId()), is(notNullValue()));
        assertThat(composeMap.get(nginxRpBuilder.buildRpContainerId()), is(notNullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBuildRpComposeSectionCorrectly() {
        Map<String, Object> rpMap = (Map<String, Object>) composeUtility.buildServicesMap().get(nginxRpBuilder.buildRpContainerId());
        assertThat(rpMap.get("image"), is("nginx"));
        assertThat(rpMap.get("container_name"), is(nginxRpBuilder.buildRpContainerId()));
        assertThat(rpMap.get("volumes"), is(notNullValue()));
        assertThat(rpMap.get("links"), is(notNullValue()));
        assertThat(rpMap.get("ports"), is(notNullValue()));
        assertThat(rpMap.get("command"), is(notNullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBuildServicesComposeSectionCorrectly() {
        Map<String, Object> servicesMap = (Map<String, Object>) composeUtility.buildServicesMap().get(clusters.get(0).buildContainerId(0));
        assertThat(servicesMap.get("image"), is(upstreamBounceApp));
        assertThat(servicesMap.get("container_name"), is(clusters.get(0).buildContainerId(0)));
        assertThat(servicesMap.get("environment"), is(notNullValue()));
        assertThat(servicesMap.get("expose"), is(notNullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBuildStartupComposeSectionCorrectly() {
        Map<String, Object> servicesMap = (Map<String, Object>) composeUtility.buildServicesMap().get(nginxRpBuilder.buildStartupContainerId());
        assertThat(servicesMap.get("image"), is("dadarek/wait-for-dependencies"));
        assertThat(servicesMap.get("container_name"), is(nginxRpBuilder.buildStartupContainerId()));
        assertThat(servicesMap.get("depends_on"), is(notNullValue()));
        assertThat(servicesMap.get("command"), is(notNullValue()));
    }
}