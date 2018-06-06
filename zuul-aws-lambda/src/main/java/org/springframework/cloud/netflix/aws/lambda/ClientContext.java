package org.springframework.cloud.netflix.aws.lambda;

import java.util.HashMap;

/**
 * @author arawn.kr@gmail.com
 */
public interface ClientContext {

    Client getClient();
    Custom getCustom();
    Environment getEnvironment();


    class Client extends HashMap<String, String> { }
    class Custom extends HashMap<String, String> { }
    class Environment extends HashMap<String, String> { }

}
