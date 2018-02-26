package mingzuozhibi.persist;

import javax.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

@MappedSuperclass
public abstract class BaseModel implements Serializable {

    private static final DateTimeFormatter formatterDate = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private Long id;
    private Long version;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    @Version
    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    protected Long toEpochMilli(LocalDateTime dateTime) {
        return Optional.ofNullable(dateTime)
                .map(date -> date.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli())
                .orElse(0L);
    }

    protected String formatDate(LocalDate localDate) {
        return localDate.format(formatterDate);
    }
}
