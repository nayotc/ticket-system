package ticketsystem.PresentationLayer.Components;

import com.vaadin.flow.component.html.Span;

public class StatusBadge extends Span {

    public enum Type {
        SUCCESS,
        WARNING,
        ERROR,
        INFO,
        NEUTRAL
    }

    public StatusBadge(String text, Type type) {
        super(text);

        addClassName("status-badge");
        addClassName(resolveClass(type));
    }

    private String resolveClass(Type type) {
        if (type == null) {
            return "status-neutral";
        }

        return switch (type) {
            case SUCCESS -> "status-success";
            case WARNING -> "status-warning";
            case ERROR -> "status-error";
            case INFO -> "status-info";
            case NEUTRAL -> "status-neutral";
        };
    }
}