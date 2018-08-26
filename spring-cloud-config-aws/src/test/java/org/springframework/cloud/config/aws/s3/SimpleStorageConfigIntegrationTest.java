package org.springframework.cloud.config.aws.s3;

import lombok.extern.slf4j.Slf4j;
import org.hamcrest.Matchers;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

import java.io.IOException;

import static org.hamcrest.Matchers.is;
import static org.junit.Assert.assertThat;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
@RunWith(SpringRunner.class)
@SpringBootTest( classes = SimpleStorageConfigIntegrationTest.SimpleStoragePropertySourceLocatorTestConfig.class
               , webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles({ "default", "local2", "s3" })
public class SimpleStorageConfigIntegrationTest {

    @Autowired
    private ConfigurableEnvironment environment;

    @Test
    public void propertySources() throws IOException {
        environment.getPropertySources().forEach(ps -> log.info("{}", ps));
        assertThat(environment.getProperty("test.value"), is("application-s3.yml"));
    }


    @SpringBootConfiguration
    static class SimpleStoragePropertySourceLocatorTestConfig {

    }

}