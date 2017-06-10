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

package com.kroger.oss.snowGlobe;

import org.hamcrest.Matchers;
import org.junit.After;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.hasKey;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class NginxRpBuilderTest {

    NginxRpBuilder nginxRpBuilder;

    @After
    public void teardown() {
        if (nginxRpBuilder != null) {
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
        Map<String, Object> composeMap = nginxRpBuilder.buildComposeMap();
        assertThat(composeMap, Matchers.hasKey(nginxRpBuilder.buildRpContainerId()));
        Map<String, Object> argsMap = (Map<String, Object>) composeMap.get(nginxRpBuilder.buildRpContainerId());
        assertThat(argsMap, hasKey("container_name"));
        assertThat(argsMap, hasKey("volumes"));
        assertThat(argsMap, hasKey("ports"));
        assertThat(argsMap, hasKey("links"));
        assertThat(argsMap, hasKey("command"));
    }

    @Test
    public void shouldbuildListOfDependentContainersAndPortsForStartup() {
        nginxRpBuilder = new NginxRpBuilder(null);
        List<AppServiceCluster> clusters = new ArrayList<>();
        clusters.add(new AppServiceCluster("cluster1", 1, false));
        clusters.add(new AppServiceCluster("cluster2", 2, false));
        assertThat(nginxRpBuilder.buildStartupCommand(clusters),
                is(clusters.get(0).getAppInstanceInfos().get(0).containerName() + ":"
                        + clusters.get(0).getAppInstanceInfos().get(0).port() + " "
                        + clusters.get(1).getAppInstanceInfos().get(0).containerName() + ":"
                        + clusters.get(1).getAppInstanceInfos().get(0).port() + " "
                        + clusters.get(1).getAppInstanceInfos().get(1).containerName() + ":"
                        + clusters.get(1).getAppInstanceInfos().get(1).port()));

    }

}