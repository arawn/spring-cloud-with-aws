package org.springframework.cloud.config.aws.s3;

import lombok.val;
import org.junit.Test;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * @author arawn.kr@gmail.com
 */
public class PropertiesResourceTest {

    @Test
    public void extractProfile() {
        val resource = new SimpleStoragePropertySourceLocator.PropertiesResource(new NameResource(""));

        assertThat(resource.extractProfile("unknown.txt"), is(nullValue()));
        assertThat(resource.extractProfile("0123456789.tar.gz"), is(nullValue()));

        assertThat(resource.extractProfile("application"), is(nullValue()));
        assertThat(resource.extractProfile("application.yml"), is(nullValue()));
        assertThat(resource.extractProfile("application-.yml"), is(nullValue()));
        assertThat(resource.extractProfile("application-profile.yml"), is("profile"));
        assertThat(resource.extractProfile("application-profile-profile.yml"), is("profile-profile"));
    }

}
