package mingzuozhibi.persist.model;

import mingzuozhibi.persist.BaseModel;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table(name = "season")
public class Season extends BaseModel {

    private String japan;
    private String title;

    @Column(length = 30, nullable = false, unique = true)
    public String getJapan() {
        return japan;
    }

    public void setJapan(String japan) {
        this.japan = japan;
    }

    @Column(length = 30, nullable = false)
    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

}
