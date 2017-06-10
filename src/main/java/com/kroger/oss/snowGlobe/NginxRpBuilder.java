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

import com.kroger.oss.snowGlobe.util.UpstreamUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;
import java.util.stream.Collectors;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

/**
 * This is the "main" class to define the nginx setup.  This is typically used with <code>AppServiceCluster</code> to
 * define an nginx instance with the fake upstream servers.  This setup will allow a user to define a little world so
 * that they can test requests being sent into it.
 *
 */
public class NginxRpBuilder {

    AppServiceCluster[] clusters;
    final File environmentFile;
    final int randomNamePrefix = GlobalRandom.getRandomPrefix();
    String environmentOverride = "default";
    ComposeUtility composeUtility;
    PortMapper portMapper = new PortMapper();
    TestFrameworkProperties testFrameworkProperties;


    /**
     * The constructor that will define the upstream servers file.  This will be populated later once the setup has been
     * defined and the cluster started.
     *
     * @param clusters
     *      Zero or more upstream clusters that will be used. These represent one or more instances in an upstream.
     */
    public NginxRpBuilder(AppServiceCluster[] clusters) {
        this.clusters = clusters;
        testFrameworkProperties = new TestFrameworkProperties();
        environmentFile = new File(new File(System.getProperty("user.dir")), "NGINX_ENV-"+ randomNamePrefix + ".conf");
        if(!testFrameworkProperties.preserveTempFiles()) {
            environmentFile.deleteOnExit();
        }
    }

    public static NginxRpBuilder configureRp(AppServiceCluster... clusters) {
        return new NginxRpBuilder(clusters);
    }

    public static NginxRpBuilder startNginxRpWithCluster(AppServiceCluster... clusters) {
        NginxRpBuilder reverseProxy = configureRp(clusters);
        return reverseProxy.start();
    }

    public static NginxRpBuilder startNginxRpInEnvWithCluster(String environmentOverride, AppServiceCluster... clusters) {
        NginxRpBuilder reverseProxy = configureRp(clusters);
        return reverseProxy.withEnvOverrides(environmentOverride).start();
    }

    public NginxRpBuilder withEnvOverrides(String environment) {
        this.environmentOverride = environment;
        return this;
    }

    public NginxRpBuilder start() {
        buildCopyOfServices();
        portMapper.initMapping(testFrameworkProperties);
        UpstreamUtil.setupUpstreamService();
        UpstreamUtil.initializeUpstreamInstances(clusters);
        buildEnvironmentFile();
        composeUtility = new ComposeUtility(this, testFrameworkProperties, clusters);
        composeUtility.start();
        return this;
    }

    private void startNginxContainer() {

    }


    private void buildCopyOfServices() {
        stream(clusters).map(AppServiceCluster::clone).collect(toList()).toArray(clusters);
    }

    public String buildRpContainerId() {
        return "RP-" + randomNamePrefix;
    }

    public String getRpNetworkName() {
        return "rp" + randomNamePrefix + "_default";
    }

    private void buildEnvironmentFile() {
        try {
            if (environmentFile.exists()) {
                environmentFile.delete();
                environmentFile.createNewFile();
            }
            String contents = buildFileContents();
            PrintWriter pw = new PrintWriter(environmentFile);
            pw.write(contents);
            pw.close();
        } catch(IOException e) {
            throw new RuntimeException(e);
        }
    }

    private String buildFileContents() {
        NginxEnvironmentFileBuilder builder = new NginxEnvironmentFileBuilder();
        if(hasFilesToScan()) {
            testFrameworkProperties.getFilesToScan(environmentOverride).stream()
                    .forEach(additionalFile ->
                            builder.readEnvConfig(System.getProperty("user.dir") + additionalFile));
        }
        stream(clusters).forEach(builder::addUpstreamServer);
        return builder.buildClusterFileContents();
    }

    private boolean hasFilesToScan() {
        return testFrameworkProperties.getFilesToScan(environmentOverride) != null;
    }

    public void stop() {
        if(testFrameworkProperties.logContainerOutput()) {
            logContainerOutput(buildRpContainerId());
        }
        if(composeUtility != null) {
            composeUtility.stop();
        }
    }

