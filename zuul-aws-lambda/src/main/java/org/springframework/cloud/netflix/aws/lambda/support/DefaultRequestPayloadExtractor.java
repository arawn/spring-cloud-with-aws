package org.springframework.cloud.netflix.aws.lambda.support;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.cloud.netflix.aws.lambda.RequestPayloadExtractor;
import org.springframework.cloud.netflix.aws.lambda.support.ServerHttpRequestUtils;
import org.springframework.http.*;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.converter.support.AllEncompassingFormHttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

import java.io.IOException;
import java.io.InputStream;
import java.io.PushbackInputStream;
import java.util.HashMap;
import java.util.Objects;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
public class DefaultRequestPayloadExtractor implements RequestPayloadExtractor {

    private MappingJackson2HttpMessageConverter jsonConverter;
    private AllEncompassingFormHttpMessageConverter formConverter;

    public DefaultRequestPayloadExtractor() {
        this.jsonConverter = new MappingJackson2HttpMessageConverter();
        this.formConverter = new AllEncompassingFormHttpMessageConverter();
    }

    @Override
    public String extract(ServerHttpRequest request) {
        return extractJson(request).toString();
    }

    protected JsonNode extractJson(ServerHttpRequest request) {
        try {
            val contentType = ServerHttpRequestUtils.getContentType(request, MediaType.APPLICATION_OCTET_STREAM);
            val inputMessage = new EmptyBodyCheckingHttpInputMessage(request);

            // body 에서 JSON 데이터 추출
            val targetType = JsonNode.class;
            if (jsonConverter.canRead(targetType, contentType)) {
                log.debug("Read [{}] as \"{}\" with [{}]", targetType, contentType, jsonConverter);
                if (Objects.nonNull(inputMessage.getBody())) {
                    return (JsonNode) jsonConverter.read(targetType, inputMessage);
                }
                return NullNode.getInstance();
            }

            // query_string + body 에서 FROM 데이터 추출
            val formData = new HashMap<String, Object>(); {
                ServerHttpRequestUtils.getQueryParams(request).forEach((key, values) ->
                        formData.put(key, values.size() == 1 ? values.get(0) : values));
                if (Objects.nonNull(inputMessage.getBody())) {
                    formConverter.read(FormBody.class, inputMessage).forEach((key, values) ->
                            formData.put(key, values.size() == 1 ? values.get(0) : values));
                }
            }
            return jsonConverter.getObjectMapper().valueToTree(formData);
        } catch (Exception error) {
            throw new HttpMessageNotReadableException("could not read request body: " + error.getMessage(), error);
        }
    }


    class FormBody extends LinkedMultiValueMap<String, Object> { }

    class EmptyBodyCheckingHttpInputMessage implements HttpInputMessage {

        private final HttpHeaders headers;
        private final InputStream body;
        private final HttpMethod method;

        EmptyBodyCheckingHttpInputMessage(HttpInputMessage inputMessage) throws IOException {
            this.headers = inputMessage.getHeaders();
            InputStream inputStream = inputMessage.getBody();
            if (inputStream == null) {
                this.body = null;
            } else if (inputStream.markSupported()) {
                inputStream.mark(1);
                this.body = (inputStream.read() != -1 ? inputStream : null);
                inputStream.reset();
            } else {
                PushbackInputStream pushbackInputStream = new PushbackInputStream(inputStream);
                int data = pushbackInputStream.read();
                if (data == -1) {
                    this.body = null;
                }
                else {
                    this.body = pushbackInputStream;
                    pushbackInputStream.unread(data);
                }
            }
            this.method = ((HttpRequest) inputMessage).getMethod();
        }

        @Override
        public HttpHeaders getHeaders() {
            return this.headers;
        }

        @Override
        public InputStream getBody() throws IOException {
            return this.body;
        }

        public HttpMethod getMethod() {
            return method;
        }

    }

}
