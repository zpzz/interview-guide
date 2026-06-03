package interview.guide.common.auth;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret = "interview-guide-dev-secret-change-me";

    private long tokenTtlSeconds = 604800;
}
