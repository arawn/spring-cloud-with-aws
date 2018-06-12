package org.springframework.cloud.netflix.aws.lambda.support;

import lombok.val;
import org.springframework.http.HttpMethod;
import org.springframework.http.InvalidMediaTypeException;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConversionException;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.util.StringUtils;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * @author arawn.kr@gmail.com
 */
public class ServerHttpRequestUtils {

    private ServerHttpRequestUtils() { }


    public static boolean isMultipart(ServerHttpRequest request) {
        if (request.getMethod() != HttpMethod.POST) {
            return false;
        }

        MediaType contentType = ServerHttpRequestUtils.getContentType(request);
        if (Objects.nonNull(contentType)) {
            return contentType.toString().toLowerCase().startsWith("multipart/");
        }
        return false;
    }

    public static MediaType getContentType(ServerHttpRequest request) {
        return getContentType(request, null);
    }

    public static MediaType getContentType(ServerHttpRequest request, MediaType defaultType) {
        try {
            MediaType contentType = request.getHeaders().getContentType();
            if (Objects.nonNull(contentType)) {
                return contentType;
            }
            return defaultType;
        } catch (InvalidMediaTypeException error) {
            throw new HttpMessageConversionException(error.getMessage(), error);
        }
    }

    public static MultiValueMap<String, String> getQueryParams(ServerHttpRequest request) throws UnsupportedEncodingException {
        String query = request.getURI().getQuery();
        if (StringUtils.hasText(query)) {
            MultiValueMap<String, String> queryParams = new LinkedMultiValueMap<>();
            String[] pairs = query.split("&");
            for (String pair : pairs) {
                int idx = pair.indexOf("=");
                String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                queryParams.add(key, value);
            }
            return queryParams;
        }
        return new LinkedMultiValueMap<>();
    }

    public static Map<String, String> convertSingleValueMap(MultiValueMap<String, String> source) {
        if (Objects.isNull(source) || source.isEmpty()) {
            return new HashMap<>();
        }

        Map<String, String> target = new HashMap<>(source.size());
        for (Map.Entry<String, List<String>> entry : source.entrySet()) {
            if (entry.getValue().size() == 1) {
                target.put(entry.getKey(), entry.getValue().get(0));
            } else {
                StringBuilder builder = new StringBuilder();
                for (Object value : entry.getValue()) {
                    if (builder.length() > 0) {
                        builder.append(',');
                    }
                    builder.append(value);
                }
                target.put(entry.getKey(), builder.toString());
            }
        }
        return target;
    }

}
