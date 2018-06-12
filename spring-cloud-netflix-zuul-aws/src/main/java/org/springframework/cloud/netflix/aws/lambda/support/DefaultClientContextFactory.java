package org.springframework.cloud.netflix.aws.lambda.support;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Value;
import lombok.val;
import org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientContextFactory;
import org.springframework.cloud.netflix.aws.lambda.ClientContext;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.StringUtils;

import java.nio.charset.Charset;
import java.util.Base64;
import java.util.HashMap;
import java.util.Locale;

/**
 * Lambda 호출시 전달할 {@link ClientContext} 를 작성하는 클래스
 * https://docs.aws.amazon.com/ko_kr/lambda/latest/dg/API_Invoke.html#API_Invoke_RequestSyntax 를 기반으로 작성한다.
 *
 * @author arawn.kr@gmail.com
 */
public class DefaultClientContextFactory implements AWSLambdaClientContextFactory {

    private Environment environment;
    private ObjectMapper objectMapper;

    public DefaultClientContextFactory(Environment environment) {
        this(environment, Jackson2ObjectMapperBuilder.json().build());
    }

    public DefaultClientContextFactory(Environment environment, ObjectMapper objectMapper) {
        this.environment = environment;
        this.objectMapper = objectMapper;
    }

    @Override
    public ClientContext create(ServerHttpRequest request) {
        val client = new ClientContext.Client(); {

        }
        val custom = new ClientContext.Custom(); {

        }
        val environment = new ClientContext.Environment(); {
            environment.put("spring.profiles.active", getActiveProfiles());
            environment.put("locale", Locale.getDefault().toString());
        }

        return new DefaultClientContext(client, custom, environment);
    }

    String getActiveProfiles() {
        return StringUtils.arrayToCommaDelimitedString(this.environment.getActiveProfiles());
    }


    @Value
    class DefaultClientContext implements ClientContext {

        private ClientContext.Client client;
        private ClientContext.Custom custom;
        private ClientContext.Environment environment;

        /**
         * @return The ClientContext JSON must be base64-encoded and has a maximum size of 3583 bytes.
         */
        @Override
        public String toString() {
            val data = new HashMap<String, ClientContext>(); {
                data.put("x-amz-Client-Context", this);
            }
            try {
                return Base64.getEncoder().encodeToString(objectMapper.writeValueAsString(data).getBytes(Charset.forName("utf-8")));
            } catch (JsonProcessingException error) {
                throw new ClientContextCreationException(error);
            }
        }

    }

    class ClientContextCreationException extends RuntimeException {
        ClientContextCreationException(Throwable cause) {
            super(cause);
        }
    }

}
