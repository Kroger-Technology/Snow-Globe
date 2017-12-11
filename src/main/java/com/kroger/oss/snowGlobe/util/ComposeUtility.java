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

package com.kroger.oss.snowGlobe.util;

import com.kroger.oss.snowGlobe.AppServiceCluster;
import com.kroger.oss.snowGlobe.NginxRpBuilder;
import com.kroger.oss.snowGlobe.TestFrameworkProperties;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class ComposeUtility {

    private final NginxRpBuilder nginxRpBuilder;
    private TestFrameworkProperties testFrameworkProperties;
    private final AppServiceCluster[] appClusters;

    public ComposeUtility(NginxRpBuilder nginxRpBuilder, TestFrameworkProperties testFrameworkProperties, AppServiceCluster... appClusters) {
        this.nginxRpBuilder = nginxRpBuilder;
        this.testFrameworkProperties = testFrameworkProperties;
        this.appClusters = appClusters;
    }

    public void start() {
        String fileContents = buildComposeFileContents();
        writeComposeFile(fileContents, testFrameworkProperties);
        startReverseProxy();
    }

    protected String getComposeFileName() {
        return "./build/" + nginxRpBuilder.buildRpContainerId() + "-compose.yml";
    }

    private void writeComposeFile(String fileContents, TestFrameworkProperties testFrameworkProperties) {
        File composeFile = new File(getComposeFileName());
        if (!testFrameworkProperties.preserveTempFiles()) {
            composeFile.deleteOnExit();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getComposeFileName()))) {
            writer.write(fileContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        if (testFrameworkProperties.logContainerOutput()) {
            ContainerUtil.shutdownContainerWithLogs(nginxRpBuilder.buildRpContainerId());
        } else {
            ContainerUtil.shutdownContainer(nginxRpBuilder.buildRpContainerId());
        }
        Arrays.stream(appClusters).forEach(appCluster ->
                appCluster.getRunningPorts().forEach(UpstreamUtil::stopUpstream));
    }

    private void startReverseProxy() {
        String[] command = {"docker-compose", "--file", getComposeFileName(), "up", "-d",
                nginxRpBuilder.buildRpContainerId()};
        if (testFrameworkProperties.logContainerOutput()) {
            ContainerUtil.runCommandWithLogs(command);
        } else {
            ContainerUtil.runCommand(command);
        }
    }

    protected String buildComposeFileContents() {
        Map<String, Object> composeYaml = new HashMap<>();
        String prefix = "version: '2'\n\n";
        composeYaml.put("services", buildNginxServiceMap());
        composeYaml.put("networks", buildNetworks());
        String body = new Yaml(buildDumperOptions()).dump(composeYaml) + "\n\n";
        return prefix + body;
    }

    private Map<String, Object> buildNetworks() {
        return singletonMap("default", singletonMap("external", singletonMap("name", testFrameworkProperties.getDockerNetworkName())));
    }

    protected Map<String, Object> buildServicesMap() {
        Map<String, Object> nginxServiceMap = buildNginxServiceMap();
        return nginxServiceMap;
    }

    private Map<String, Object> buildNginxServiceMap() {
        return nginxRpBuilder.buildComposeMap();
    }

    private DumperOptions buildDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return options;
    }
}
