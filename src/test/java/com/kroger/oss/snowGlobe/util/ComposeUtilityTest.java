package com.kroger.oss.snowGlobe.util;

import com.kroger.oss.snowGlobe.AppServiceCluster;
import com.kroger.oss.snowGlobe.NginxRpBuilder;
import com.kroger.oss.snowGlobe.TestFrameworkProperties;
import com.kroger.oss.snowGlobe.util.ComposeUtility;
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
        nginxRpBuilder = new NginxRpBuilder("snow-globe.yml", appClusters);
        composeUtility = new ComposeUtility(nginxRpBuilder, testFrameworkProperties,appClusters);
        when(testFrameworkProperties.getUpstreamBounceImage()).thenReturn(upstreamBounceApp);
    }

    @Test
    public void shouldBuildComposeServicesCorrectly() {
        Map<String, Object> composeMap = composeUtility.buildServicesMap();
        assertThat(composeMap.get(nginxRpBuilder.buildRpContainerId()), is(notNullValue()));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void shouldBuildRpComposeSectionCorrectly() {
        Map<String, Object> rpMap = (Map<String, Object>) composeUtility.buildServicesMap().get(nginxRpBuilder.buildRpContainerId());
        assertThat(rpMap.get("image"), is("nginx"));
        assertThat(rpMap.get("container_name"), is(nginxRpBuilder.buildRpContainerId()));
        assertThat(rpMap.get("volumes"), is(notNullValue()));
        assertThat(rpMap.get("ports"), is(notNullValue()));
        assertThat(rpMap.get("command"), is(notNullValue()));
    }
}