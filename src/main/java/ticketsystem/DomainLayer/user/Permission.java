package ticketsystem.DomainLayer.user;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Optional;
import java.util.Set;

public enum Permission {

    MANAGE_EVENT_INVENTORY("inventory:event:manage"),
    CONFIGURE_HALL_AND_MAP("hall:config:setup"),
    MANAGE_INQUIRIES("inquiry:response:manage"),
    SET_PURCHASING_POLICY("policy:purchasing:setup"),
    SET_DISCOUNT_POLICY("policy:discount:setup"),
    VIEW_PURCHASE_HISTORY("history:purchases:view"),
    GENERATE_SALES_REPORT("reports:sales:generate");

    private final String key;

    Permission(String key) {
        this.key = key;
    }

    /**
     * Returns the string representation of the permission.
     */
    public String getKey() {
        return key;
    }

    /**
     * Utility method to find a Permission by its string key.
     */
    public static Optional<Permission> fromKey(String key) {
        return Arrays.stream(values())
                .filter(p -> p.key.equalsIgnoreCase(key))
                .findFirst();
    }
<<<<<<< HEAD
<<<<<<< HEAD
=======
>>>>>>> 28466bf (Finish use-case assign manager to company without notifications)

    /**
     * Returns a set containing all available permissions in the system.
     * For assigning full access to Founders or Owners.
     */
    public static Set<Permission> getAllPermissions() {
        return EnumSet.allOf(Permission.class);
    }
<<<<<<< HEAD
=======
>>>>>>> ba7482c (Update Permission enum class)
=======
>>>>>>> 28466bf (Finish use-case assign manager to company without notifications)
}