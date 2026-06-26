package ticketsystem.InfrastructureLayer;

import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import ticketsystem.ApplicationLayer.UserService;

@RestController
@RequestMapping("/api/session")
public class SessionExitController {

    private final UserService userService;

    public SessionExitController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/exit", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> exit(@RequestBody(required = false) String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            userService.exit(sessionToken.trim());
        } catch (IllegalArgumentException | IllegalStateException ignored) {
        }

        return ResponseEntity.noContent().build();
    }
}
