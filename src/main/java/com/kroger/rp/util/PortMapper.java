package com.kroger.rp.util;

import java.io.IOException;
import java.net.ServerSocket;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

class PortMapper {

    private Map<Integer, Integer> nginxToDockerPortMap = new HashMap<>();
    private Map<String, Integer> urlRegexToNginxPortMap = new HashMap<>();

    Integer getMappedPortForUrl(String url) {
        return nginxToDockerPortMap.get(getNginxPort(url));
    }

    List<String> getComposePortMapping() {
        return nginxToDockerPortMap.entrySet().stream()
                .map(entry -> entry.getValue() + ":" + entry.getKey())
                .collect(toList());
    }

    void initMapping() {
        List<Map<String, Object>> yamlMapping = TestFrameworkProperties.getNginxPortMapping();
        urlRegexToNginxPortMap = yamlMapping.stream()
                .collect(toMap(mapping -> valueOf(getActualMappingForPort(mapping).get("pattern")),
                               mapping -> parseInt(valueOf(getActualMappingForPort(mapping).get("port")))));
        nginxToDockerPortMap = urlRegexToNginxPortMap.entrySet().stream()
                .collect(toMap(Map.Entry::getValue, entry -> getAvailablePort()));
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> getActualMappingForPort(Map<String, Object> singlePortMapping) {
        return (Map<String, Object>) singlePortMapping.entrySet().stream()
                    .findFirst().orElseThrow(() -> new RuntimeException("Invalid port mapping entry"))
                    .getValue();
    }

    private Integer getNginxPort(String url) {
        return urlRegexToNginxPortMap.entrySet().stream()
                    .filter(entry -> url.matches(entry.getKey()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Unable to map url request: \"" + url + "\" to known port in the yaml configuration."))
                    .getValue();
    }

    public static int getAvailablePort() {
        try {
            ServerSocket serverSocket = new ServerSocket(0);
            int localPort = serverSocket.getLocalPort();
            serverSocket.close();
            return localPort;
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
