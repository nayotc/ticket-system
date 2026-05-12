package ticketsystem.DTO.Event;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import ticketsystem.DomainLayer.event.Area;
import ticketsystem.DomainLayer.event.Element;
import ticketsystem.DomainLayer.event.EventMap;
import ticketsystem.DomainLayer.event.SeatingArea;
import ticketsystem.DomainLayer.event.StandingArea;

public record EventMapDTO(
        PairDTO<Integer, Integer> size,
        List<IMapElementDTO> elements,
        boolean soldOut) {

    public static EventMapDTO from(EventMap map) {
        if (map == null) {
            return null;
        }

        return new EventMapDTO(
                PairDTO.from(map.getSize()),
                map.getElements()
                        .stream()
                        .map(EventMapDTO::mapElement)
                        .collect(Collectors.toList()),
                map.isSoldOut()
        );
    }

    private static IMapElementDTO mapElement(Element element) {
        if (element instanceof SeatingArea seatingArea) {
            return SeatingAreaDTO.from(seatingArea);
        }

        if (element instanceof StandingArea standingArea) {
            return StandingAreaDTO.from(standingArea);
        }

        return ElementDTO.from(element);
    }

    public List<IMapElementDTO> getElementDTOs() {
        return elements;
    }
    
}


