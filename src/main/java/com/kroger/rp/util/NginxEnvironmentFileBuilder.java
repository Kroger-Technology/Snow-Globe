package com.kroger.rp.util;

import com.kroger.rp.util.environment.UpstreamAppInfo;

import java.io.File;
import java.util.List;

import static java.util.Arrays.stream;

public class NginxEnvironmentFileBuilder extends BaseEnvironmentUtil{


    public NginxEnvironmentFileBuilder(String nginxConfFile) {
        readNginxConfFile(nginxConfFile, "proxy_pass");
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
}
