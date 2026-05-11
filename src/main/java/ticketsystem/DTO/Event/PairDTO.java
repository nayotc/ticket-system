package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.Pair;

public record PairDTO<K, V>(K first, V second) {

    public static <K, V> PairDTO<K, V> from(Pair<K, V> pair) {
        if (pair == null) {
            return null;
        }

        return new PairDTO<>(pair.getFirst(), pair.getSecond());
    }
}
