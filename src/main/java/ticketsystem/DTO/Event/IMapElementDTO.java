package ticketsystem.DTO.Event;

public interface IMapElementDTO {

    Long id();

    String name();

    PairDTO<Integer, Integer> location();

    PairDTO<Integer, Integer> size();
}