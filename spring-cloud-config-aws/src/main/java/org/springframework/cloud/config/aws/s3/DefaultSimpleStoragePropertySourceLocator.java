package org.springframework.cloud.config.aws.s3;

import com.amazonaws.services.s3.AmazonS3;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import org.springframework.boot.env.PropertySourcesLoader;
import org.springframework.cloud.aws.core.io.s3.PathMatchingSimpleStorageResourcePatternResolver;
import org.springframework.cloud.aws.core.io.s3.SimpleStorageResourceLoader;
import org.springframework.core.env.CompositePropertySource;
import org.springframework.core.env.Environment;
import org.springframework.core.env.PropertySource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.ResourcePatternResolver;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j(topic = "org.springframework.cloud.config.aws.s3.SimpleStoragePropertySourceLocator")
public class DefaultSimpleStoragePropertySourceLocator extends AbstractSimpleStoragePropertySourceLocator {

    private PathMatchingSimpleStorageResourcePatternResolver resourceLoader;
    private PropertySourcesLoader propertiesLoader;

    public DefaultSimpleStoragePropertySourceLocator(SimpleStorageConfigProperties properties, AmazonS3 amazonS3) {
        super(properties, amazonS3);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        super.afterPropertiesSet();

        val resourcePatternResolver = new NoOpResourcePatternResolver();
        val simpleStorageResourceLoader = new SimpleStorageResourceLoader(amazonS3, resourcePatternResolver); {
            simpleStorageResourceLoader.afterPropertiesSet();
        }

        this.resourceLoader = new PathMatchingSimpleStorageResourcePatternResolver(amazonS3, simpleStorageResourceLoader, resourcePatternResolver);
        this.propertiesLoader = new PropertySourcesLoader();
    }

    @Override
    protected PropertySource<?> locateInternal(Environment environment) {
        List<String> profiles = new ArrayList<>(Arrays.asList(environment.getActiveProfiles()));
        List<Resource> resources = Arrays.stream(getProperties().getBuckets())
                                         .map(bucket -> String.format(PROPERTIES_LOCATION_PATTERN, bucket))
                                         .flatMap(location -> findResources(location).stream())
                                         .filter(resource -> resource.acceptsProfiles(environment))
                                         .sorted(new PropertiesResourceComparator(environment))
                                         .collect(Collectors.toList());

        profiles.add(0, null);

        CompositePropertySource propertySource = new CompositePropertySource("s3ConfigService");
        for(Resource resource : resources) {
            for(String profile : profiles) {
                try {
                    PropertySource<?> source = propertiesLoader.load(resource, profile);
                    if (Objects.nonNull(source)) propertySource.addPropertySource(source);
                } catch (IOException error) {
                    if (getProperties().isFailFast()) {
                        throw new IllegalArgumentException(error);
                    } else {
                        log.warn("Could not load PropertySource: {}", error.getMessage());
                    }
                }
            }
        }

        return propertySource.getPropertySources().isEmpty() ? null : propertySource;
    }

    protected List<PropertiesResource> findResources(String locationPattern) {
        try {
            log.info("Fetching config from aws at: {}", locationPattern);
            return Arrays.stream(resourceLoader.getResources(locationPattern))
                         .map(PropertiesResource::new)
                         .collect(Collectors.toList());
        } catch (IOException error) {
            throw new IllegalStateException(error);
        }
    }


    static class NoOpResourcePatternResolver implements ResourcePatternResolver {

        @Override
        public Resource[] getResources(String locationPattern) throws IOException {
            return null;
        }

        @Override
        public Resource getResource(String location) {
            return null;
        }

        @Override
        public ClassLoader getClassLoader() {
            return getClassLoader();
        }

    }

}
