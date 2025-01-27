package subway.dao;

public class StationEntity {
    private final Long id;
    private final String name;

    public StationEntity(String name) {
        this(null, name);
    }

    public StationEntity(Long id, String name) {
        this.id = id;
        this.name = name;
    }

    public Long getId() {
        return id;
    }

    public String getName() {
        return name;
    }
}
