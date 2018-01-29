package com.kroger.oss.snowGlobe.util;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class UpstreamUtilTest {
    @Test
    public void should_use_localhost_if_no_dockerhost() throws URISyntaxException {
        String host = UpstreamUtil.getUpstreamHost("");
        assertThat(host, is("localhost"));
    }

    @Test
    public void should_use_localhost_if_dockerhost_is_socket() throws URISyntaxException {
        String host = UpstreamUtil.getUpstreamHost("unix:///var/run/docker.sock");
        assertThat(host, is("localhost"));
    }

    @Test
    public void should_use_remote_host_if_dockerhost_is_tcp() throws URISyntaxException {
        String host = UpstreamUtil.getUpstreamHost("tcp://some-other-host:2375");
        assertThat(host, is("some-other-host"));
    }
}
