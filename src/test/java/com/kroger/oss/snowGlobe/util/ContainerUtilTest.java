package com.kroger.oss.snowGlobe.util;

import org.junit.Test;

import java.util.Map;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.*;

public class ContainerUtilTest {

    @Test
    public void should_parse_docker_ps_to_get_ports() {
        String exampleLine = "d28faaa22c3a        nginx               \"nginx -g 'daemon ...\"   6 seconds ago       Up 5 seconds        0.0.0.0:57347->80/tcp, 0.0.0.0:57346->443/tcp   RP-2e7545ceaf8adc77ffec25e158b501c7";
        Map<Integer, Integer> mappedPorts = ContainerUtil.parseMappedPorts(exampleLine);
        assertThat(mappedPorts.get(80), is(57347));
        assertThat(mappedPorts.get(443), is(57346));
    }

}