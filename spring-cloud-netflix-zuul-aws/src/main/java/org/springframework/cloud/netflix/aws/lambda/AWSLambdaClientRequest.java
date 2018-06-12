package org.springframework.cloud.netflix.aws.lambda;

import com.amazonaws.adapters.types.StringToByteBufferAdapter;
import com.amazonaws.services.lambda.AWSLambdaAsync;
import com.amazonaws.services.lambda.model.InvocationType;
import com.amazonaws.services.lambda.model.InvokeRequest;
import com.amazonaws.services.lambda.model.InvokeResult;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.client.AbstractClientHttpRequest;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.util.Objects;

/**
 * @author arawn.kr@gmail.com
 */
@Slf4j
public class AWSLambdaClientRequest extends AbstractClientHttpRequest {

    private final AWSLambdaAsync lambdaClient;
    private final String functionArn;
    private final ClientContext clientContext;
    private final ByteBuffer payload;

    public AWSLambdaClientRequest(AWSLambdaAsync lambdaClient, String functionArn) {
        this(lambdaClient, functionArn, null, (ByteBuffer) null);
    }

    public AWSLambdaClientRequest(AWSLambdaAsync lambdaClient, String functionArn, ClientContext clientContext, String payload) {
        this(lambdaClient, functionArn, clientContext, new StringToByteBufferAdapter().adapt(payload));
    }

    public AWSLambdaClientRequest(AWSLambdaAsync lambdaClient, String functionArn, ClientContext clientContext, ByteBuffer payload) {
        this.lambdaClient = Objects.requireNonNull(lambdaClient, "람다 클라이언트가 필요합니다.");
        this.functionArn = validateFunctionArn(functionArn);
        this.clientContext = clientContext;
        this.payload = payload;
    }

    protected String validateFunctionArn(String functionArn) {
        if (Objects.requireNonNull(functionArn).startsWith("arn:aws:lambda:")) {
            return functionArn;
        }
        throw new IllegalArgumentException("람다 ARN(amazon resource name)이 아닙니다.");
    }

    @Override
    public HttpMethod getMethod() {
        return HttpMethod.POST;
    }

    @Override
    public URI getURI() {
        try {
            return new URI(functionArn);
        } catch (URISyntaxException error) {
            throw new IllegalArgumentException(error);
        }
    }

    @Override
    protected OutputStream getBodyInternal(HttpHeaders headers) throws IOException {
        return new ByteBufferBackedOutputStream(payload);
    }

    @Override
    protected ClientHttpResponse executeInternal(HttpHeaders headers) throws IOException {
        return new AWSLambdaClientResponse(invoke(headers));
    }

    protected InvokeResult invoke(HttpHeaders headers) {
        InvokeRequest request = new InvokeRequest().withFunctionName(functionArn)
                                                   .withInvocationType(InvocationType.RequestResponse);
        if (Objects.nonNull(clientContext)) {
            request.setClientContext(clientContext.toString());
        }
        if (Objects.nonNull(payload)) {
            request.setPayload(payload);
        }

        return lambdaClient.invoke(request);
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("AWSLambdaClientRequest { ");
        sb.append("functionArn='").append(functionArn).append('\'');
        sb.append(", clientContext=").append(clientContext);
        sb.append('}');
        return sb.toString();
    }


    static class ByteBufferBackedOutputStream extends OutputStream {

        private final ByteBuffer buffer;
        private ByteBufferBackedOutputStream(ByteBuffer buffer) {
            this.buffer = buffer;
        }

        @Override
        public void write(int b) throws IOException {
            buffer.put((byte) b);
        }

        @Override
        public void write(byte[] bytes, int off, int len) throws IOException {
            buffer.put(bytes, off, len);
        }

    }

}
