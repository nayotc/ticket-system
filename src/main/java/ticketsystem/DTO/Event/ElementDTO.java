package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.Element;

public record ElementDTO(
        long id,
        String name,
        PairDTO<Integer, Integer> location,
        PairDTO<Integer, Integer> size,
        String type
) implements IMapElementDTO {

    public static ElementDTO from(Element element) {
        if (element == null) {
            return null;
        }

        return new ElementDTO(
                element.getId(),
                element.getName(),
                PairDTO.from(element.getLocation()),
                PairDTO.from(element.getSize()),
                element.getClass().getSimpleName()
        );
    }
}
