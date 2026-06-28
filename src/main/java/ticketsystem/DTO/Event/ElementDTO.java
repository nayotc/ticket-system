package ticketsystem.DTO.Event;

import ticketsystem.DomainLayer.event.Element;

public record ElementDTO(
        Long id,
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
                typeof(element)
        );
    }

    public static String typeof(Element element) {
        if (element == null) {
            return "GENERIC";
        }

        if (element instanceof ticketsystem.DomainLayer.event.SeatingArea) {
            return "SEATING_AREA";
        } else if (element instanceof ticketsystem.DomainLayer.event.StandingArea) {
            return "STANDING_AREA";
        }

        String name = element.getName();
        if (name == null) {
            return "GENERIC";
        }

        String normalized = name.strip().toUpperCase();

        if (normalized.contains("STAGE") || normalized.contains("במה")) {
            return "STAGE";
        } else if (normalized.contains("ENTRANCE") || normalized.contains("כניסה")) {
            return "ENTRANCE";
        } else if (normalized.contains("EXIT") || normalized.contains("יציאה")) {
            return "EXIT";
        } else if (normalized.contains("BAR") || normalized.contains("דוכן") || normalized.contains("דוכנים") || normalized.contains("בר") || normalized.contains("בר משקאות") || normalized.contains("משקאות")) {
            return "BAR";
        } else if (normalized.contains("medical") || normalized.contains("medical station") || normalized.contains("medical area") || normalized.contains("עזרה ראשונה") || normalized.contains("אזור רפואי")) {
            return "FIRSTAID";
        } else {
            return "GENERIC";
        }
    }

}
