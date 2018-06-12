package org.springframework.cloud.netflix.zuul.filters.route;

import com.netflix.config.DynamicIntProperty;
import com.netflix.config.DynamicPropertyFactory;
import com.netflix.hystrix.*;
import com.netflix.zuul.constants.ZuulConstants;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.http.client.ClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.util.Objects;

/**
 * @author arawn.kr@gmail.com
 */
public class AWSLambdaCommand extends HystrixCommand<ClientHttpResponse> {

    private static final String COMMAND_KEY = "service-lambda";

    private final ClientHttpRequest lambdaRequest;
    private final FallbackProvider fallbackProvider;

    public AWSLambdaCommand(ClientHttpRequest lambdaRequest, FallbackProvider fallbackProvider, ZuulProperties zuulProperties) {
        super(getSetter(COMMAND_KEY, zuulProperties));
        this.lambdaRequest = lambdaRequest;
        this.fallbackProvider = fallbackProvider;
    }

    @Override
    protected ClientHttpResponse run() throws Exception {
        return lambdaRequest.execute();
    }

    @Override
    protected ClientHttpResponse getFallback() {
        if(Objects.nonNull(fallbackProvider)) {
            return fallbackProvider.fallbackResponse();
        }
        return super.getFallback();
    }


    /**
     * {@link org.springframework.cloud.netflix.zuul.filters.route.support.AbstractRibbonCommand} 에서 복제 후 가공
     */
    protected static Setter getSetter(final String commandKey, ZuulProperties zuulProperties) {
        // @formatter:off
        final Setter commandSetter = Setter.withGroupKey(HystrixCommandGroupKey.Factory.asKey("AWSLambdaCommand"))
                                           .andCommandKey(HystrixCommandKey.Factory.asKey(commandKey));
        final HystrixCommandProperties.Setter setter = createSetter(commandKey, zuulProperties);
        if (zuulProperties.getRibbonIsolationStrategy() == HystrixCommandProperties.ExecutionIsolationStrategy.SEMAPHORE) {
            final String name = ZuulConstants.ZUUL_EUREKA + commandKey + ".semaphore.maxSemaphores";
            // we want to default to semaphore-isolation since this wraps
            // 2 others commands that are already thread isolated
            final DynamicIntProperty value = DynamicPropertyFactory.getInstance()
                    .getIntProperty(name, zuulProperties.getSemaphore().getMaxSemaphores());
            setter.withExecutionIsolationSemaphoreMaxConcurrentRequests(value.get());
        } else if (zuulProperties.getThreadPool().isUseSeparateThreadPools()) {
            final String threadPoolKey = zuulProperties.getThreadPool().getThreadPoolKeyPrefix() + commandKey;
            commandSetter.andThreadPoolKey(HystrixThreadPoolKey.Factory.asKey(threadPoolKey));
        }

        return commandSetter.andCommandPropertiesDefaults(setter);
        // @formatter:on
    }

    /**
     * {@link org.springframework.core.env.Environment}을 통해 타임아웃 설정값을 불러온다.
     * Spring Boot를 사용한다면, application.yml에서 아래와 같이 작성시 commandHystrixTimeout가 적용된다.
     *
     * hystrix:
     *   command.service-lambda.execution.isolation.thread.timeoutInMilliseconds: 5250
     *
     */
    protected static HystrixCommandProperties.Setter createSetter(String commandKey, ZuulProperties zuulProperties) {
        DynamicPropertyFactory dynamicPropertyFactory = DynamicPropertyFactory.getInstance();
        int defaultHystrixTimeout = dynamicPropertyFactory.getIntProperty("hystrix.command.default.execution.isolation.thread.timeoutInMilliseconds",500).get();
        int commandHystrixTimeout = dynamicPropertyFactory.getIntProperty("hystrix.command." + commandKey + ".execution.isolation.thread.timeoutInMilliseconds",0).get();
        int hystrixTimeout = commandHystrixTimeout > 0 ? commandHystrixTimeout : defaultHystrixTimeout;

        return HystrixCommandProperties.Setter()
                                       .withExecutionIsolationStrategy(zuulProperties.getRibbonIsolationStrategy())
                                       .withExecutionTimeoutInMilliseconds(hystrixTimeout);
    }

}