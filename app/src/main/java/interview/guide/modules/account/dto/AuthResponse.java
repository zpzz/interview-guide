package interview.guide.modules.account.dto;

public record AuthResponse(
    String token,
    UserDTO user
) {}
