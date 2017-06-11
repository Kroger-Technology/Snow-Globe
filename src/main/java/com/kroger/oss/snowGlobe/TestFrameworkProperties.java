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

import org.yaml.snakeyaml.Yaml;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.List;
import java.util.Map;

import static java.lang.Integer.parseInt;
import static java.lang.String.valueOf;
import static java.util.stream.Collectors.joining;

public class TestFrameworkProperties {

    public Map<String, Object> properties;

    private static String overrideYmlConfigurationFile;

    public static void setConfigFile(String configFile) {
        overrideYmlConfigurationFile = configFile;
    }

    public TestFrameworkProperties() {
        if(overrideYmlConfigurationFile != null) {
            loadFile(overrideYmlConfigurationFile);
        } else {
            loadFile("snow-globe.yml");
        }
        handleLoggingSettings();
    }

    private void handleLoggingSettings() {
        if(properties.getOrDefault("snowglobe.disable.commons.logging", "false").toString().equalsIgnoreCase("true")) {
            System.setProperty("org.apache.commons.logging.Log", "org.apache.commons.logging.impl.NoOpLog");
        }
    }

    @SuppressWarnings("unchecked")
    void loadFile(String path) {
        try {
            properties = (Map<String, Object>) new Yaml().load(new FileInputStream(path));
        } catch (FileNotFoundException e) {
            System.err.println("Unable to find 'snow-globe.yml'.  This is needed to run.");
            throw new RuntimeException(e);
        }
    }

    private boolean getBooleanValue(String key) {
        return properties.get(key) != null &&
                properties.get(key).toString().equalsIgnoreCase("true");
    }

    private boolean getBooleanValue(String key, boolean defaultValue) {
        if(properties.get(key) == null) {
            return defaultValue;
        }
        return properties.get(key).toString().equalsIgnoreCase("true");
    }

    private String getStringValue(String key) {
        if(properties.get(key) != null) {
            return properties.get(key).toString();
        } else {
            return null;
        }
    }

    private String getStringValue(String key, String defaultValue) {
        return (properties.get(key) != null) ?
                properties.get(key).toString() :
                defaultValue;
    }


    public String getUpstreamBounceImage() {
        return getStringValue("upstream.bounce.image");
    }

    boolean logContainerOutput() {
        return getBooleanValue("snowglobe.log.output");
    }

    public boolean preserveTempFiles() {
        return getBooleanValue("snowglobe.preserve.temp.files");
    }

    public boolean defineUpstreamZones() {
        return getBooleanValue("nginx.define.upstream.zones");
    }

    String getNginxImage() {
        return getStringValue("nginx.container", "nginx");
    }

    String getUpstreamLocation(String environment) {
        return getEnvironmentString(environment, "nginx.upstream.file.path");
    }

    @SuppressWarnings("unchecked")
    List<String> getNginxVolumes(String environment) {
        return getEnvironmentList(environment, "nginx.volume.mounts");
    }

    @SuppressWarnings("unchecked")
    List<Map<String, Object>> getNginxPortMapping() {
        return (List<Map<String, Object>>) properties.get("nginx.url.port.mapping");
    }


    public List<String> getFilesToScan(String environment) {
        return getEnvironmentList(environment, "nginx.env.config.files");
    }

    @SuppressWarnings("unchecked")
    private List<String> getEnvironmentList(String environment, String key) {
        try {
            Map<String, Object> configMap = (Map<String, Object>) properties.get(key);
            return (configMap.containsKey(environment)) ?
                        (List<String>) configMap.get(environment) :
                        (List<String>) configMap.get("default");
        } catch (Exception e) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String getEnvironmentString(String environment, String key) {
        try {
            Map<String, Object> configMap = (Map<String, Object>) properties.get(key);
            return (configMap.containsKey(environment)) ?
                    valueOf(configMap.get(environment)) :
                    valueOf(configMap.get("default"));
        } catch (Exception e) {
            return null;
        }
    }

    public String getSourceDirectory() {
        return getStringValue("nginx.source.base.directory");
    }

    public String getDeployedDirectory() {
        return getStringValue("nginx.deploy.base.directory");
    }

    @SuppressWarnings("unchecked")
    public String getStartCommand(String environment) {
        Map<String, List<String>> startCommands = (Map<String, List<String>>) properties.get("nginx.start.command");
        if(!startCommands.containsKey(environment)) {
            environment = "default";
        }
        return startCommands.get(environment).stream().collect(joining(" "));
    }


    void setPropertyForTesting(String key, String value) {
        properties.put(key, value);
    }

    private int getIntValue(String key, int defaultValue) {
        try {
            return Integer.parseInt(properties.get(key).toString());
        }catch(Exception ignored) {
            return defaultValue;
        }
    }

    public int getMaxNginxStartupTime() {
        return getIntValue("nginx.max.startupTime", 10);
    }

    public int getMaxNginxStartupPollingTimeMs() {
        return getIntValue("nginx.startup.PollingTimeMs", 200);
    }
}
