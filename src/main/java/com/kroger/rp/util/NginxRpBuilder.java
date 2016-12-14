package com.kroger.rp.util;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.*;

import static com.kroger.rp.util.TestFrameworkProperties.getFilesToScan;
import static com.kroger.rp.util.TestFrameworkProperties.getNginxVolumes;
import static com.kroger.rp.util.TestFrameworkProperties.preserveTempFiles;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class NginxRpBuilder {

    private AppServiceCluster[] clusters;
    private final File environmentFile;
    private final int randomNamePrefix = Math.abs(new Random(System.currentTimeMillis()).nextInt());
    private String environmentOverride = "default";
    private ComposeUtility composeUtility;
    private PortMapper portMapper = new PortMapper();


    public NginxRpBuilder(AppServiceCluster[] clusters) {
        this.clusters = clusters;
        environmentFile = new File(new File(System.getProperty("user.dir")), "NGINX_ENV-"+ randomNamePrefix + ".conf");
        if(!preserveTempFiles()) {
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

    public NginxRpBuilder start(){
        portMapper.initMapping();
        buildEnvironmentFile();
        composeUtility = new ComposeUtility(this, clusters);
        composeUtility.start();
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
        NginxEnvironmentFileBuilder builder = new NginxEnvironmentFileBuilder();
        if(hasFilesToScan()) {
            getFilesToScan(environmentOverride).stream()
                    .forEach(additionalFile ->
                            builder.readEnvConfig(System.getProperty("user.dir") + additionalFile));
        }
        stream(clusters).forEach(builder::addUpstreamServer);
        return builder.buildClusterFileContents();
    }

    private boolean hasFilesToScan() {
        return getFilesToScan(environmentOverride) != null;
    }

    public void stop() {
        if(TestFrameworkProperties.logContainerOutput()) {
            logContainerOutput(buildRpContainerId());
        }
        composeUtility.stop();
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
        argsMap.put("command", getStartCommand());
        return composeMap;
    }

    private List<String> buildComposeVolumes() {
        List<String> nginxVolumes = new ArrayList<>();
        nginxVolumes.addAll(buildNginxVolumeMounts());
        nginxVolumes.add(buildEnvironmentFileMapping());
        return nginxVolumes.stream()
                .map(volume -> System.getProperty("user.dir") + (volume.startsWith("/") ? "" : "/") + volume)
                .collect(toList());
    }

    private List<String> buildNginxVolumeMounts() {
        List<String> allVolumeMounts = new ArrayList<>();
        allVolumeMounts.addAll(getNginxVolumes().stream().filter(s -> !s.contains("*")).collect(toList()));
        getNginxVolumes().stream()
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

    public String getStartCommand() {
        return TestFrameworkProperties.getStartCommand(environmentOverride);
    }
}
