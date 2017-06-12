package com.kroger.oss.snowGlobe.util;

import java.io.IOException;

public class ContainerUtil {

    public static void logContainerOutput(String containerName) {
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

    public static void startContainer(String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder =
                new ProcessBuilder(command);
        Process process = processBuilder.start();
        process.waitFor();
    }
}
