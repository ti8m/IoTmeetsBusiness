package ch.ti8m.iotmeetsbusiness.persistency;

import java.util.List;

/**
 * Created by sa005 on 15.04.2016.
 */
public class Sensor {

    private String id;
    private String writeKey;
    private String readKey;
    private String description;
    private String location;
    private List tags;

    public Sensor() {
    }

    public Sensor(String id, String writeKey, String readKey, String description, String location, List tags) {
        this.id = id;
        this.writeKey = writeKey;
        this.readKey = readKey;
        this.description = description;
        this.location = location;
        this.tags = tags;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getWriteKey() {
        return writeKey;
    }

    public void setWriteKey(String writeKey) {
        this.writeKey = writeKey;
    }

    public String getReadKey() {
        return readKey;
    }

    public void setReadKey(String readKey) {
        this.readKey = readKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public List getTags() {
        return tags;
    }

    public void setTags(List tags) {
        this.tags = tags;
    }
}
