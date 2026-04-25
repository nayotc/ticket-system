package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;

public class WaitingQueueRepository implements IWaitingQueueRepository {

    private final Map<Long, Queue<String>> eventQueues; // ConcurrentHashMap with eventid and queue of sessionIds

    public WaitingQueueRepository() {
        this.eventQueues = new ConcurrentHashMap<>();
    }

    private Queue<String> getOrCreateQueue(long eventId) {
        return eventQueues.computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
    }

    @Override
    public void enqueueUser(long eventId, String sessionId) {
        Queue<String> queue = getOrCreateQueue(eventId);

        if (!queue.contains(sessionId)) {
            queue.offer(sessionId); // enter to end of the queue
            System.out.println("User with Session ID " + sessionId + " added to waiting list for Event " + eventId);
        }
    }

// dequeue of a defined num of users from the waiting queue for a specific event.
    @Override
    public List<String> dequeueBatch(long eventId, long batchSize) {
        Queue<String> queue = getOrCreateQueue(eventId);
        List<String> approvedUsers = new ArrayList<>();

        for (long i = 0; i < batchSize; i++) {
            String sessionId = queue.poll();
            if (sessionId != null) {
                approvedUsers.add(sessionId);
            } else {
                break; // queue is empty
            }
        }

        return approvedUsers;
    }

    @Override
    public int getQueueSize(long eventId) {
        return getOrCreateQueue(eventId).size();
    }

    @Override
    public void removeUserFromQueue(long eventId, String sessionId) {
        Queue<String> queue = getOrCreateQueue(eventId);
        queue.remove(sessionId);
    }
}
