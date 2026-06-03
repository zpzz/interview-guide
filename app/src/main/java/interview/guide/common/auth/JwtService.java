package interview.guide.common.auth;

import interview.guide.common.exception.BusinessException;
import interview.guide.common.exception.ErrorCode;
import org.springframework.stereotype.Service;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class JwtService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final AuthProperties authProperties;
    private final ObjectMapper objectMapper;

    public JwtService(AuthProperties authProperties, ObjectMapper objectMapper) {
        this.authProperties = authProperties;
        this.objectMapper = objectMapper;
    }

    public String generateToken(CurrentUser user) {
        try {
            long now = Instant.now().getEpochSecond();
            Map<String, Object> header = Map.of(
                "alg", "HS256",
                "typ", "JWT"
            );
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", String.valueOf(user.id()));
            payload.put("username", user.username());
            payload.put("nickname", user.nickname());
            payload.put("roles", user.roles());
            payload.put("iat", now);
            payload.put("exp", now + authProperties.getTokenTtlSeconds());

            String headerPart = encodeJson(header);
            String payloadPart = encodeJson(payload);
            String signingInput = headerPart + "." + payloadPart;
            return signingInput + "." + sign(signingInput);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.INTERNAL_ERROR, "生成登录凭证失败");
        }
    }

    public CurrentUser parseToken(String token) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭证无效");
            }

            String signingInput = parts[0] + "." + parts[1];
            String expectedSignature = sign(signingInput);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭证无效");
            }

            Map<String, Object> payload = objectMapper.readValue(
                decode(parts[1]),
                new TypeReference<>() {
                }
            );
            long exp = ((Number) payload.get("exp")).longValue();
            if (Instant.now().getEpochSecond() > exp) {
                throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录已过期");
            }

            @SuppressWarnings("unchecked")
            List<String> roleList = (List<String>) payload.getOrDefault("roles", List.of());
            Set<String> roles = roleList.stream().collect(Collectors.toUnmodifiableSet());
            return new CurrentUser(
                Long.valueOf(String.valueOf(payload.get("sub"))),
                String.valueOf(payload.get("username")),
                String.valueOf(payload.get("nickname")),
                roles
            );
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED, "登录凭证无效");
        }
    }

    private String encodeJson(Object value) throws Exception {
        return encode(objectMapper.writeValueAsBytes(value));
    }

    private String encode(byte[] bytes) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private String sign(String input) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        SecretKeySpec key = new SecretKeySpec(
            authProperties.getJwtSecret().getBytes(StandardCharsets.UTF_8),
            HMAC_ALGORITHM
        );
        mac.init(key);
        return encode(mac.doFinal(input.getBytes(StandardCharsets.UTF_8)));
    }

    private boolean constantTimeEquals(String left, String right) {
        return MessageDigestUtil.constantTimeEquals(left, right);
    }
}
