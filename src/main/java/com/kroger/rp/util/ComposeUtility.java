package com.kroger.rp.util;


import com.kroger.rp.util.AppServiceCluster;
import com.kroger.rp.util.NginxRpBuilder;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import static com.kroger.rp.util.TestFrameworkProperties.preserveTempFiles;
import static java.util.Arrays.asList;
import static java.util.Arrays.stream;
import static java.util.stream.Collectors.toList;

public class ComposeUtility {

    private final NginxRpBuilder nginxRpBuilder;
    private final AppServiceCluster[] appClusters;

    public ComposeUtility(NginxRpBuilder nginxRpBuilder, AppServiceCluster... appClusters) {
        this.nginxRpBuilder = nginxRpBuilder;
        this.appClusters = appClusters;
    }

    public void start() {
        String fileContents = buildComposeFileContents();
        writeComposeFile(fileContents);
        startDockerCompose();
    }

    private String getComposeFileName() {
        return nginxRpBuilder.buildRpContainerId() + "-compose.yml";
    }

    private void writeComposeFile(String fileContents) {
        File composeFile = new File(getComposeFileName());
        if(!preserveTempFiles()) {
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
            getServiceNames().parallelStream().map(serviceName -> {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder("docker", "rm", "-f", serviceName);
                    processBuilder.start();
                    Thread.sleep(100); // wait a little for the command to kick off and then exit.
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                return "done";
            }).collect(toList());
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void startDockerCompose() {
        try {

            ProcessBuilder processBuilder = new ProcessBuilder("docker-compose", "--file", getComposeFileName(),
                    "up", "-d", "--remove-orphans");
            processBuilder.inheritIO();
            Process process = processBuilder.start();
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String buildComposeFileContents() {
        Map<String, Object> composeYaml = new HashMap<>();
        composeYaml.put("version", "2");
        composeYaml.put("services", buildServicesMap());
        return new Yaml(buildDumperOptions()).dump(composeYaml);
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
                    .map(AppServiceCluster::buildComposeMap)
                    .flatMap(m -> m.entrySet().stream())
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    private List<String> getServiceNames() {
        List<String> serviceNames = new ArrayList<>();
        serviceNames.add(nginxRpBuilder.buildRpContainerId());
        Arrays.stream(appClusters)
                .forEach(appServiceCluster -> appServiceCluster.getAppInstanceInfos().stream()
                        .forEach(upstreamAppInfo -> serviceNames.add(upstreamAppInfo.containerName())));
        return serviceNames;
    }
}
