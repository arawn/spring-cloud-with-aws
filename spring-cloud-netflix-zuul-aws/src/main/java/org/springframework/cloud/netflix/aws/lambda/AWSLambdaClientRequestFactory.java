package org.springframework.cloud.netflix.aws.lambda;

import com.amazonaws.services.lambda.AWSLambdaAsync;
import lombok.val;
import org.springframework.cloud.netflix.aws.lambda.support.ServerHttpRequestUtils;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.server.ServerHttpRequest;

/**
 * @author arawn.kr@gmail.com
 */
public class AWSLambdaClientRequestFactory {

    private AWSLambdaAsync lambdaClient;
    private AWSLambdaClientContextFactory clientContextFactory;
    private RequestPayloadExtractor payloadExtractor;

    public AWSLambdaClientRequestFactory(AWSLambdaAsync lambdaClient, AWSLambdaClientContextFactory clientContextFactory, RequestPayloadExtractor payloadExtractor) {
        this.lambdaClient = lambdaClient;
        this.clientContextFactory = clientContextFactory;
        this.payloadExtractor = payloadExtractor;
    }

    public ClientHttpRequest createRequest(String functionArn, ServerHttpRequest request) {
        return createRequest(functionArn, request, false);
    }

    private ClientHttpRequest createRequest(String functionArn, ServerHttpRequest request, boolean proxyMode) {
        if (ServerHttpRequestUtils.isMultipart(request)) {
            throw new UnsupportedMultipartRequestException();
        }
        if (proxyMode) {
            // AWS API Gateway 람다 통합 기능과 비슷하게 지원할 예정
            throw new UnsupportedOperationException("프록시 모드는 지원하지 않습니다.");
        }

        val clientContext = clientContextFactory.create(request);
        val payload = payloadExtractor.extract(request);
        val lambdaRequest = new AWSLambdaClientRequest(lambdaClient, functionArn, clientContext, payload); {
            lambdaRequest.getHeaders().putAll(request.getHeaders());
        }
        return lambdaRequest;
    }


    class UnsupportedMultipartRequestException extends RuntimeException {
        UnsupportedMultipartRequestException() {
            super("unsupported multipart request");
        }
    }

}
