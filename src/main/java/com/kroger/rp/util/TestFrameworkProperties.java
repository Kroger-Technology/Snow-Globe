package com.kroger.rp.util;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;

public class TestFrameworkProperties {

    public static Map<String, Object> properties;

    static {
        initProperties("snow-globe.yaml");
        handleLoggingSettings();
    }

    private static void handleLoggingSettings() {
        if(properties.getOrDefault("snowglobe.disable.commons.logging", "false").toString().equalsIgnoreCase("true")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }
    }

    @SuppressWarnings("unchecked")
    static void initProperties(String path) {
        try {
            properties = (Map<String, Object>) new Yaml().load(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to find 'snow-globe.yaml'.  This is needed to run.");
            throw new RuntimeException(e);
        }
    }

    static void initPropertiesFromFile(String path) {
        initProperties(path);
    }

    private static boolean getBooleanValue(String key) {
        return properties.get(key) != null &&
                properties.get(key).toString().equalsIgnoreCase("true");
    }

    private static String getStringValue(String key) {
        if(properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return null;
        }
    }

    static String getFakeUpstreamImage() {
        return getStringValue("upstream.fake.container");
    }

    static boolean logContainerOutput() {
        return getBooleanValue("snowglobe.log.output");
    }

    public static boolean preserveTempFiles() {
        return getBooleanValue("snowglobe.preserve.temp.files");
    }

    public static boolean defineUpstreamZones() {
        return getBooleanValue("nginx.define.upstream.zones");
    }

    static String getNginxImage() {
        return getStringValue("nginx.container");
    }

    static String getUpstreamLocation(String environment) {
        return getEnvironmentString(environment, "nginx.upstream.file.path");
    }

    @SuppressWarnings("unchecked")
    static List<String> getNginxVolumes(String environment) {
        return getEnvironmentList(environment, "nginx.volume.mounts");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> getNginxPortMapping() {
        return (List<Map<String, Object>>) properties.get("nginx.url.port.mapping");
    }


    public static List<String> getFilesToScan(String environment) {
        return getEnvironmentList(environment, "nginx.env.config.files");
    }

    @SuppressWarnings("unchecked")
    private static List<String> getEnvironmentList(String environment, String key) {
        try {
            Map<String, Object> configMap = (Map<String, Object>) properties.get(key);
            return (configMap.containsKey(environment)) ?
                        (List<String>) configMap.get(environment) :
                        (List<String>) configMap.get("default");
        } catch (Exception e) {
            return null;
        }
    }

    private static String getEnvironmentString(String environment, String key) {
        try {
            Map<String, Object> configMap = (Map<String, Object>) properties.get(key);
            return (configMap.containsKey(environment)) ?
                    valueOf(configMap.get(environment)) :
                    valueOf(configMap.get("default"));
        } catch (Exception e) {
            return null;
        }
    }

    public static String getSourceDirectory() {
        return getStringValue("nginx.source.base.directory");
    }

    public static String getDeployedDirectory() {
        return getStringValue("nginx.deploy.base.directory");
    }

    @SuppressWarnings("unchecked")
    public static String getStartCommand(String environment) {
        Map<String, List<String>> startCommands = (Map<String, List<String>>) properties.get("nginx.start.command");
        if(!startCommands.containsKey(environment)) {
            environment = "default";
        }
        return startCommands.get(environment).stream().collect(joining(" "));
    }
}
