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

package com.kroger.rp.util;

import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.*;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

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
        startDockerCompose();
    }

    private String getComposeFileName() {
        return nginxRpBuilder.buildRpContainerId() + "-compose.yml";
    }

    private void writeComposeFile(String fileContents, TestFrameworkProperties testFrameworkProperties) {
        File composeFile = new File(getComposeFileName());
        if(!testFrameworkProperties.preserveTempFiles()) {
            composeFile.deleteOnExit();
        }
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(getComposeFileName()))) {
            writer.write(fileContents);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void stop() {
        try {
            List<String> serviceNames = getServiceNames();
            serviceNames.add(nginxRpBuilder.buildRpContainerId());

//            serviceNames.stream().parallel().forEach(serviceName -> {
//                ProcessBuilder disconnectProcess = new ProcessBuilder("docker", "network", "disconnect", nginxRpBuilder.getRpNetworkName(), serviceName);
//                if(testFrameworkProperties.logContainerOutput()) {
//                    disconnectProcess.inheritIO();
//                }
//                try {
//                    Process start = disconnectProcess.start();
//                    start.waitFor();
//                } catch (IOException e) {
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//                }
//            });

            serviceNames.stream().parallel().forEach(serviceName -> {
                ProcessBuilder shutdownProcess = new ProcessBuilder("docker", "rm", "-f", serviceName);
                if(testFrameworkProperties.logContainerOutput()) {
                    shutdownProcess.inheritIO();
                }
                try {
                    shutdownProcess.start();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            });
//            ProcessBuilder fullStopProcess = new ProcessBuilder("docker-compose", "--project-name",
//                    nginxRpBuilder.buildRpContainerId(), "--file", getComposeFileName(), "down");
//                if(testFrameworkProperties.logContainerOutput()) {
//                fullStopProcess.inheritIO();
//            }
//            try {
//                fullStopProcess.start();
//            } catch (IOException e) {
//                e.printStackTrace();
//            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startDockerCompose() {
        try {

            ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", "--file", getComposeFileName(), "up", "-d");
            if(testFrameworkProperties.logContainerOutput()) {
                processBuilder.inheritIO();
            }
            Process process = processBuilder.start();
            process.waitFor();
            waitForServicesToStart();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void waitForServicesToStart() {
       getServiceNames().stream().forEach(this::waitForServiceToStart);
    }

    private void waitForServiceToStart(String serviceName) {
        try {
            Process serviceProcess = new ProcessBuilder("docker", "logs", "-f", serviceName).start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(serviceProcess.getInputStream()));
            String line = reader.readLine();
            boolean gotStartedOutput = (line != null && line.contains("app listening"));
            int retries = 0;
            while(!gotStartedOutput) {
                retries++;
                line = reader.readLine();
                gotStartedOutput = (line != null && line.contains("app listening"));
                if(retries > 80) {
                    throw new RuntimeException("Timeout waiting on: " + serviceName + " to start.");
                }
                Thread.sleep(100);
            }
            serviceProcess.destroyForcibly();
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildComposeFileContents() {
        Map<String, Object> composeYaml = new HashMap<>();
        String prefix = "version: '2'\n\n";
        composeYaml.put("services", buildServicesMap());
        String body = new Yaml(buildDumperOptions()).dump(composeYaml) + "\n\n";
        return prefix + body;
    }

    private Map<String, Object> buildServicesMap() {
        Map<String, Object> nginxServiceMap = buildNginxServiceMap();
        Map<String, Object> allServicesMap = buildUpstreamsMap();
        allServicesMap.putAll(nginxServiceMap);
        return allServicesMap;
    }

    private Map<String, Object> buildNginxServiceMap() {
        return nginxRpBuilder.buildComposeMap(asList(appClusters));
    }

    private DumperOptions buildDumperOptions() {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        return options;
    }

    private Map<String, Object> buildUpstreamsMap() {
        return stream(appClusters)
                    .map(c -> c.buildComposeMap(testFrameworkProperties))
                    .flatMap(m -> m.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> getServiceNames() {
        List<String> serviceNames = new ArrayList<>();
        Arrays.stream(appClusters)
                .forEach(appServiceCluster -> appServiceCluster.getAppInstanceInfos().stream()
                        .forEach(upstreamAppInfo -> serviceNames.add(upstreamAppInfo.containerName())));
        return serviceNames;
    }
}
