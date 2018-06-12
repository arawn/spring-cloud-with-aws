package org.springframework.cloud.netflix.aws.lambda.support;

import lombok.Builder;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.server.ServerHttpAsyncRequestControl;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;
import java.util.Objects;

@Builder
public class MockServerHttpRequest implements ServerHttpRequest {

    final URI uri;
    final HttpMethod method;
    final HttpHeaders headers;
    final InputStream body;

    @Override
    public URI getURI() {
        return this.uri;
    }

    @Override
    public HttpMethod getMethod() {
        return this.method;
    }

    @Override
    public HttpHeaders getHeaders() {
        return this.headers;
    }

    @Override
    public InputStream getBody() throws IOException {
        return this.body;
    }

    @Override
    public Principal getPrincipal() {
        return null;
    }

    @Override
    public InetSocketAddress getLocalAddress() {
        return null;
    }

    @Override
    public InetSocketAddress getRemoteAddress() {
        return null;
    }

    @Override
    public ServerHttpAsyncRequestControl getAsyncRequestControl(ServerHttpResponse response) {
        return null;
    }


    public static ServerHttpRequest of(String uri) {
        return of(uri, HttpMethod.GET, MediaType.APPLICATION_FORM_URLENCODED, null);
    }

    public static ServerHttpRequest of(String uri, MediaType contentType, String body) {
        return of(uri, HttpMethod.POST, contentType, body);
    }

    public static ServerHttpRequest of(String uri, HttpMethod method, MediaType contentType, String body) {
        URI requestUri;
        try {
            requestUri = new URI(uri);
        } catch (URISyntaxException error) {
            throw new RuntimeException(uri + "을 해석할 수 없습니다.", error);
        }
        HttpHeaders headers = new HttpHeaders(); {
            headers.setContentType(contentType);
        }
        InputStream bodyStream = null; {
            if (Objects.nonNull(body)) {
                bodyStream = new ByteArrayInputStream(body.getBytes());
            }
        }

        return MockServerHttpRequest.builder()
                                    .uri(requestUri)
                                    .method(method)
                                    .headers(headers)
                                    .body(bodyStream)
                                    .build();
    }

}
