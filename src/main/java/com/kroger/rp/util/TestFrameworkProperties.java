package com.kroger.rp.util;

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static java.lang.Thread.currentThread;
import static java.util.Arrays.asList;
import static java.util.stream.Collectors.joining;

public class TestFrameworkProperties {

    public static Map<String, Object> properties;

    static {
        initProperties();
        handleLoggingSettings();
    }

    private static void handleLoggingSettings() {
        if(properties.getOrDefault("snowglobe.disable.commons.logging", "false").toString().equalsIgnoreCase("true")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }
    }

    private static String getStringValue(String key) {
        if(properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    static void initProperties() {
        try {
            properties = (Map<String, Object>) new Yaml().load(new FileInputStream("snow-globe.yaml"));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to find 'snow-globe.yaml'.  This is needed to run.");
            throw new RuntimeException(e);
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


    private static boolean getBooleanValue(String key) {
        return properties.get(key) != null &&
                properties.get(key).toString().equalsIgnoreCase("true");
    }

    static String getNginxImage() {
        return getStringValue("nginx.container");
    }

    static String getUpstreamLocation() {
        return getStringValue("nginx.upstream.file.path");
    }

    @SuppressWarnings("unchecked")
    static List<String> getNginxVolumes() {
        return (List<String>) properties.get("nginx.volume.mounts");
    }

    @SuppressWarnings("unchecked")
    static List<Map<String, Object>> getNginxPortMapping() {
        return (List<Map<String, Object>>) properties.get("nginx.url.port.mapping");
    }

    @SuppressWarnings("unchecked")
    public static List<String> getFilesToScan(String environmentOverride) {
        try {
            Map<String, Object> additionalFiles = (Map<String, Object>) properties.get("nginx.env.config.files");
            return (List<String>) additionalFiles.get(environmentOverride);
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
