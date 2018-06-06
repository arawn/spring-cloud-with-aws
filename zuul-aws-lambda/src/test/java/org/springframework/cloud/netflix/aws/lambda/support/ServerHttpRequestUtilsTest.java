package org.springframework.cloud.netflix.aws.lambda.support;

import lombok.val;
import org.junit.Test;

import java.io.UnsupportedEncodingException;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.*;

public class ServerHttpRequestUtilsTest {

    @Test
    public void simpleQuery() throws UnsupportedEncodingException {
        val request = MockServerHttpRequest.of("http://localhost?param1=value1&param2=value2");
        val queryParams = ServerHttpRequestUtils.getQueryParams(request);

        assertThat(queryParams.size(), is(2));
        assertThat(queryParams.get("param1").size(), is(1));
        assertThat(queryParams.get("param1"), hasItems("value1"));
        assertThat(queryParams.get("param2").size(), is(1));
        assertThat(queryParams.get("param2"), hasItems("value2"));

    }

    @Test
    public void arrayQuery() throws UnsupportedEncodingException {
        val request = MockServerHttpRequest.of("http://localhost?param1=value1&param1=value2");
        val queryParams = ServerHttpRequestUtils.getQueryParams(request);

        assertThat(queryParams.size(), is(1));
        assertThat(queryParams.get("param1").size(), is(2));
        assertThat(queryParams.get("param1"), hasItems("value1", "value2"));
    }

}