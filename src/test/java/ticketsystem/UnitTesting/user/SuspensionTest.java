package ticketsystem.UnitTesting.user;

import org.junit.jupiter.api.Test;

import ticketsystem.DomainLayer.user.Suspension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

public class SuspensionTest {

    private final Long adminId = 1L;
    private final LocalDateTime startDate = LocalDateTime.now().minusDays(1);
    private final String reason = "Violation of system rules";

    @Test
    void GivenValidTemporarySuspension_WhenCreateSuspension_ThenSuspensionIsActive() {
        // Arrange
        LocalDateTime endDate = LocalDateTime.now().plusDays(1);

        // Act
        Suspension suspension = new Suspension(adminId, startDate, endDate, reason);

        // Assert
        assertTrue(suspension.isActive());
        assertFalse(suspension.isRevoked());
    }

    @Test
    void GivenValidPermanentSuspension_WhenCreateSuspension_ThenSuspensionIsActive() {
        // Act
        Suspension suspension = new Suspension(adminId, startDate, null, reason);

        // Assert
        assertTrue(suspension.isActive());
        assertFalse(suspension.isRevoked());
    }

    @Test
    void GivenExpiredSuspension_WhenIsActive_ThenReturnFalse() {
        // Arrange
        LocalDateTime expiredEndDate = LocalDateTime.now().minusHours(1);

        // Act
        Suspension suspension = new Suspension(adminId, startDate, expiredEndDate, reason);

        // Assert
        assertFalse(suspension.isActive());
    }

    @Test
    void GivenActiveSuspension_WhenRevoke_ThenSuspensionIsNotActiveAndRevoked() {
        // Arrange
        Suspension suspension = new Suspension(
                adminId,
                startDate,
                LocalDateTime.now().plusDays(1),
                reason
        );

        // Act
        suspension.revoke();

        // Assert
        assertTrue(suspension.isRevoked());
        assertFalse(suspension.isActive());
    }

    @Test
    void GivenNullAdminId_WhenCreateSuspension_ThenExceptionIsThrown() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Suspension(null, startDate, null, reason)
        );
    }

    @Test
    void GivenNullStartDate_WhenCreateSuspension_ThenExceptionIsThrown() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Suspension(adminId, null, null, reason)
        );
    }

    @Test
    void GivenNullReason_WhenCreateSuspension_ThenExceptionIsThrown() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Suspension(adminId, startDate, null, null)
        );
    }

    @Test
    void GivenBlankReason_WhenCreateSuspension_ThenExceptionIsThrown() {
        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Suspension(adminId, startDate, null, "   ")
        );
    }

    @Test
    void GivenEndDateBeforeStartDate_WhenCreateSuspension_ThenExceptionIsThrown() {
        // Arrange
        LocalDateTime invalidEndDate = startDate.minusDays(1);

        // Act & Assert
        assertThrows(IllegalArgumentException.class, () ->
                new Suspension(adminId, startDate, invalidEndDate, reason)
        );
    }
}