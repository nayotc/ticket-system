package ticketsystem.ApplicationLayer;

import java.util.function.Consumer;

public interface IBrodcaster {

    Runnable registerListener(String sessionId, Consumer<String> notifier);
}
