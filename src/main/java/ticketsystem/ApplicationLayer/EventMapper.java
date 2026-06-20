package ticketsystem.ApplicationLayer;

import java.util.Locale;
import java.util.function.Supplier;

import ticketsystem.DTO.Event.ElementDTO;
import ticketsystem.DTO.Event.EventDTO;
import ticketsystem.DTO.Event.EventMapDTO;
import ticketsystem.DTO.Event.IMapElementDTO;
import ticketsystem.DTO.Event.PairDTO;
import ticketsystem.DTO.Event.SeatDTO;
import ticketsystem.DTO.Event.SeatPositionDTO;
import ticketsystem.DTO.Event.SeatingAreaDTO;
import ticketsystem.DTO.Event.StandingAreaDTO;
import ticketsystem.DomainLayer.event.*;

public class EventMapper {
    private EventMapper() {
        // Private constructor to prevent instantiation
    }

    public static Event toEvent(EventDTO dto, Supplier<EventMap> eventMapSupplier) {
        if (dto == null) {
            return null;
        }

        Event event = new Event(
                dto.date(),
                dto.name(),
                dto.companyId(),
                dto.openedBy(),
                dto.location() != null ? toEventLocation(dto.location()) : null,
                dto.trafficThreshold(),
                dto.category() != null ? toEventCategory(dto.category()) : null,
                dto.artistName(),
                dto.ticketPrice(),
                dto.mapSize() != null ? toDomain(dto.mapSize()) : null
        );

        if (eventMapSupplier != null) {
            EventMap suppliedMap = eventMapSupplier.get();
            if (suppliedMap != null) {
                event.setMap(suppliedMap);
            }
        }

        return event;
    }

    public static EventMap toDomain(EventMapDTO dto) {
        if (dto == null) {
            return null;
        }

        EventMap eventMap = new EventMap(toDomain(dto.size()));

        for (IMapElementDTO mapElement : dto.getElementDTOs()) {
            eventMap.addElement(toDomain(mapElement));
        }

        return eventMap;
    }

    public static Element toDomain(IMapElementDTO  dto) {
        if (dto == null) {
            return null;
        }

        if (dto instanceof SeatingAreaDTO seatingAreaDTO) {
            return toDomain(seatingAreaDTO);
        }

        if (dto instanceof StandingAreaDTO standingAreaDTO) {
            return toDomain(standingAreaDTO);
        }

        if (dto instanceof ElementDTO elementDTO) {
            return toDomain(elementDTO);
        }
        throw new IllegalArgumentException("Unknown element type: " + dto.getClass().getName());
    }

    public static Element toDomain(ElementDTO dto) {
        if (dto == null) {
            return null;
        }

        return new Element(
                dto.name(),
                toDomain(dto.location()),
                toDomain(dto.size())
        );
    }

    public static Pair<Integer, Integer> toDomain(PairDTO<Integer, Integer> dto) {
        if (dto == null) {
            return null;
        }

        return new Pair<>(dto.first(), dto.second());
    }

    public static SeatingArea toDomain(SeatingAreaDTO dto) {
        if (dto == null) {
            return null;
        }

        SeatingArea seatingArea = new SeatingArea(
                dto.name(),
                toDomain(dto.location()),
                toDomain(dto.size()),
                dto.rows(),
                dto.columns()
        );

        for (SeatDTO seatDTO : dto.seats()) {
            SeatPosition position = toDomain(seatDTO.position());
            Seat seat = seatingArea.getSeats().get(position);

            if (seat == null) {
                throw new IllegalArgumentException("Seat position does not exist in seating area");
            }

            seat.setStatus(toSeatStatus(seatDTO.status()));
        }

        return seatingArea;
    }

    public static StandingArea toDomain(StandingAreaDTO dto) {
        if (dto == null) {
            return null;
        }

        if (dto.reserved() + dto.sold() > dto.capacity()) {
            throw new IllegalArgumentException("Reserved and sold spots cannot exceed capacity");
        }

        StandingArea standingArea = new StandingArea(
                dto.name(),
                toDomain(dto.location()),
                toDomain(dto.size()),
                dto.capacity()                
        );
        standingArea.setReserved(dto.reserved());
        standingArea.setSold(dto.sold());
        return standingArea;
    }

    public static SeatPosition toDomain(SeatPositionDTO dto) {
        if (dto == null) {
            return null;
        }

        return new SeatPosition(dto.row(), dto.number());
    }

    public static EventCategory toEventCategory(String category) {
        if (category == null) {
            return null;
        }

        return EventCategory.valueOf(category.trim().toUpperCase(Locale.ROOT));
    }

    public static EventLocation toEventLocation(String location) {
        if (location == null) {
            return null;
        }

        return EventLocation.valueOf(location.trim().toUpperCase(Locale.ROOT));
    }

    public static Seat.SeatStatus toSeatStatus(String status) {
        return Seat.SeatStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }

    public static SaleStatus toSaleStatus(String status) {
        return SaleStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
    }

}
