package com.kroger.rp.util;

import org.junit.After;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class NginxRpBuilderTest {

    NginxRpBuilder nginxRpBuilder;

    @After
    public void teardown() {
        if(nginxRpBuilder != null) {
            nginxRpBuilder.stop();
        }
    }

    @Test
    public void shouldGetRPInstanceName() {
        nginxRpBuilder = new NginxRpBuilder(null);
        int randomSuffix = nginxRpBuilder.randomNamePrefix;
        assertThat(nginxRpBuilder.buildRpContainerId(), is("RP-" + randomSuffix));
    }

    @Test
    public void shouldBeAbleToSetAnEnvironment() {
        String environmentOverride = "non-prod";
        nginxRpBuilder = new NginxRpBuilder(null).withEnvOverrides(environmentOverride);
        assertThat(nginxRpBuilder.environmentOverride, is(environmentOverride));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldBuildComposeInformation() {
        nginxRpBuilder = new NginxRpBuilder(null);
        List<AppServiceCluster> clusters = new ArrayList<>();
        clusters.add(new AppServiceCluster("cluster1", 1, false));
        clusters.add(new AppServiceCluster("cluster2", 1, false));
        Map<String, Object> composeMap = nginxRpBuilder.buildComposeMap(clusters);
        assertThat(composeMap, hasKey(nginxRpBuilder.buildRpContainerId()));
        Map<String, Object> argsMap = (Map<String, Object>) composeMap.get(nginxRpBuilder.buildRpContainerId());
        assertThat(argsMap, hasKey("container_name"));
        assertThat(argsMap, hasKey("volumes"));
        assertThat(argsMap, hasKey("ports"));
        assertThat(argsMap, hasKey("links"));
        assertThat(argsMap, hasKey("command"));
    }

}