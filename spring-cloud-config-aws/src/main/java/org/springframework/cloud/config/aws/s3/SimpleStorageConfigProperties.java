package org.springframework.cloud.config.aws.s3;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;

/**
 * @author arawn.kr@gmail.com
 */
@Data
@Validated
@ConfigurationProperties("spring.cloud.config.s3")
public class SimpleStorageConfigProperties {

    private Credentials credentials = new Credentials();
    private String[] buckets;
    private boolean failFast = false;


    @Data
    public static class Credentials {

        private String accessKey;
        private String secretKey;
        private boolean instanceProfile = false;

        public boolean hasKey() {
            return StringUtils.hasText(accessKey) && StringUtils.hasText(secretKey);
        }

    }

}
