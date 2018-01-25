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

package com.kroger.oss.snowGlobe;

import com.kroger.oss.snowGlobe.util.ComposeUtility;
import com.kroger.oss.snowGlobe.util.ContainerUtil;
import com.kroger.oss.snowGlobe.util.UpstreamUtil;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

/**
 * This is the "main" class to define the nginx setup.  This is typically used with <code>AppServiceCluster</code> to
 * define an nginx instance with the fake upstream servers.  This setup will allow a user to define a little world so
 * that they can test requests being sent into it.
 */
public class NginxRpBuilder {

    final AppServiceCluster[] clusters;
    ComposeUtility composeUtility;
    PortMapper portMapper = new PortMapper();
    FrameworkProperties frameworkProperties;
    String configurationHash = null;
    private String upstreamFileContents;


    /**
     * The constructor that will define the upstream servers file.  This will be populated later once the setup has been
     * defined and the cluster started.
     *
     * @param clusters Zero or more upstream clusters that will be used. These represent one or more instances in an upstream.
     */
    public NginxRpBuilder(String snowGlobeConfig, AppServiceCluster[] clusters) {
        if (clusters != null && clusters.length > 0) {
            this.clusters = stream(clusters).map(AppServiceCluster::clone).collect(toList()).toArray(clusters);
        } else {
            this.clusters = new AppServiceCluster[0];
        }
        frameworkProperties = new FrameworkProperties(snowGlobeConfig);
    }


    public static NginxRpBuilder configureRp(AppServiceCluster... clusters) {
        return new NginxRpBuilder("snow-globe.yml", clusters);
    }

    public static NginxRpBuilder configureRp(String configFile, AppServiceCluster... clusters) {
        return new NginxRpBuilder(configFile, clusters);
    }

    public static NginxRpBuilder runNginxWithUpstreams(AppServiceCluster... clusters) {
        NginxRpBuilder reverseProxy = configureRp(clusters);
        return reverseProxy.start();
    }

    public static NginxRpBuilder runNginxWithUpstreams(String configFile, AppServiceCluster... clusters) {
        NginxRpBuilder reverseProxy = configureRp(configFile, clusters);
        return reverseProxy.start();
    }

    public NginxRpBuilder start() {
        analyzeNginxConfig();
        initializeUpstreamInstances();
        buildEnvironmentFile();
        composeUtility = new ComposeUtility(this, frameworkProperties);
        composeUtility.start();
        return this;
    }

    private void initializeUpstreamInstances() {
        portMapper.initMapping(frameworkProperties);
        UpstreamUtil.setupUpstreamService();
        UpstreamUtil.initializeUpstreamInstances(clusters);
    }


    public String getHashedPrefix() {
        if (configurationHash == null) {
            analyzeNginxConfig();
        }
        return configurationHash;
    }

    public String buildRpContainerId() {
        return "RP-" + getHashedPrefix();
    }

    private File getEnvironmentFile() {
        final String buildDirectory = System.getProperty("user.dir") + File.separator + "build";
        new File(buildDirectory).mkdirs();
        return new File(new File(buildDirectory), "NGINX_ENV-" + getHashedPrefix() + ".conf");
    }

    private void buildEnvironmentFile() {
        File environmentFile = getEnvironmentFile();
        if (!frameworkProperties.preserveTempFiles()) {
            environmentFile.deleteOnExit();
        }
        try {
            String contents = upstreamFileContents;
            PrintWriter pw = new PrintWriter(environmentFile);
            pw.write(contents);
            pw.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    private void analyzeNginxConfig() {
        NginxEnvironmentFileBuilder builder = new NginxEnvironmentFileBuilder();
        determineUpstreamClusters(builder);
        builder.registerUpstreams(clusters);
        builder.computeUpstreamPorts();
        upstreamFileContents = builder.buildClusterFileContents();
        configurationHash = builder.computeConfigurationHash();
        builder.setUpstreamPorts(clusters);
    }


    private void determineUpstreamClusters(NginxEnvironmentFileBuilder builder) {
        if (hasFilesToScan()) {
            frameworkProperties.getFilesToScan().stream()
                    .forEach(additionalFile ->
                            builder.readEnvConfig(System.getProperty("user.dir") + additionalFile));
        }
    }

    private boolean hasFilesToScan() {
        return frameworkProperties.getFilesToScan() != null;
    }

    public void outputNginxLogs() {
        ContainerUtil.logContainerOutput(buildRpContainerId());
    }


    public Map<String, Object> buildComposeMap() {
        Map<String, Object> composeMap = new HashMap<>();
        Map<String, Object> argsMap = new HashMap<>();
        composeMap.put(buildRpContainerId(), argsMap);
        argsMap.put("container_name", buildRpContainerId());
        argsMap.put("image", frameworkProperties.getNginxImage());
        argsMap.put("volumes", buildComposeVolumes());
        argsMap.put("restart", "always");
        argsMap.put("ports", buildComposePorts());
        argsMap.put("command", getStartCommand());
        return composeMap;
    }

    private List<String> buildComposeVolumes() {
        List<String> nginxVolumes = buildNginxVolumeMounts().stream()
                .map(volume -> (volume.startsWith("/") ? ".." : "../") + volume + ":rw")
                .collect(toList());
        nginxVolumes.add(buildEnvironmentFileMapping());
        return nginxVolumes;
    }

    private List<String> buildNginxVolumeMounts() {
        List<String> nginxVolumes = frameworkProperties.getNginxVolumes();
        return calculateVolumeMounts(nginxVolumes);
    }

    protected List<String> calculateVolumeMounts(List<String> nginxVolumes) {
        List<String> allVolumeMounts = new ArrayList<>();
        allVolumeMounts.addAll(nginxVolumes
                .stream()
                .filter(s -> !s.contains("*"))
                .filter(s -> new File(s.substring(0, s.indexOf(":"))).exists())
                .collect(toList()));
        nginxVolumes.stream()
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
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    private String buildDynamicVolumeMount(String destinationDirectoy, String filePath) {
        String rawMount = filePath + ":" + destinationDirectoy + filePath.substring(filePath.lastIndexOf("/"));
        return rawMount.replaceAll("//", "/");
    }

    private String buildEnvironmentFileMapping() {
        return "./" + getEnvironmentFile().getName() + ":" + frameworkProperties.getUpstreamLocation();
    }

    private List<String> buildComposePorts() {
        return portMapper.getComposePortMapping();
    }

    public Integer getPortForUrl(String url) {
        return portMapper.getMappedPortForUrl(url);
    }

    public String getStartCommand() {
        return frameworkProperties.getStartCommand();
    }

    public void assignPortFormRunningContainer(Map<Integer, Integer> existingPorts) {
        portMapper.useExistingPorts(existingPorts);
    }
}
