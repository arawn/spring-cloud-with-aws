# Integration for AWS S3 with Spring Cloud Config

[Spring Cloud Config](https://cloud.spring.io/spring-cloud-config/)는 애플리케이션에 필요한 설정 정보를 외부화하고 몇가지 기능을 제공하기 위해 만들어진 프로젝트이다.

클라이언트(Config Client)와 서버(Config Server)로 구성되어 있는데, 스프링부트와 굉장히 잘 통합되어 있기 때문에 사용하기가 편하다. 하지만 서버를 별도로 구성하고 운영해야 하기 때문에 운영부담이 생길 수 밖에 없다. 

이 코드는 2% 부족하지만 기능에 대부분을 대체 할 수 있는 [Amazon S3](https://docs.aws.amazon.com/ko_kr/AmazonS3/latest/dev/Welcome.html)를 서버로 사용하고, 클라이언트는 확장해서 사용하는 코드이다. 

핵심은 `org.springframework.cloud.config.aws.s3.DefaultSimpleStoragePropertySourceLocator` 클래스이다. Spring Boot와 Spring Cloud Config에 의해 자동으로 해당 클래스가 동작하기 때문에 설정 정보가 있는 S3를 사용하기 위한 설정만 `bootstrap.yml`에 하면된다.

```xml
spring.cloud.config:
  s3:
    enabled: true
    credentials:
      accessKey: {aws.accessKey}
      secretKey: {aws.secretKey}
    buckets: {bucketName1},{bucketName2}
```

EC2에서 기동시 인스턴스 프로파일을 사용할 수 있는데, 다음과 같이 설정하면 된다.

```xml
spring.cloud.config:
  s3:
    enabled: true
    credentials:
      instanceProfile: true
    buckets: {bucketName1},{bucketName2}
```

설정된 bucket에서 `application`으로 시작하는 모든 properties, xml, yml 파일을 읽어 `PropertySource`에 변환 후 스프링부트 환경구성(Enviroment)에 등록한다.
등록하는 과정에서 스프링부트 기본전략과 유사하게 필터링되거나 우선 순위가 조정된다.

```
.
bucket
└── resources
    ├── application.yml
    ├── application-development.yml    
    └── application-production.yml
```   

위와 같이 S3 bucket에 3개에 설정 파일이 등록되어 있고, 스프링부트 프로파일로 `production`을 설정 후 애플리케이션을 기동하면 다음과 같이 로그가 출력된다. 

```
INFO 6651 --- [           main] c.a.s.SimpleStoragePropertySourceLocator : Fetching config from aws at: s3://bucket/application*.*
INFO 6688 --- [           main] b.c.PropertySourceBootstrapConfiguration : Located property source: CompositePropertySource [name='s3ConfigService', propertySources=[MapPropertySource {name='Amazon s3 resource [bucket='bucket' and object='application-production.yml']'}, MapPropertySource {name='Amazon s3 resource [bucket='bucket' and object='application.yml']'}]]
```

지금은 단순히 S3에서 설정 정보를 불러오는 기능만 구현되어 있지만, 향후 S3가 제공하는 버저닝, 변경 이벤트 통지, 암호화 등을 사용할 수 있도록 기능을 사용할 수 있도록 개선해볼 생각이다. 