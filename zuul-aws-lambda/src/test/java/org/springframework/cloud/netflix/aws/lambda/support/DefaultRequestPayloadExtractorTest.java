package org.springframework.cloud.netflix.aws.lambda.support;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.val;
import org.junit.Test;

import java.net.URISyntaxException;
import java.util.Iterator;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.springframework.http.MediaType.APPLICATION_FORM_URLENCODED;
import static org.springframework.http.MediaType.APPLICATION_JSON;

/**
 * @author arawn.kr@gmail.com
 */
public class DefaultRequestPayloadExtractorTest {

    final DefaultRequestPayloadExtractor extractor = new DefaultRequestPayloadExtractor();

    @Test
    public void jsonObjectBody() {
        val request = MockServerHttpRequest.of("http://localhost/lambda", APPLICATION_JSON, "{\"attribute1\":\"value\",\"attribute2\":\"321\"}");

        val payload = extractor.extractJson(request);
        assertThat(payload.get("attribute1").textValue(), is("value"));
        assertThat(payload.get("attribute2").textValue(), is("321"));
    }

    @Test
    public void jsonArrayBody() {
        val request = MockServerHttpRequest.of("http://localhost/lambda", APPLICATION_JSON, "[{\"attribute\":\"value1\"}, {\"attribute\":\"value2\"}]");

        val payload = extractor.extractJson(request);
        assertThat(payload.isArray(), is(true));
        assertThat(payload.size(), is(2));
    }

    @Test
    public void queryString() {
        val request = MockServerHttpRequest.of("http://localhost/lambda?param1=data&param2=123");

        val payload = extractor.extractJson(request);
        assertThat(payload.get("param1").textValue(), is("data"));
        assertThat(payload.get("param2").textValue(), is("123"));
    }

    @Test
    public void formBody() {
        val request = MockServerHttpRequest.of("http://localhost/lambda", APPLICATION_FORM_URLENCODED, "say=hi&to=arawn");

        val payload = extractor.extractJson(request);
        assertThat(payload.get("say").textValue(), is("hi"));
        assertThat(payload.get("to").textValue(), is("arawn"));
    }

    @Test
    public void formArrayBody() {
        val request = MockServerHttpRequest.of("http://localhost/lambda", APPLICATION_FORM_URLENCODED, "param=data1&param=data2");

        val payload = extractor.extractJson(request);
        assertThat(payload.get("param").isArray(), is(true));
        assertThat(payload.get("param").size(), is(2));
        val values = asStream(payload.get("param").elements()).map(JsonNode::asText).collect(Collectors.toSet());
        assertThat(values, hasItems("data1", "data2"));
    }

    @Test
    public void queryStringAndFormBody() {
        val request = MockServerHttpRequest.of("http://localhost/lambda?say=hello&to=arawn", APPLICATION_FORM_URLENCODED, "to=minchan");

        val payload = extractor.extractJson(request);
        assertThat(payload.get("say").textValue(), is("hello"));
        assertThat(payload.get("to").textValue(), is("minchan"));
    }

    @Test
    public void emptyPayload() {
        val request = MockServerHttpRequest.of("http://localhost/lambda");

        JsonNode payload = extractor.extractJson(request);
        assertThat(payload.isObject(), is(true));
        assertThat(payload.size(), is(0));
    }


    public static <T> Stream<T> asStream(Iterator<T> sourceIterator) {
        Iterable<T> iterable = () -> sourceIterator;
        return StreamSupport.stream(iterable.spliterator(), false);
    }

}