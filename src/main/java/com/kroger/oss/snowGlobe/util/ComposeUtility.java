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

import com.kroger.oss.snowGlobe.FrameworkProperties;
import com.kroger.oss.snowGlobe.NginxRpBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Collections.singletonMap;

public class ComposeUtility {

    private static List<String> containersWithShutDownHooks = new ArrayList<>();
    private final NginxRpBuilder nginxRpBuilder;
    private FrameworkProperties frameworkProperties;

    public ComposeUtility(NginxRpBuilder nginxRpBuilder, FrameworkProperties frameworkProperties) {
        this.nginxRpBuilder = nginxRpBuilder;
        this.frameworkProperties = frameworkProperties;
    }

    public void start() {
        String fileContents = buildComposeFileContents();
        writeComposeFile(fileContents, frameworkProperties);
        String containerId = nginxRpBuilder.buildRpContainerId();
        if (ContainerUtil.isContainerRunning(containerId)) {
            if(frameworkProperties.getShouldRestartNginxOnEachRun()) {
                reload();
            }
            nginxRpBuilder.assignPortFormRunningContainer(ContainerUtil.getMappedPorts(containerId));
        } else {
            startReverseProxy();
            addNginxShutDownHook(containerId);
        }
    }

    /**
     * Causes the Nginx process to reload.  If a custom command is provided by the framework properties,
     * then it will be passed in, otherwise the default of "nginx", "-s", "reload" will be supplied.
     */
    public void reload() {
        String containerId = nginxRpBuilder.buildRpContainerId();
        ContainerUtil.restartNginx(containerId, 100, frameworkProperties.getNginxReloadCommand());
    }

    protected String getComposeFileName() {
        return "./build/" + nginxRpBuilder.buildRpContainerId() + "-compose.yml";
    }

    private void writeComposeFile(String fileContents, FrameworkProperties frameworkProperties) {
        File composeFile = new File(getComposeFileName());
        if (!frameworkProperties.preserveTempFiles()) {
            composeFile.deleteOnExit();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getComposeFileName()))) {
            writer.write(fileContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void addNginxShutDownHook(String runningContainer) {
        final boolean logShutdown = frameworkProperties.logContainerOutput();
        if (!containersWithShutDownHooks.contains(runningContainer)) {
            containersWithShutDownHooks.add(runningContainer);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                if (logShutdown) {
                    ContainerUtil.shutdownContainerWithLogs(runningContainer);
                } else {
                    ContainerUtil.shutdownContainer(runningContainer);
                }
            }));
        }
    }

    private void startReverseProxy() {
        String[] command = {"docker-compose", "--file", getComposeFileName(), "up", "-d",
                nginxRpBuilder.buildRpContainerId()};
        if (frameworkProperties.logContainerOutput()) {
            ContainerUtil.runCommandWithLogs(command);
        } else {
            ContainerUtil.runCommand(command);
        }
    }

    protected String buildComposeFileContents() {
        Map<String, Object> composeYaml = new HashMap<>();
        String prefix = "version: '2'\n\n";
        composeYaml.put("services", nginxRpBuilder.buildComposeMap());
        composeYaml.put("networks", buildNetworks());
        String body = new Yaml(buildDumperOptions()).dump(composeYaml) + "\n\n";
        return prefix + body;
    }

    private Map<String, Object> buildNetworks() {
        return singletonMap("default", singletonMap("external", singletonMap("name", frameworkProperties.getDockerNetworkName())));
    }

    protected Map<String, Object> buildServicesMap() {
        return nginxRpBuilder.buildComposeMap();
    }

    private DumperOptions buildDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return options;
    }
}
