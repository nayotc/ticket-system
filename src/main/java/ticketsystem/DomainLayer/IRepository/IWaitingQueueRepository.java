package ticketsystem.DomainLayer.IRepository;

public interface IWaitingQueueRepository {

    void enqueueUser(long eventId, String sessionId);

    java.util.List<String> dequeueBatch(long eventId, long batchSize);

    int getQueueSize(long eventId);

    void removeUserFromQueue(long eventId, String sessionId);
}
