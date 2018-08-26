# Integration for AWS with Spring Cloud

[Spring Cloud](https://projects.spring.io/spring-cloud/)를 기반으로 AWS가 제공하는 관리형 서비스를 통합하는 코드 모음이다.
코드를 실행해 보기 위해서는 적절한 권한을 가진 [AWS 액세스 키](https://docs.aws.amazon.com/ko_kr/IAM/latest/UserGuide/id_credentials_access-keys.html)가 필요합니다.

## spring-cloud-config-aws

애플리케이션 기동시 필요한 구성 정보(application.yml과 같은)를 [Amazon S3](https://docs.aws.amazon.com/ko_kr/AmazonS3/latest/dev/Welcome.html)에 두고 불러올 수 있도록 구현한 코드이다.   

## spring-cloud-netflix-zuul-aws

[AWS Lambda](https://aws.amazon.com/ko/lambda/)를 Spring Cloud Netflix Zuul과 통합하고 외부에 HTTP API로 제공하는 코드이다.

