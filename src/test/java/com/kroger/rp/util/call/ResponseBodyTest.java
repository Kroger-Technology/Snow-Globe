package com.kroger.rp.util.call;

import org.junit.Test;

import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class ResponseBodyTest {

    @Test
    public void shouldBuildResponseBody() {
        String rawResponse = "{}";
        ResponseBody body = new ResponseBody(rawResponse);
        assertThat(rawResponse, is(body.getRawResponse()));
        assertThat(body.getRequest(), is(notNullValue()));
    }

    @Test
    public void shouldBuildResponseBodyDirectlyFromRP() {
        String rawResponse = "{}";
        ResponseBody body = ResponseBody.buildDirectResponseFromRp(rawResponse);
        assertThat(rawResponse, is(body.getRawResponse()));
        assertThat(body.getRequest(), is(notNullValue()));
    }
}