package ticketsystem.InfrastructureLayer;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

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

    private static final long EXIT_DELAY_MS = 2000;

    private final UserService userService;
    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "session-exit");
                thread.setDaemon(true);
                return thread;
            });
    private final ConcurrentHashMap<String, ScheduledFuture<?>> pendingExits =
            new ConcurrentHashMap<>();

    public SessionExitController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping(value = "/exit", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> exit(@RequestBody(required = false) String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        scheduleExit(sessionToken.trim());
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/cancel-exit", consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<Void> cancelExit(@RequestBody(required = false) String sessionToken) {
        if (sessionToken == null || sessionToken.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        cancelScheduledExit(sessionToken.trim());
        return ResponseEntity.noContent().build();
    }

    private void scheduleExit(String token) {
        cancelScheduledExit(token);

        ScheduledFuture<?> future = scheduler.schedule(() -> {
            pendingExits.remove(token);
            try {
                userService.exit(token);
            } catch (IllegalArgumentException | IllegalStateException ignored) {
            }
        }, EXIT_DELAY_MS, TimeUnit.MILLISECONDS);

        pendingExits.put(token, future);
    }

    private void cancelScheduledExit(String token) {
        ScheduledFuture<?> future = pendingExits.remove(token);
        if (future != null) {
            future.cancel(false);
        }
    }
}
