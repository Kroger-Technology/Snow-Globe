package com.kroger.rp.util;

import com.kroger.rp.util.compose.ComposeBuilder;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class NginxRpBuilder {

    private AppServiceCluster[] clusters;
    private final File environmentFile;
    private final int randomNamePrefix = Math.abs(new Random(System.currentTimeMillis()).nextInt());
    private String environmentOverride = "";
    private ComposeBuilder composeBuilder;
    private PortMapper portMapper = new PortMapper();


    public NginxRpBuilder(AppServiceCluster[] clusters) {
        this.clusters = clusters;
        environmentFile = new File(new File(System.getProperty("user.dir")), "NGINX_ENV-"+ randomNamePrefix + ".conf");
        environmentFile.deleteOnExit();
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

    public NginxRpBuilder start(){
        portMapper.initMapping();
        buildEnvironmentFile();
        composeBuilder = new ComposeBuilder(this, clusters);
        composeBuilder.start();
        return this;
    }

    public String buildRpContainerId() {
        return "RP-" + randomNamePrefix;
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
        NginxEnvironmentFileBuilder builder = new NginxEnvironmentFileBuilder(System.getProperty("user.dir") + "/src/nginx/nginx.conf");
        if(environmentOverride.length() > 0) {
            builder.readOverridesFile(System.getProperty("user.dir") + "/src/nginx/env-overrides/" + environmentOverride + "_route_overrides.conf");
        }
        stream(clusters).forEach(builder::addUpstreamServer);
        return builder.buildClusterFileContents();
    }

    public void stop() {
        if(TestFrameworkProperties.logContainerOutput()) {
            logContainerOutput(buildRpContainerId());
        }
        composeBuilder.stop();
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
        argsMap.put("image", TestFrameworkProperties.getNginxImage());
        argsMap.put("volumes", buildComposeVolumes());
        argsMap.put("ports", buildComposePorts());
        argsMap.put("links", getServiceContainerNames(serviceClusters));
        return composeMap;
    }

    private List<String> buildComposeVolumes() {
        List<String> nginxVolumes = new ArrayList<>();
        nginxVolumes.addAll(TestFrameworkProperties.getNginxVolumes());
        nginxVolumes.add(buildEnvironmentFileMapping());
        return nginxVolumes.stream()
                .map(volume -> System.getProperty("user.dir") + (volume.startsWith("/") ? "" : "/") + volume)
                .collect(toList());
    }

    private String buildEnvironmentFileMapping() {
        return environmentFile.getName() + ":" + TestFrameworkProperties.getUpstreamLocation();
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
}
