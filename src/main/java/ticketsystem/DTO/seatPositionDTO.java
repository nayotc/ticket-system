package ticketsystem.DTO;

public class seatPositionDTO {
    private int row;
    private int chair;

    public seatPositionDTO() {}

    public seatPositionDTO(int row, int chair) {
        this.row = row;
        this.chair = chair;
    }

    public int getRow() { return row; }
    public void setRow(int row) { this.row = row; }

    public int getChair() { return chair; }
    public void setChair(int chair) { this.chair = chair; }

    @Override
    public String toString() {
        return "seatPositionDTO{" +
                "row=" + row +
                ", chair=" + chair +
                '}';
    }
    
}
