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

package com.kroger.dcp.snowGlobe;

import com.kroger.dcp.snowGlobe.environment.UpstreamAppInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class NginxEnvironmentFileBuilder {

    TestFrameworkProperties testFrameworkProperties;

    public NginxEnvironmentFileBuilder() {
        testFrameworkProperties = new TestFrameworkProperties();
    }

    Map<String, List<UpstreamAppInfo>> upstreamServers = new HashMap<>();

    public void addUpstreamServer(String clusterName, List<UpstreamAppInfo> appInfos) {
        upstreamServers.put(clusterName.trim(), appInfos);
    }

    public String buildClusterFileContents() {
        return upstreamServers.keySet()
                .stream()
                .map(serverName -> buildUpstreamServerEntry(serverName, upstreamServers.get(serverName)))
                .reduce(String::concat)
                .get();
    }


    protected void readNginxConfFile(String confFile, String prefix) {
        if (confFile.contains("*")) {
            getWildCardFiles(confFile).stream().forEach(file -> readNginxConfFile(file, prefix));
        } else {
            readSingleNginxFile(confFile, prefix);
        }
    }

    private void readSingleNginxFile(String confFile, String prefix) {
        try {
            FileReader fileReader = new FileReader(confFile);
            BufferedReader reader = new BufferedReader(fileReader);
            while (reader.ready()) {
                parseNginxFileLine(prefix, reader.readLine());
            }
            fileReader.close();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void parseNginxFileLine(String prefix, String line) {
        if (line.contains(prefix)) {
            addEmptyCluster(line);
        } else if (line.trim().startsWith("include ")) {
            readNginxConfFile(getIncludeFileName(line), prefix);
        }
    }

    protected String getIncludeFileName(String line) {
        return correctFilePath(line.trim().substring(8).trim().replace(";", "").trim());
    }

    protected String correctFilePath(String filePath) {
        return (filePath.contains(testFrameworkProperties.getDeployedDirectory())) ?
                filePath.replace(testFrameworkProperties.getDeployedDirectory(), testFrameworkProperties.getSourceDirectory()) :
                testFrameworkProperties.getSourceDirectory() + File.separator + filePath;
    }

    /**
     * Here, we expect each line to be in a specific format that gives us a hint as to which
     * upstream servers the configuration will be looking for.  We just add an empty one so it doesn't
     * complain and shutdown.
     *
     * @param line a line from the nginx.conf file that should be of the format:
     *             "   proxy_pass http://[CLUSTER NAME]/..."
     */
    void addEmptyCluster(String line) {
        String prefixRemoved = line.substring(line.indexOf("://") + 3);
        String clusterName = prefixRemoved;
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, "/");
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, "$");
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, ";");
        clusterName = handleClusterNameChar(prefixRemoved, clusterName, "/");
        addUpstreamServer(clusterName, singletonList(new UpstreamAppInfo("127.0.0.1", 65534)));
    }

    private String handleClusterNameChar(String prefixRemoved, String clusterName, String character) {
        return (prefixRemoved.indexOf(character) > 0) ?
            prefixRemoved.substring(0, prefixRemoved.indexOf(character)) : clusterName;
    }

    public void addUpstreamServer(AppServiceCluster appServiceCluster) {
        this.addUpstreamServer(appServiceCluster.getClusterName(), appServiceCluster.getAppInstanceInfos());
    }

    public void readEnvConfig(String envConfig) {
        File envConfigFile = new File(envConfig);
        if (envConfigFile.exists()) {
            if (envConfigFile.isDirectory() && envConfigFile.listFiles() != null) {
                stream(envConfigFile.listFiles())
                        .map(File::getAbsolutePath)
                        .forEach(this::readEnvConfig);
            } else {
                readNginxConfFile(envConfig, "proxy_pass");
            }
        }
    }

    String buildUpstreamServerEntry(String serverName, List<UpstreamAppInfo> infos) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n")
                .append("  upstream ").append(serverName).append(" { \n");
        if (testFrameworkProperties.defineUpstreamZones()) {
            sb.append("    zone " + serverName + " 64k;\n");
        }
        infos.stream()
                .forEach(app ->
                        sb.append("    server ")
                                .append(app.containerName())
                                .append(":")
                                .append(app.port())
                                .append(";\n"));
        sb.append("  }")
                .append("\n")
                .append("\n");
        return sb.toString();
    }

    public List<String> getWildCardFiles(String wildcardInclude) {
        String beforeStar = wildcardInclude.split("\\*")[0];
        String afterStar = wildcardInclude.split("\\*")[1];
        String baseDirectory = beforeStar.contains("/") ? beforeStar.substring(0, beforeStar.lastIndexOf("/")) : "";
        try {
            return stream(new File(baseDirectory).list())
                    .filter(s -> s.matches(".*" + afterStar))
                    .map(s -> beforeStar + s)
                    .collect(toList());
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    TestFrameworkProperties getPropertiesForTest() {
        return testFrameworkProperties;
    }
}
