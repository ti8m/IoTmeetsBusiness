package ch.ti8m.iotmeetsbusiness.persistency;

import java.util.List;

/**
 * Created by sa005 on 15.04.2016.
 */
public class DataChannel {

    private String id;
    private String writeKey;
    private String readKey;
    private String description;
    private String location;
    private String position;
    private List tags;
    private String value1;
    private String value2;

    public DataChannel() {
    }

    public DataChannel(String id, String writeKey, String readKey, String description, String location, String position, List tags, String value1, String value2) {
        this.id = id;
        this.writeKey = writeKey;
        this.readKey = readKey;
        this.description = description;
        this.location = location;
        this.position = position;
        this.tags = tags;
        this.value1 = value1;
        this.value2 = value2;
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

    public String getPosition() { return position; }

    public void setLocation(String location) { this.location = location; }

    public void setPosition(String position) { this.position = position; }

    public List getTags() {
        return tags;
    }

    public void setTags(List tags) {
        this.tags = tags;
    }

    public String getValue1() {
        return value1;
    }

    public void setValue1(String value1) {
        this.value1 = value1;
    }

    public String getValue2() {
        return value2;
    }

    public void setValue2(String value2) { this.value2 = value2; }

}
