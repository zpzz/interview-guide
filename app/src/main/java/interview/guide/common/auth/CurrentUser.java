package interview.guide.common.auth;

import java.util.Set;

public record CurrentUser(
    Long id,
    String username,
    String nickname,
    Set<String> roles
) {

    public boolean isAdmin() {
        return roles != null && roles.contains("ADMIN");
    }
}
