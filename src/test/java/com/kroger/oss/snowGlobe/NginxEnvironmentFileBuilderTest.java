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

import com.kroger.oss.snowGlobe.environment.UpstreamAppInfo;
import org.junit.Before;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class NginxEnvironmentFileBuilderTest {

    private NginxEnvironmentFileBuilder fileBuilder;

    @Before
    public void setup() {
        fileBuilder = new NginxEnvironmentFileBuilder();
    }

    @Test
    public void shouldBeAbleToAddUpstreamEntry() {
        final String cluster1 = "cluster1";
        final String cluster2 = "cluster2";
        fileBuilder.addEmptyCluster("http://" + cluster1);
        fileBuilder.addEmptyCluster("http://" + cluster2);
        String clusterFileContents = fileBuilder.buildClusterFileContents();

        String expectedContents = fileBuilder.buildUpstreamServerEntry(cluster1, new UpstreamAppInfo()) +
                fileBuilder.buildUpstreamServerEntry(cluster2, new UpstreamAppInfo());

        assertThat(expectedContents, is(clusterFileContents));
    }

    @Test
    public void shouldHandleUpstreamLinesWithHttps() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass https://" + cluster1;
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldHandleUpstreamLinesWithHttp() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1;
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldHandleUpstreamLinesWithTrailingSpace() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1 + " ;";
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldHandleUpstreamLinesWithPath() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1 + "/path/to/other/thing";
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldHandleUpstreamLinesWithDollarSignAtEnd() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1 + "$";
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, not(hasKey(cluster1)));
    }

    @Test
    public void shouldHandleUpstreamLinesWithDollarSignAtBeginning() {
        String cluster1 = "$cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1;
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, not(hasKey(cluster1)));
    }

    @Test
    public void shouldHandleUpstreamLinesWithDollarSignInMiddle() {
        String cluster1 = "cluster1$env";
        final String upstreamLine = "proxy_pass http://" + cluster1;
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, not(hasKey(cluster1)));
    }

    @Test
    public void shouldHandleUpstreamLinesWithDollarSignAfterClusterName() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1 + "/q=$1";
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldHandleUpstreamLinesWithSemiColon() {
        String cluster1 = "cluster1";
        final String upstreamLine = "proxy_pass http://" + cluster1 + ";";
        fileBuilder.addEmptyCluster(upstreamLine);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster1));
    }

    @Test
    public void shouldBeAbleToAddUpstreamFromAppServiceCluster() {
        String cluster = "cluster1";
        AppServiceCluster serviceCluster = mock(AppServiceCluster.class);
        when(serviceCluster.getClusterName()).thenReturn(cluster);
        fileBuilder.addUpstreamServer(serviceCluster);
        assertThat(fileBuilder.upstreamServers, hasKey(cluster));
    }

    @Test
    public void shouldBuildUpstreamEntryWithZone() {
        fileBuilder.getPropertiesForTest().setPropertyForTesting("nginx.define.upstream.zones", "true");
        String serverName = "server";
        String containerName = "upstream";
        int containerPort = 0;
        UpstreamAppInfo appInfo = new UpstreamAppInfo();
        String actualEntry = fileBuilder.buildUpstreamServerEntry(serverName, appInfo);

        assertThat(actualEntry, is("\n  upstream " + serverName + " { \n" +
                "    zone " + serverName + " 64k;\n" +
                "    server " + containerName + ":" + containerPort + ";\n" +
                "  }" +
                "\n" +
                "\n"));
    }

    @Test
    public void shouldFetchWildcardIncludesForExtensionWildcard() {
        List<String> wildCardFiles = fileBuilder.getWildCardFiles("src/test/resources/wildCardTests/*.html");
        assertThat(wildCardFiles, contains("src/test/resources/wildCardTests/index.html",
                "src/test/resources/wildCardTests/open.html",
                "src/test/resources/wildCardTests/test.html"));
    }

    @Test
    public void shouldFetchWildcardIncludesForPrefixWildcard() {
        List<String> wildCardFiles = fileBuilder.getWildCardFiles("src/test/resources/wildCardTests/index.*");
        assertThat(wildCardFiles, contains("src/test/resources/wildCardTests/index.html",
                "src/test/resources/wildCardTests/index.txt"));
    }

    @Test
    public void shouldFetchWildcardIncludesForAllMatchingWildCard() {
        List<String> wildCardFiles = fileBuilder.getWildCardFiles("src/test/resources/wildCardTests/*");
        assertThat(wildCardFiles, contains("src/test/resources/wildCardTests/index.html",
                "src/test/resources/wildCardTests/index.txt",
                "src/test/resources/wildCardTests/open.html",
                "src/test/resources/wildCardTests/readme.txt",
                "src/test/resources/wildCardTests/test.html"));
    }

}