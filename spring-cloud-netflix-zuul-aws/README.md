# Integration for AWS Lambda with Spring Cloud Netflix Zuul

[Spring Cloud Netflix](https://cloud.spring.io/spring-cloud-netflix/) - [Zuul](https://github.com/Netflix/zuul)과 [AWS Lambda](https://aws.amazon.com/ko/lambda/)를 통합하는 예제 코드다. AWS Lambda로 작성된 기능을 HTTP 엔드포인트로 외부에 제공할 필요가 있어 작성한 코드를 다듬어서 공개한다.

AWS Lambda를 HTTP로 통합하는 방법을 찾아보면 [Amazon API Gateway‎](https://docs.aws.amazon.com/apigateway/latest/developerguide/getting-started-with-lambda-integration.html)를 사용하는 방법이 가장 많이 보인다. 현재 담당하고 있는 시스템은 Spring Cloud Netflix - Zuul을 사용해 이미 API Gateway 서비스를 운용하고 있어, Java용 AWS SDK를 사용해 람다 함수를 라우팅해주는 컴포넌트를 작성했다.

2가지 역할을 수행하는 모듈로 작성되어 있다.
 
### org.springframework.cloud.netflix.aws.lambda

AWS SDK를 사용해서 람다 요청 및 응답 처리를 수행하는 모듈로 핵심은 다음 2가지 클래스다. 

> - org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientRequest
> - org.springframework.cloud.netflix.aws.lambda.AWSLambdaClientResponse

람다 리소스 이름([ARN](https://docs.aws.amazon.com/ko_kr/general/latest/gr/aws-arns-and-namespaces.html)과 JSON으로 작성된 요청 데이터(payload)로 `AWSLambdaClientRequest` 를 생성 후 호출하면 `AWSLambdaClientResponse` 가 반환된다.

```java
public class AWSLambdaClientRequestTest {

    @Test
    public void executeLambda() throws IOException {
        AWSCredentialsProvider credentialsProvider = new ClasspathPropertiesFileCredentialsProvider();
        AWSLambdaAsync lambdaClient = AWSLambdaAsyncClientBuilder
                .standard()
                .withCredentials(credentialsProvider)
                .withRegion("ap-northeast-2")
                .build();

        String functionArn = "arn:aws:lambda:region:account-id:function:function-name";
        val request = new AWSLambdaClientRequest(lambdaClient, functionArn);

        ClientHttpResponse response = request.execute();
        assertThat(response.getStatusCode(), is(HttpStatus.OK));
        assertThat(response.getHeaders().getContentType(), is(MediaType.APPLICATION_JSON));
    }

}
```

`AWSLambdaAsync` 생성시 타임아웃외에 다양한 설정 값을 통해 제어가 가능하다.

```java
@Configuration
public class AWSLambdaClientConfig {

    @Autowired
    private Environment environment;

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

}
```

### org.springframework.cloud.netflix.zuul.filters.route

Zuul을 통해 람다 라우팅을 수행하는 모듈로 핵심은 `org.springframework.cloud.netflix.zuul.filters.route.AWSLambdaRoutingFilter` 클래스다. `AWSLambdaClientRequest`와 `AWSLambdaClientResponse`를 사용해 라우팅 처리하며, Hystrix가 적용되어 있다. [Zuul 라우팅 설정](https://cloud.spring.io/spring-cloud-netflix/multi/multi__router_and_filter_zuul.html)에서 url 속성에 람다 ARN을 지정하면 동작한다. 

```xml
zuul:
  routes:
    lambda:
      path: /lambda
      url: arn:aws:lambda:region:account-id:function:function-name
```

위와 같이 설정 후 `/lambda` URL로 접근시 지정된 람다를 호출 후 응답 결과를 반환해준다.

Hystrix에 타임아웃 설정은 다음과 같이 가능하다. (service-lambda는 고정값이다.)

```xml
hystrix:
  command.service-lambda.execution.isolation.thread.timeoutInMilliseconds: 5250
```

## 데모

> 준비물:
> AWS 인증 정보(accessKey, secretKey)
> 실행 가능한 AWS 람다

1. `spring-cloud-netflix-zuul-aws/zuul-aws-lambda/src/test/resources` 폴더에 application.yml과 AwsCredentials.properties 파일을 작성한다. (sample 참고)
2. `org.springframework.cloud.netflix.AWSLambdaRoutingIntegrationTest` 클래스 실행