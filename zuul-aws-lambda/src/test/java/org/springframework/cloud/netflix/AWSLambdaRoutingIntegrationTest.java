package org.springframework.cloud.netflix;

import com.amazonaws.ClientConfiguration;
import com.amazonaws.ClientConfigurationFactory;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.ClasspathPropertiesFileCredentialsProvider;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.AWSLambdaAsyncClientBuilder;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.context.PropertyPlaceholderAutoConfiguration;
import org.springframework.boot.autoconfigure.jackson.JacksonAutoConfiguration;
import org.springframework.boot.autoconfigure.web.*;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.client.discovery.simple.SimpleDiscoveryClientAutoConfiguration;
import org.springframework.cloud.client.loadbalancer.LoadBalancerAutoConfiguration;
import org.springframework.cloud.commons.util.UtilAutoConfiguration;
import org.springframework.cloud.netflix.archaius.ArchaiusAutoConfiguration;
import org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientContextFactory;
import org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientRequestFactory;
import org.springframework.cloud.netflix.aws.lambda.RequestPayloadExtractor;
import org.springframework.cloud.netflix.aws.lambda.support.DefaultClientContextFactory;
import org.springframework.cloud.netflix.aws.lambda.support.DefaultRequestPayloadExtractor;
import org.springframework.cloud.netflix.ribbon.RibbonAutoConfiguration;
import org.springframework.cloud.netflix.zuul.EnableZuulProxy;
import org.springframework.cloud.netflix.zuul.ZuulProxyAutoConfiguration;
import org.springframework.cloud.netflix.zuul.filters.ZuulProperties;
import org.springframework.cloud.netflix.zuul.filters.route.AWSLambdaRoutingFilter;
import org.springframework.cloud.netflix.zuul.filters.route.FallbackProvider;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.http.MediaType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Set;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@RunWith(SpringRunner.class)
@SpringBootTest(classes = AWSLambdaRoutingIntegrationTest.IntegrationTestConfig.class, webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@AutoConfigureMockMvc
public class AWSLambdaRoutingIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Test
    public void simpleProxy() throws Exception {
        mvc.perform(get("/google")).andExpect(status().isOk());
    }

    @Test
    public void lambda() throws Exception {
        mvc.perform(get("/lambda").accept(MediaType.APPLICATION_JSON))
           .andExpect(status().isOk())
           .andDo(print());
    }


    @SpringBootConfiguration
    @Import({
        /* Spring Web */
        ServerPropertiesAutoConfiguration.class,
        EmbeddedServletContainerAutoConfiguration.class,
        DispatcherServletAutoConfiguration.class,
        WebMvcAutoConfiguration.class,
        ErrorMvcAutoConfiguration.class,
        HttpEncodingAutoConfiguration.class,
        HttpMessageConvertersAutoConfiguration.class,
        JacksonAutoConfiguration.class,
        PropertyPlaceholderAutoConfiguration.class,

        /* Spring Cloud Netflix */
        UtilAutoConfiguration.class,
        ArchaiusAutoConfiguration.class,
        SimpleDiscoveryClientAutoConfiguration.class,
        LoadBalancerAutoConfiguration.class,
        RibbonAutoConfiguration.class,
        ZuulProxyAutoConfiguration.class
    })
    @EnableZuulProxy
    static class IntegrationTestConfig {

        @Autowired
        private Environment environment;

        @Autowired
        private ZuulProperties zuulProperties;

        @Autowired(required = false)
        private Set<FallbackProvider> fallbackProviders;

        @Bean
        public AWSLambdaAsync awsLambdaAsync() {
            // AWS 인증정보
            AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();

            // 람다 클라이언트 설정
            ClientConfiguration configuration = new ClientConfigurationFactory().getConfig();
            configuration.setConnectionTimeout(ClientConfiguration.DEFAULT_CONNECTION_TIMEOUT);
            configuration.setSocketTimeout(ClientConfiguration.DEFAULT_SOCKET_TIMEOUT);
            configuration.setRequestTimeout(ClientConfiguration.DEFAULT_REQUEST_TIMEOUT);
            configuration.setMaxErrorRetry(0);

            return AWSLambdaAsyncClientBuilder.standard()
                                              .withCredentials(credentialsProvider)
                                              .withRegion(Regions.AP_NORTHEAST_2)
                                              .withClientConfiguration(configuration)
                                              .build();
        }

        @Bean
        public AWSLambdaClientRequestFactory awsLambdaClientRequestFactory(AWSLambdaAsync awsLambdaAsync) {
            AWSLambdaClientContextFactory clientContextFactory = new DefaultClientContextFactory(environment);
            RequestPayloadExtractor payloadExtractor = new DefaultRequestPayloadExtractor();

            return new AWSLambdaClientRequestFactory(awsLambdaAsync, clientContextFactory, payloadExtractor);
        }

        @Bean
        public AWSLambdaRoutingFilter awsLambdaRoutingFilter(AWSLambdaClientRequestFactory requestFactory) {
            AWSLambdaRoutingFilter awsLambdaRoutingFilter = new AWSLambdaRoutingFilter(requestFactory, zuulProperties);
            awsLambdaRoutingFilter.setFallbackProviders(fallbackProviders);

            return awsLambdaRoutingFilter;
        }

    }

}