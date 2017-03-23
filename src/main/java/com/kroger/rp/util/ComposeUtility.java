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
import java.util.stream.Collectors;

import static java.util.Arrays.asList;
import static java.util.Arrays.stream;

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
            serviceNames.stream().parallel().forEach(this::shutDownService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void shutDownService(String serviceName) {
        ProcessBuilder shutdownProcess = new ProcessBuilder("docker", "rm", "-f", serviceName);
        if(testFrameworkProperties.logContainerOutput()) {
            shutdownProcess.inheritIO();
        }
        try {
            shutdownProcess.start();
        } catch (IOException e) {
            e.printStackTrace();
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
            boolean gotStartedOutput = isAppStarted(getServiceLogs(serviceName));
            int retries = 0;
            while(!gotStartedOutput) {
                retries++;
                gotStartedOutput = (isAppStarted(getServiceLogs(serviceName)));
                int maxRetries = testFrameworkProperties.getServiceStartupTimeout() * 10;
                if(retries > maxRetries) {
                    throw new RuntimeException("Timeout waiting on: " + serviceName + " to start.\n" +
                            "\tApp Service Output:\n\n" + getServiceLogs(serviceName));
                }
                Thread.sleep(100);
            }
        } catch(Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean isAppStarted(String logs) {
        return logs != null && logs.contains("app listening");
    }

    private String getServiceLogs(String serviceName) throws Exception{
        Process serviceProcess = new ProcessBuilder("docker", "logs", serviceName).start();
        InputStream inputStream = serviceProcess.getInputStream();
        InputStream errorStream = serviceProcess.getErrorStream();
        String output = getProcessInput(serviceProcess, inputStream);
        String stdErr = getProcessInput(serviceProcess, errorStream);
        if(stdErr != null && stdErr.trim().length() > 0) {
            output += "\n\tError stream:\n-------------------\n";
            output += stdErr;
        }
        return output;
    }

    private String getProcessInput(Process serviceProcess, InputStream inputStream) throws InterruptedException, IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
        serviceProcess.waitFor();
        StringBuilder builder = new StringBuilder();
        String line;
        while ( (line = reader.readLine()) != null) {
            builder.append(line);
            builder.append(System.getProperty("line.separator"));
        }
        return builder.toString();
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
