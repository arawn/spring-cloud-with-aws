package org.springframework.cloud.netflix.aws.lambda;

import org.springframework.http.server.ServerHttpRequest;

/**
 * @author arawn.kr@gmail.com
 */
public interface AWSLambdaClientContextFactory {

    ClientContext create(ServerHttpRequest request);

}
