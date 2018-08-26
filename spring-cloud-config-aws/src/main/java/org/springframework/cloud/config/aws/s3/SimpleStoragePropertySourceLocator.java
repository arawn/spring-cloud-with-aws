package org.springframework.cloud.config.aws.s3;

import lombok.experimental.Delegate;
import lombok.val;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.cloud.bootstrap.config.PropertySourceLocator;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * @author arawn.kr@gmail.com
 */
public interface SimpleStoragePropertySourceLocator extends PropertySourceLocator, InitializingBean {

    String DEFAULT_PROPERTIES_NAME = "application";

    // starting name with DEFAULT_PROPERTIES_NAME, all files in s3 bucket
    String PROPERTIES_LOCATION_PATTERN = "s3://%s/" + DEFAULT_PROPERTIES_NAME + "*.*";
    // DEFAULT_PROPERTIES_NAME-{profile}.yml (properties, xml, etc)
    Pattern EXTRACT_PROFILE_PATTERN = Pattern.compile("^("+ DEFAULT_PROPERTIES_NAME + "-)(.*)\\.(.*)$");


    class PropertiesResource implements Resource {

        @Delegate
        private final Resource properties;
        private final String profile;

        protected PropertiesResource(Resource properties) {
            this.properties = properties;
            this.profile = extractProfile(properties.getFilename());
        }

        protected String extractProfile(String fileName) {
            val matcher = EXTRACT_PROFILE_PATTERN.matcher(fileName);
            if (matcher.find()) {
                String profile = matcher.group(2);
                if (StringUtils.hasText(profile)) {
                    return profile;
                }
            }
            return null;
        }

        protected boolean acceptsProfiles(Environment environment) {
            return Objects.isNull(profile) || environment.acceptsProfiles(profile);
        }

        protected String getProfile() {
            return profile;
        }

    }

    /*
     * sort resources by spring profile.
     */
    class PropertiesResourceComparator implements Comparator<PropertiesResource> {

        private List<String> profiles;
        public PropertiesResourceComparator(Environment environment) {
            this.profiles = Arrays.asList(environment.getActiveProfiles());
        }

        @Override
        public int compare(PropertiesResource first, PropertiesResource second) {
            val firstProfile = first.getProfile();
            val firstProfileOrder = profiles.indexOf(firstProfile);
            val secondProfile = second.getProfile();
            val secondProfileOrder = profiles.indexOf(secondProfile);

            if (Objects.nonNull(firstProfile) && Objects.nonNull(secondProfile)) {
                if (firstProfileOrder < 0) {
                    return 1;
                } else if (secondProfileOrder < 0) {
                    return -1;
                } else {
                    return Integer.compare(firstProfileOrder, secondProfileOrder) * -1;
                }
            } else if (Objects.isNull(firstProfile) && Objects.isNull(secondProfile)) {
                return 0;
            } else if (Objects.isNull(firstProfile) && secondProfileOrder < 0) {
                return -1;
            } else if (Objects.isNull(firstProfile)) {
                return 1;
            } else if(firstProfileOrder < 0) {
                return 1;
            } else {
                return -1;
            }
        }

    }

}
