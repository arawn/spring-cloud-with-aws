package org.springframework.cloud.config.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;

import java.util.Objects;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
@Data
@AllArgsConstructor
public abstract class AbstractSimpleStoragePropertySourceLocator implements SimpleStoragePropertySourceLocator {

    protected SimpleStorageConfigProperties properties;
    protected AmazonS3 amazonS3;

    @Override
    public PropertySource<?> locate(Environment environment) {
        if (Objects.isNull(properties.getBuckets()) || properties.getBuckets().length == 0) {
            log.debug("buckets is empty.");
            return null;
        }

        return locateInternal(environment);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        Objects.requireNonNull(amazonS3, "AmazonS3 is required");
        Objects.requireNonNull(properties, "SimpleStorageConfigProperties is required");
    }


    protected abstract PropertySource<?> locateInternal(Environment environment);

}
