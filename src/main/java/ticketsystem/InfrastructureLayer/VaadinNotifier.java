package ticketsystem.InfrastructureLayer;

import ticketsystem.ApplicationLayer.INotifier;
import ticketsystem.InfrastructureLayer.Broadcaster;

public class VaadinNotifier implements INotifier {
    private final Broadcaster broadcaster;

    public VaadinNotifier(Broadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @Override
    public void notifyUser(String sessionId, String message) {
        broadcaster.broadcast(sessionId, message);
    }
}
