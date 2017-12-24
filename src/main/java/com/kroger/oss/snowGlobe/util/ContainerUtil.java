package com.kroger.oss.snowGlobe.util;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.stream.Collectors;

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

    public static void runCommandWithLogs(String... command) {
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.inheritIO();
            System.out.println("\n\tRunning Command: " + Arrays.stream(command).collect(Collectors.joining(" ")));
            System.out.println("----------------------------------------------------");
            Process process = processBuilder.start();
            process.waitFor();
            System.out.println("[EXIT CODE: " + process.exitValue() + "]");
            System.out.println("----------------------------------------------------");
            process.waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void runCommand(String... command) {
        try {
            new ProcessBuilder(command).start().waitFor();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static void shutdownContainer(String containerName) {
        runCommand("docker", "rm", "-f", containerName);
    }

    public static void shutdownContainerWithLogs(String containerName) {
        runCommandWithLogs("docker", "rm", "-f", containerName);
    }
}
