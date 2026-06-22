package interview.guide.common.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.http.client.BufferingClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpResponse;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Slf4j
public final class ApiPathResolver {

  private static final int DEFAULT_CONNECT_TIMEOUT = 10000;
  private static final int DEFAULT_READ_TIMEOUT = 300000;

  private static final Pattern TRAILING_VERSION = Pattern.compile("/v\\d+[a-zA-Z0-9]*$");

  private ApiPathResolver() {}

  public static OpenAiApi buildOpenAiApi(String baseUrl, String apiKey) {
    return buildOpenAiApi(baseUrl, apiKey, DEFAULT_CONNECT_TIMEOUT, DEFAULT_READ_TIMEOUT);
  }

  public static OpenAiApi buildOpenAiApi(String baseUrl, String apiKey,
      int connectTimeout, int readTimeout) {
    SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
    requestFactory.setConnectTimeout(connectTimeout);
    requestFactory.setReadTimeout(readTimeout);

    BufferingClientHttpRequestFactory bufferingRequestFactory = new BufferingClientHttpRequestFactory(requestFactory);
    RestClient.Builder restClientBuilder = RestClient.builder()
        .requestFactory(bufferingRequestFactory)
        .requestInterceptor((request, body, execution) -> {
          log.info(
              "[LLM HTTP] request: method={}, uri={}, contentType={}, bodyBytes={}",
              request.getMethod(),
              request.getURI(),
              request.getHeaders().getContentType(),
              body == null ? 0 : body.length
          );
          ClientHttpResponse response = execution.execute(request, body);
          logResponse(response, request.getURI().toString());
          return response;
        })
        .messageConverters(ApiPathResolver::customizeMessageConverters);

    OpenAiApi.Builder apiBuilder = OpenAiApi.builder()
        .baseUrl(baseUrl)
        .apiKey(apiKey)
        .restClientBuilder(restClientBuilder);
    if (baseUrlContainsVersion(baseUrl)) {
      apiBuilder.completionsPath("/chat/completions").embeddingsPath("/embeddings");
    }
    return apiBuilder.build();
  }

  private static void customizeMessageConverters(List<HttpMessageConverter<?>> converters) {
    for (HttpMessageConverter<?> converter : converters) {
      if (converter instanceof MappingJackson2HttpMessageConverter jacksonConverter) {
        List<MediaType> supportedMediaTypes = new ArrayList<>(jacksonConverter.getSupportedMediaTypes());
        if (!supportedMediaTypes.contains(MediaType.APPLICATION_OCTET_STREAM)) {
          supportedMediaTypes.add(MediaType.APPLICATION_OCTET_STREAM);
          jacksonConverter.setSupportedMediaTypes(supportedMediaTypes);
        }
      }
    }
  }

  private static void logResponse(ClientHttpResponse response, String uri) throws IOException {
    byte[] body = response.getBody().readAllBytes();
    String contentType = response.getHeaders().getContentType() != null
        ? response.getHeaders().getContentType().toString()
        : "";
    log.info(
        "[LLM HTTP] response: uri={}, status={}, contentType={}, body={}",
        uri,
        response.getStatusCode().value(),
        contentType,
        abbreviateBody(body)
    );
  }

  private static String abbreviateBody(byte[] body) {
    if (body == null || body.length == 0) {
      return "[empty]";
    }
    String text = new String(body, StandardCharsets.UTF_8)
        .replaceAll("\\s+", " ")
        .trim();
    if (text.isEmpty()) {
      return "[binary:" + body.length + " bytes]";
    }
    return text.length() <= 800 ? text : text.substring(0, 800) + "...";
  }

  public static boolean baseUrlContainsVersion(String baseUrl) {
    if (baseUrl == null || baseUrl.isBlank()) {
      return false;
    }
    String stripped = stripTrailingSlashes(baseUrl.trim());
    return TRAILING_VERSION.matcher(stripped).find();
  }

  public static String stripTrailingSlashes(String value) {
    if (value == null) {
      return "";
    }
    String result = value.trim();
    while (result.endsWith("/")) {
      result = result.substring(0, result.length() - 1);
    }
    return result;
  }
}
