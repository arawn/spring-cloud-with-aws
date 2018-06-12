package org.springframework.cloud.netflix.aws.lambda;

import com.amazonaws.services.lambda.model.InvokeResult;
import lombok.val;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.AbstractClientHttpResponse;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * @author arawn.kr@gmail.com
 */
public class AWSLambdaClientResponse extends AbstractClientHttpResponse {

    private final InvokeResult invokeResult;

    public AWSLambdaClientResponse(InvokeResult invokeResult) {
        this.invokeResult = invokeResult;
    }

    @Override
    public int getRawStatusCode() throws IOException {
        if (StringUtils.hasText(invokeResult.getFunctionError())) {
            val series = HttpStatus.Series.valueOf(invokeResult.getStatusCode());
            if (series == HttpStatus.Series.CLIENT_ERROR || series == HttpStatus.Series.SERVER_ERROR) {
                return invokeResult.getStatusCode();
            }
            return 500;
        }
        return invokeResult.getStatusCode();
    }

    @Override
    public String getStatusText() throws IOException {
        if (StringUtils.hasText(invokeResult.getFunctionError())) {
            return invokeResult.getFunctionError();
        }
        return getStatusCode().getReasonPhrase();
    }

    @Override
    public HttpHeaders getHeaders() {
        val headers = new HttpHeaders(); {
            invokeResult.getSdkHttpMetadata().getHttpHeaders().forEach(headers::add);
        }
        return HttpHeaders.readOnlyHttpHeaders(headers);
    }

    @Override
    public InputStream getBody() throws IOException {
        return new ByteBufferBackedInputStream(invokeResult.getPayload());
    }

    @Override
    public void close() {

    }

    @Override
    public String toString() {
        return String.format("AWSLambdaClientResponse { StatusCode: %d, AWS Request ID: %s }", invokeResult.getStatusCode(), invokeResult.getSdkResponseMetadata().getRequestId());
    }


    class ByteBufferBackedInputStream extends InputStream {

        private final ByteBuffer buffer;
        private ByteBufferBackedInputStream(ByteBuffer buffer) { this.buffer = buffer; }

        @Override
        public int available() {
            return buffer.remaining();
        }

        @Override
        public int read() throws IOException {
            return buffer.hasRemaining() ? (buffer.get() & 0xFF) : -1;
        }

        @Override
        public int read(byte[] bytes, int off, int len) throws IOException {
            if (!buffer.hasRemaining()) return -1;
            len = Math.min(len, buffer.remaining());
            buffer.get(bytes, off, len);
            return len;
        }

    }

}