    private void logContainerOutput(String containerName) {
        try {
            System.out.println("\n\tLogging output for container: " + containerName);
            System.out.println("----------------------------------------------------");
            ProcessBuilder processBuilder = new ProcessBuilder("docker", "logs", containerName);
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
            System.out.println("----------------------------------------------------");
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public Map<String, Object> buildComposeMap(List<AppServiceCluster> serviceClusters) {
        Map<String, Object> composeMap = new HashMap<>();
        Map<String, Object> argsMap = new HashMap<>();
        composeMap.put(buildRpContainerId(), argsMap);
        argsMap.put("container_name", buildRpContainerId());
        argsMap.put("image", testFrameworkProperties.getNginxImage());
        argsMap.put("volumes", buildComposeVolumes());
        argsMap.put("ports", buildComposePorts());
        argsMap.put("command", getStartCommand());
        return composeMap;
    }

    protected Map<String, Object> buildDependenciesStartupMap(List<AppServiceCluster> serviceClusters) {
        Map<String, Object> composeMap = new HashMap<>();
        Map<String, Object> argsMap = new HashMap<>();
        composeMap.put(buildStartupContainerId(), argsMap);
        argsMap.put("container_name", buildStartupContainerId());
        argsMap.put("environment", buildStartupEnvironment());
        argsMap.put("image", testFrameworkProperties.getStartupImage());
        argsMap.put("depends_on", getServiceContainerNames(serviceClusters));
        argsMap.put("command", buildStartupCommand(serviceClusters));
        return composeMap;
    }

    private String[] buildStartupEnvironment() {
        return new String[]{"SLEEP_LENGTH=" + testFrameworkProperties.getStartupPollTime()};
    }

    protected String buildStartupContainerId() {
        return "startup-" + randomNamePrefix;
    }

    protected String buildStartupCommand(List<AppServiceCluster> serviceClusters) {
        return serviceClusters.stream()
                .map(AppServiceCluster::getAppInstanceInfos)
                .flatMap(Collection::stream)
                .map(upstreamAppInfo -> upstreamAppInfo.containerName() + ":" + upstreamAppInfo.port())
                .collect(Collectors.joining(" "));
    }

    private List<String> buildComposeVolumes() {
        List<String> nginxVolumes = new ArrayList<>();
        nginxVolumes.addAll(buildNginxVolumeMounts());
        nginxVolumes.add(buildEnvironmentFileMapping());
        return nginxVolumes.stream()
                .map(volume -> (volume.startsWith("/") ? "." : "./") + volume + ":ro")
                .collect(toList());
    }

    private List<String> buildNginxVolumeMounts() {
        List<String> allVolumeMounts = new ArrayList<>();
        allVolumeMounts.addAll(testFrameworkProperties.getNginxVolumes(environmentOverride).stream().filter(s -> !s.contains("*")).collect(toList()));
        testFrameworkProperties.getNginxVolumes(environmentOverride).stream()
                .filter(s -> s.contains("*"))
                .map(this::processMountWildCard)
                .forEach(allVolumeMounts::addAll);
        return allVolumeMounts;
    }

    private List<String> processMountWildCard(String wildCardMount) {
        String destinationDirectoy = wildCardMount.split(":")[1];
        String wildCardDirectory = wildCardMount.split(":")[0].replaceAll("\\*", "");
        try {
            return stream(new File(wildCardDirectory).listFiles())
                    .filter(file -> !file.isDirectory())
                    .map(file -> file.getPath())
                    .map(filePath -> buildDynamicVolumeMount(destinationDirectoy, filePath))
                    .collect(toList());
        } catch(Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildDynamicVolumeMount(String destinationDirectoy, String filePath) {
        String rawMount = filePath + ":" + destinationDirectoy + filePath.substring(filePath.lastIndexOf("/"));
        return rawMount.replaceAll("//", "/");
    }

    private String buildEnvironmentFileMapping() {
        return environmentFile.getName() + ":" + testFrameworkProperties.getUpstreamLocation(environmentOverride);
    }

    private List<String> getServiceContainerNames(List<AppServiceCluster> serviceClusters) {
        return serviceClusters.stream()
                .map(AppServiceCluster::getInstanceNames)
                .flatMap(Collection::stream)
                .collect(toList());
    }

    private List<String> buildComposePorts() {
        return portMapper.getComposePortMapping();
    }

    public Integer getPortForUrl(String url) {
        return portMapper.getMappedPortForUrl(url);
    }

    public String getStartCommand() {
        return testFrameworkProperties.getStartCommand(environmentOverride);
    }
}
