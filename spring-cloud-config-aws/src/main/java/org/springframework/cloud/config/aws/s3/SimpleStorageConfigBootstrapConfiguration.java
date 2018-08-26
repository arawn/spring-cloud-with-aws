package org.springframework.cloud.config.aws.s3;

import com.amazonaws.AmazonClientException;
import com.amazonaws.auth.*;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
@Configuration
@EnableConfigurationProperties
@ConditionalOnClass({ AmazonS3.class })
@ConditionalOnProperty(name = "spring.cloud.config.s3.enabled", havingValue = "true")
public class SimpleStorageConfigBootstrapConfiguration {

    @Bean
    public SimpleStorageConfigProperties simpleStorageConfigProperties() {
        return new SimpleStorageConfigProperties();
    }

    @Bean
    @ConditionalOnClass(SimpleStorageResourceLoader.class)
    @ConditionalOnMissingBean(SimpleStoragePropertySourceLocator.class)
    public SimpleStoragePropertySourceLocator simpleStorageResourceLoaderPropertySourceLocator(SimpleStorageConfigProperties properties) {
        return new DefaultSimpleStoragePropertySourceLocator(properties, createAmazonS3(properties));
    }

    protected AmazonS3 createAmazonS3(SimpleStorageConfigProperties properties) {
        return AmazonS3ClientBuilder.standard()
                                    .withCredentials(createCredentialsProvider(properties))
                                    .build();
    }

    protected AWSCredentialsProvider createCredentialsProvider(SimpleStorageConfigProperties properties) {
        if (properties.getCredentials().isInstanceProfile()) {
            return new DefaultAWSCredentialsProviderChain();
        } else if (properties.getCredentials().hasKey()) {
            val credentials = new BasicAWSCredentials(properties.getCredentials().getAccessKey(), properties.getCredentials().getSecretKey());
            return new AWSStaticCredentialsProvider(credentials);
        }
        return new EmptyCredentialsProviderChain();
    }


    class EmptyCredentialsProviderChain extends DefaultAWSCredentialsProviderChain {

        @Override
        public AWSCredentials getCredentials() {
            try {
                return super.getCredentials();
            } catch (AmazonClientException ace) { }

            log.debug("no credentials available; falling back to anonymous access");
            return null;
        }
    }

}
