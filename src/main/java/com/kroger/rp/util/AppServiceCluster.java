package com.kroger.rp.util;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static com.kroger.rp.util.TestFrameworkProperties.getFakeUpstreamImage;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;

public class AppServiceCluster {

    private final int randomNamePrefix = Math.abs(new Random(System.currentTimeMillis()).nextInt());
    private final String clusterName;
    private final int instances;
    private List<Integer> ports = new ArrayList<>();
    private int httpResponseCode = 200;
    private String matchingPaths = "*";
    private Map<String, String> responseHeaders = new HashMap<>();
    private final boolean useHttps;

    public static AppServiceCluster makeHttpWebService(String clusterName) {
        return new AppServiceCluster(clusterName, 1, false);
    }

    public static AppServiceCluster makeHttpWebService(String clusterName, int instances) {
        return new AppServiceCluster(clusterName, instances, false);
    }

    public static AppServiceCluster makeHttpsWebService(String clusterName) {
        return new AppServiceCluster(clusterName, 1, true);
    }

    public static AppServiceCluster makeHttpsWebService(String clusterName, int instances) {
        return new AppServiceCluster(clusterName, instances, true);
    }

    public AppServiceCluster(String clusterName, int instances, boolean useHttps) {
        this.clusterName = clusterName;
        this.instances = instances;
        this.useHttps = useHttps;
    }

    public AppServiceCluster withHttpResponses(int httpResponseCode) {
        this.httpResponseCode = httpResponseCode;
        return this;
    }

    public AppServiceCluster start() {
        String startCommand = System.getProperty("user.dir") + "/src/test/resources/startService.sh";
        String commandPath = System.getProperty("user.dir") + "/src/test/resources/";
        IntStream.range(0, instances).parallel().forEach(instance -> {
            try {
                int port = ContainerUtil.getAvailablePort();
                ports.add(port);
                startServiceContainer(startCommand, commandPath, instance, port);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        });
        return this;
    }

    private void startServiceContainer(String startCommand, String commandPath, int instance, int port) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(startCommand,
                buildContainerId(instance),
                clusterName,
                Integer.toString(instance),
                Integer.toString(httpResponseCode),
                matchingPaths,
                Integer.toString(port),
                (useHttps ? "https" : "http"));
        processBuilder.directory(new File(commandPath));
        processBuilder.redirectOutput(ProcessBuilder.Redirect.INHERIT);
        Process process = processBuilder.start();
        process.waitFor();
    }

    /**
     * Wraps all headers in quotes for argument on parameter.  Also does the horrible escape of quotes.
     *
     * @return
     */
    private String getHeadersAsArgument() {
        try {
            return "\"" + new ObjectMapper().writeValueAsString(responseHeaders).replaceAll("\"", "\\\\\"") + "\"";
        } catch (IOException e) {
            System.err.println("Unable to parse resposne header for upstream application");
            return "";
        }
    }

    public AppServiceCluster withExpectedPaths(String... matchingPaths) {
        this.matchingPaths = stream(matchingPaths).reduce((s, s2) -> s + "|" + s2).get();
        return this;
    }

    private String buildContainerId(int instance) {
        return "CLUSTER-" + randomNamePrefix + "-" + clusterName + "-" + Integer.toString(instance);
    }

    public String getClusterName() {
        return this.clusterName;
    }

    public AppServiceCluster withResponseHeader(String key, String value) {
        responseHeaders.put(key, value);
        return this;
    }

    public Map<String, Object> buildComposeMap() {
        Map<String, Object> composeMap = new HashMap<>();
        IntStream.range(0, instances).forEach(instance -> {
            Map<String, Object> appArgsMap = new HashMap<>();
            appArgsMap.put("container_name", buildContainerId(instance));
            appArgsMap.put("image", getFakeUpstreamImage());
            appArgsMap.put("environment", buildEnvironmentList(instance));
            appArgsMap.put("expose", singletonList(3000));
            composeMap.put(buildContainerId(instance), appArgsMap);
        });
        return composeMap;
    }

    private List<String> buildEnvironmentList(int instance) {
        List<String> environmentVariables = new ArrayList<>();
        environmentVariables.add("INSTANCE_NUMBER=" + instance);
        environmentVariables.add("CLUSTER_NAME=" + getClusterName());
        environmentVariables.add("APP_PATHS=" + matchingPaths);
        environmentVariables.add("RESPONSE_CODE=" + httpResponseCode);
        environmentVariables.add("RESPONSE_HEADERS=" + getHeadersAsArgument() + "");
        environmentVariables.add("USE_HTTPS=" + (useHttps ? "https" : "http"));
        return environmentVariables;
    }

    List<String> getInstanceNames() {
        return IntStream.range(0, instances)
                .mapToObj(this::buildContainerId)
                .collect(Collectors.toList());
    }

    public List<UpstreamAppInfo> getAppInstanceInfos() {
        return IntStream.range(0, instances)
                .mapToObj(instance -> new UpstreamAppInfo(buildContainerId(instance), 3000))
                .collect(Collectors.toList());
    }
}
