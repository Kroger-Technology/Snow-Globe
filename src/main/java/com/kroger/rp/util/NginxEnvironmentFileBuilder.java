package com.kroger.rp.util;

import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.kroger.rp.util.TestFrameworkProperties.getDeployedDirectory;
import static com.kroger.rp.util.TestFrameworkProperties.getSourceDirectory;
import static java.util.Arrays.stream;
import static java.util.Collections.singletonList;
import static java.util.stream.Collectors.toList;

public class NginxEnvironmentFileBuilder {

    Map<String, List<UpstreamAppInfo>> upstreamServers = new HashMap<>();

    public void addUpstreamServer(String clusterName, List<UpstreamAppInfo> appInfos) {
        upstreamServers.put(clusterName, appInfos);
    }

    public String buildClusterFileContents() {
        return upstreamServers.keySet()
                .stream()
                .map(serverName -> buildUpstreamServerEntry(serverName, upstreamServers.get(serverName)))
                .reduce(String::concat)
                .get();
    }


    protected void readNginxConfFile(String confFile, String prefix) {
        if(confFile.contains("*")) {
            getWildCardFiles(confFile).stream().forEach(file -> readNginxConfFile(file, prefix));
        } else {
            try {

                FileReader fileReader = new FileReader(confFile);
                BufferedReader reader = new BufferedReader(fileReader);
                while (reader.ready()) {
                    String line = reader.readLine();
                    if (line.contains(prefix)) {
                        addEmptyCluster(line);
                    } else if (line.trim().startsWith("include ") && line.trim().replaceAll("#.*", "").contains(".conf")) {
                        String subFileName = getIncludeFileName(line);
                        readNginxConfFile(subFileName, prefix);
                    }

                }
                fileReader.close();
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    protected String getIncludeFileName(String line) {
        String filePath = line.trim().substring(8).trim().replace(";", "").trim();
        return correctFilePath(filePath);
    }

    protected String correctFilePath(String filePath) {
        return (filePath.contains(getDeployedDirectory())) ?
                filePath.replace(getDeployedDirectory(), getSourceDirectory()) :
                getSourceDirectory() + File.separator + filePath;
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
        if (prefixRemoved.indexOf("/") > 0) {
            clusterName = prefixRemoved.substring(0, prefixRemoved.indexOf("/"));
        }

        if (prefixRemoved.indexOf("$") > 0) {
            clusterName = prefixRemoved.substring(0, prefixRemoved.indexOf("$"));
        }
        if (clusterName.contains(";")) {
            clusterName = clusterName.substring(0, clusterName.indexOf(";"));
        }
        if (clusterName.contains("/")) {
            clusterName = clusterName.substring(0, clusterName.indexOf("/"));
        }
        addUpstreamServer(clusterName, singletonList(new UpstreamAppInfo("127.0.0.1", 65534)));
    }

    public void addUpstreamServer(AppServiceCluster appServiceCluster) {
        this.addUpstreamServer(appServiceCluster.getClusterName(), appServiceCluster.getAppInstanceInfos());
    }

    public void readEnvConfig(String envConfig) {
        File envConfigFile = new File(envConfig);
        if(envConfigFile.exists()) {
            if(envConfigFile.isDirectory() && envConfigFile.listFiles() != null) {
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
        String afterStar =  wildcardInclude.split("\\*")[1];
        String baseDirectory = beforeStar.contains("/") ? beforeStar.substring(0, beforeStar.lastIndexOf("/")) : "";
        try {
            return stream(new File(baseDirectory).list())
                    .filter(s -> s.matches(".*" + afterStar))
                    .map(s -> beforeStar + s)
                    .collect(toList());
        } catch(Exception e) {
            return new ArrayList<>();
        }

    }
}
