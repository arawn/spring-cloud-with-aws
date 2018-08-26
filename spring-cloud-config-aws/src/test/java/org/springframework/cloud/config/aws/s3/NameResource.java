package org.springframework.cloud.config.aws.s3;

import org.springframework.core.io.AbstractResource;

import java.io.IOException;
import java.io.InputStream;

/**
 * @author arawn.kr@gmail.com
 */
class NameResource extends AbstractResource {

    private String resourceName;
    public NameResource(String resourceName) {
        this.resourceName = resourceName;
    }

    @Override
    public String getFilename() {
        return resourceName;
    }

    @Override
    public String getDescription() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        throw new UnsupportedOperationException();
    }

}
