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

    public String getStructuredData() {
        StringBuilder sb = new StringBuilder();
        buildTreeString(founder, 0, sb);
        return sb.toString();
    }

    private void buildTreeString(String current, int depth, StringBuilder sb) {
        for (int i = 0; i < depth; i++) sb.append("  ");
        sb.append("- ").append(current).append("\n");

        List<String> appointees = hierarchy.get(current);
        if (appointees != null) {
            for (String appointee : appointees) {
                buildTreeString(appointee, depth + 1, sb);
            }
        }
    }
}
