package org.springframework.cloud.config.aws.s3;

import lombok.val;
import org.junit.Test;
import org.springframework.cloud.config.aws.s3.SimpleStoragePropertySourceLocator.PropertiesResource;
import org.springframework.cloud.config.aws.s3.SimpleStoragePropertySourceLocator.PropertiesResourceComparator;
import org.springframework.mock.env.MockEnvironment;

import java.util.ArrayList;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class PropertiesResourceComparatorTest {

    @Test
    public void compare() {
        val environment = new MockEnvironment(); {
            environment.setActiveProfiles("default", "first", "second");
        }
        val comparator = new PropertiesResourceComparator(environment);

        val resources = new ArrayList<PropertiesResource>();
        resources.add(new PropertiesResource(new NameResource("application-second.yml")));
        resources.add(new PropertiesResource(new NameResource("application.properties")));
        resources.add(new PropertiesResource(new NameResource("application-third.yml")));
        resources.add(new PropertiesResource(new NameResource("application.yml")));
        resources.add(new PropertiesResource(new NameResource("application-first.yml")));
        resources.add(new PropertiesResource(new NameResource("application.xml")));
        resources.add(new PropertiesResource(new NameResource("application-default.yml")));
        resources.sort(comparator);

        assertThat(resources.get(0).getProfile(), is("second"));
        assertThat(resources.get(1).getProfile(), is("first"));
        assertThat(resources.get(2).getProfile(), is("default"));
        assertThat(resources.get(3).getProfile(), is(nullValue()));
        assertThat(resources.get(4).getProfile(), is(nullValue()));
        assertThat(resources.get(5).getProfile(), is(nullValue()));
        assertThat(resources.get(6).getProfile(), is("third"));
    }

}
