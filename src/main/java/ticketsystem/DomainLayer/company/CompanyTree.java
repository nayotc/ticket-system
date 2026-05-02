package ticketsystem.DomainLayer.company;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CompanyTree {
    private final Map<String, List<String>> hierarchy;
    private final String founder;

    public CompanyTree(String founder) {
        this.founder = founder;
        this.hierarchy = new HashMap<>();
        this.hierarchy.put(founder, new ArrayList<>());
    }

    public void addAppointment(String appointer, String appointee) {
        hierarchy.computeIfAbsent(appointer, k -> new ArrayList<>()).add(appointee);
        hierarchy.putIfAbsent(appointee, new ArrayList<>());
    }

    // --- Deletion Methods ---

    /**
     * Removes a user from the roles tree and reassigns their appointees to their appointer.
     */
    public void removeNode(String userToRemove) {
        // Edge case: Cannot remove the founder
        if (userToRemove.equals(founder)) {
            return; 
        }

        String appointerOfDeletedUser = null;

        // 1. Find the parent (appointer) of the user we want to remove
        for (Map.Entry<String, List<String>> entry : hierarchy.entrySet()) {
            if (entry.getValue().contains(userToRemove)) {
                appointerOfDeletedUser = entry.getKey();
                // Remove the user from their appointer's list
                entry.getValue().remove(userToRemove);
                break;
            }
        }

        // If the user wasn't found in the tree at all, we can stop here
        if (appointerOfDeletedUser == null) {
            return;
        }

        // 2. Extract all the appointees (children) of the user to be removed
        // hierarchy.remove(userToRemove) removes the node and returns its list of appointees
        List<String> orphanedAppointees = hierarchy.remove(userToRemove);

        // 3. Reassign the orphaned appointees to the appointer of the deleted user
        if (orphanedAppointees != null && !orphanedAppointees.isEmpty()) {
            hierarchy.get(appointerOfDeletedUser).addAll(orphanedAppointees);
        }
    }
    
    // ---------------------------------

    public String getStructuredData(Map<String, String> userPermissions) {
            StringBuilder sb = new StringBuilder();
            buildTreeString(founder, 0, sb, userPermissions);
            return sb.toString();
        }

    private void buildTreeString(String current, int depth, StringBuilder sb, Map<String, String> userPermissions) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("- ").append(current);

        if (userPermissions != null && userPermissions.containsKey(current)) {
            sb.append(" [").append(userPermissions.get(current)).append("]");
        }
        sb.append("\n");

        List<String> appointees = hierarchy.get(current);
        if (appointees != null) {
            for (String appointee : appointees) {
                buildTreeString(appointee, depth + 1, sb, userPermissions);
            }
        }
    }
}