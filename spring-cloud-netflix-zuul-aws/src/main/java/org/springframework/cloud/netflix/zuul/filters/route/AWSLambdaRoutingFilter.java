package org.springframework.cloud.netflix.zuul.filters.route;

import com.google.common.io.CharStreams;
import com.netflix.hystrix.exception.HystrixRuntimeException;
import com.netflix.zuul.ZuulFilter;
import com.netflix.zuul.context.RequestContext;
import com.netflix.zuul.exception.ZuulException;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientRequestFactory;
import org.springframework.cloud.netflix.zuul.filters.ProxyRequestHelper;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.support.FilterConstants;
import org.springframework.cloud.netflix.zuul.util.ZuulRuntimeException;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.Set;

import static org.springframework.cloud.netflix.zuul.filters.support.FilterConstants.SERVICE_ID_KEY;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
public class AWSLambdaRoutingFilter extends ZuulFilter {

    private AWSLambdaClientRequestFactory requestFactory;
    private ZuulProperties zuulProperties;
    private Set<FallbackProvider> fallbackProviders;
    private ProxyRequestHelper requestHelper;

    public AWSLambdaRoutingFilter(AWSLambdaClientRequestFactory requestFactory, ZuulProperties zuulProperties) {
        this.requestFactory = requestFactory;
        this.zuulProperties = zuulProperties;
        this.requestHelper = new ProxyRequestHelper();
    }

    @Override
    public String filterType() {
        return FilterConstants.ROUTE_TYPE;
    }

    @Override
    public int filterOrder() {
        return FilterConstants.RIBBON_ROUTING_FILTER_ORDER - 5;
    }

    @Override
    public boolean shouldFilter() {
        return isLambdaRequest(getCurrentFunctionArn()) && RequestContext.getCurrentContext().sendZuulResponse();
    }

    protected String getCurrentFunctionArn() {
        return String.valueOf(RequestContext.getCurrentContext().get(SERVICE_ID_KEY));
    }

    protected boolean isLambdaRequest(Object serviceId) {
        return Objects.nonNull(serviceId) && serviceId.toString().startsWith("arn:aws:lambda:");
    }

    @Override
    public Object run() {
        requestHelper.addIgnoredHeaders();

        val context = RequestContext.getCurrentContext();
        val functionArn = getCurrentFunctionArn();
        val request = new ServletServerHttpRequest(context.getRequest());

        try {
            val lambdaRequest = requestFactory.createRequest(functionArn, request);
            val lambdaResponse = forward(lambdaRequest, getFallbackProvider(functionArn));
            setResponse(context, lambdaResponse);
            return lambdaResponse;
        } catch (ZuulException error) {
            throw new ZuulRuntimeException(error);
        } catch (Exception error) {
            throw new ZuulRuntimeException(new ZuulException(error, 500, "AWSLambdaFunctionInvokeError"));
        }
    }

    protected FallbackProvider getFallbackProvider(String functionArn) {
        if (Objects.nonNull(fallbackProviders)) {
            return fallbackProviders.stream()
                                    .filter(it -> Objects.equals(it.getRoute(), functionArn))
                                    .findFirst()
                                    .orElse(null);
        }
        return null;
    }

    protected ClientHttpResponse forward(ClientHttpRequest lambdaRequest, FallbackProvider fallbackProvider) throws ZuulException {
        try {
            log.debug("forward lambda function: {}", lambdaRequest);
            val lambdaCommand = new AWSLambdaCommand(lambdaRequest, fallbackProvider, zuulProperties);
            return lambdaCommand.execute();
        } catch (HystrixRuntimeException error) {
            if (error.getFailureType() == HystrixRuntimeException.FailureType.TIMEOUT) {
                throw new ZuulException(error, 500, "HystrixTimeoutException");
            }
            throw error;
        }
    }

    protected void setResponse(RequestContext context, ClientHttpResponse response) throws IOException, ZuulException {
        if (response.getStatusCode().is4xxClientError() || response.getStatusCode().is5xxServerError()) {
            try (Reader reader = new InputStreamReader(response.getBody())) {
                log.error(CharStreams.toString(reader));
            }
            throw new ZuulException("람다 함수 호출 중 오류가 발생했습니다.", response.getRawStatusCode(), "AWSLambdaFunctionError");
        }

        requestHelper.setResponse(response.getStatusCode().value(), response.getBody(), response.getHeaders());
        context.set("zuulResponse", response);

        // prevent RibbonRoutingFilter from running
        context.set(SERVICE_ID_KEY, null);
        // prevent SimpleHostRoutingFilter from running
        context.setRouteHost(null);
    }

    @Autowired(required = false)
    public void setFallbackProviders(Set<FallbackProvider> fallbackProviders) {
        this.fallbackProviders = fallbackProviders;
    }

}
