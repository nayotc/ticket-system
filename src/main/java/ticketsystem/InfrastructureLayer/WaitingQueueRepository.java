package ticketsystem.InfrastructureLayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import ticketsystem.ApplicationLayer.ISystemLogger;
import ticketsystem.DomainLayer.IRepository.IWaitingQueueRepository;

public class WaitingQueueRepository implements IWaitingQueueRepository {

    private final ISystemLogger logger;
    //fifo
    private final Map<Long, Queue<String>> eventQueues; // ConcurrentHashMap with eventid and queue of tokens

    // to track which sessions are currently in the queue for each event, for O(1) lookups and removals
    private final Map<Long, Set<String>> queuedSessionsTracker;

    public WaitingQueueRepository() {
        this.eventQueues = new ConcurrentHashMap<>();
        this.queuedSessionsTracker = new ConcurrentHashMap<>();
        this.logger = new LogbackSystemLogger();
    }

    @Override
    public void enqueueUser(long eventId, String token) {
        logger.logEvent("enqueueUser", "Enqueuing user for event ID: " + eventId + ", token: " + token);
        //in set the search is atomic and thread safe, so we dont need to check the whole queue
        Set<String> sessionTracker = queuedSessionsTracker.computeIfAbsent(eventId, k -> ConcurrentHashMap.newKeySet());
        if (sessionTracker.add(token)) {
            // only how entered the queue can be added to actual queue
            Queue<String> queue = eventQueues.computeIfAbsent(eventId, k -> new ConcurrentLinkedQueue<>());
            queue.offer(token);
        }
    }

// dequeue of a defined num of users from the waiting queue for a specific event.
    @Override
    public List<String> dequeueBatch(long eventId, long batchSize) {
        logger.logEvent("dequeueBatch", "Dequeuing batch of users for event ID: " + eventId + ", batch size: " + batchSize);
        Queue<String> queue = eventQueues.get(eventId);
        Set<String> sessionTracker = queuedSessionsTracker.get(eventId);

        if (queue == null || queue.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> approvedUsers = new ArrayList<>();
        for (int i = 0; i < batchSize; i++) {
            String token = queue.poll();
            if (token != null) {
                approvedUsers.add(token);
                if (sessionTracker != null) {
                    sessionTracker.remove(token);
                }
            } else {
                break;
            }
        }
        if (queue.isEmpty()) { // clean up empty queue to save memory
            eventQueues.remove(eventId);
        }

        return approvedUsers;
    }

    @Override
    public int getQueueSize(long eventId) {
        logger.logEvent("getQueueSize", "Getting queue size for event ID: " + eventId);
        Queue<String> queue = eventQueues.get(eventId);
        return (queue == null) ? 0 : queue.size(); // return queue size or 0 if no queue exists for the event
    }

    @Override
    public void removeUserFromQueue(long eventId, String token) {
        logger.logEvent("removeUserFromQueue", "Removing user from queue for event ID: " + eventId + ", token: " + token);
        Queue<String> queue = eventQueues.get(eventId);
        Set<String> sessionTracker = queuedSessionsTracker.get(eventId);

        if (queue != null && sessionTracker != null) {
            sessionTracker.remove(token);
            queue.remove(token);

            if (queue.isEmpty()) {
                eventQueues.remove(eventId);
                queuedSessionsTracker.remove(eventId);
            }
        }
    }
}
