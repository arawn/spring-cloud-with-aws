package org.springframework.cloud.netflix.aws.lambda;

import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
public class AWSLambdaClientRequestTest {

    @Test
    @Ignore("배포된 AWS 함수를 직접 호출해보는 테스트 케이스입니다.")
    public void executeLambda() throws IOException {
        val credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        val lambdaClient = AWSLambdaAsyncClientBuilder.standard()
                                                      .withCredentials(credentialsProvider)
                                                      .withRegion("ap-northeast-2")
                                                      .build();

        val functionArn = System.getProperty("functionArn");
        val request = new AWSLambdaClientRequest(lambdaClient, functionArn);

        ClientHttpResponse response = request.execute();
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));

        ObjectMapper mapper = new ObjectMapper();
        JsonNode responseBody = mapper.readValue(response.getBody(), JsonNode.class);
        log.info("response body: {}", responseBody);
    }

}