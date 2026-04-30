package ticketsystem.DomainLayer.IRepository;

import java.util.List;

public interface IWaitingQueueRepository {

    void enqueueUser(long eventId, String sessionId);

    java.util.List<String> dequeueBatch(long eventId, long batchSize);

    int getQueueSize(long eventId);

    void removeUserFromQueue(long eventId, String sessionId);

    public List<String> clearQueue(long eventId);

}
