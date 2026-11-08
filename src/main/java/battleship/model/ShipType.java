package battleship.model;

public enum ShipType {
    CARRIER("Tàu sân bay", 5),
    BATTLESHIP("Thiết giáp hạm", 4),
    CRUISER("Tuần dương hạm", 3),
    SUBMARINE("Tàu ngầm", 3),
    DESTROYER("Khu trục hạm", 2);

    public final String displayName;
    public final int length;

    ShipType(String displayName, int length) {
        this.displayName = displayName;
        this.length = length;
    }
}
