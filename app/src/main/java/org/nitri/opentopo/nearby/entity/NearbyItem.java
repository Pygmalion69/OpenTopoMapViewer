package org.nitri.opentopo.nearby.entity;

import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;
import androidx.annotation.NonNull;

@Entity(tableName = "Nearby", indices={@Index(value = "pageid", unique = true)})
public class NearbyItem implements Comparable<NearbyItem> {

    @PrimaryKey(autoGenerate = true)
    private int id;

    private String pageid;

    private int index;

    private String title;

    private String description;

    private String thumbnail;

    private int width;

    private int height;

    private double lat;

    private double lon;

    private String url;

    @Ignore
    private int distance;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getPageid() {
        return pageid;
    }

    public void setPageid(String pageid) {
        this.pageid = pageid;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(String thumbnail) {
        this.thumbnail = thumbnail;
    }

    public int getWidth() {
        return width;
    }

    public void setWidth(int width) {
        this.width = width;
    }

    public int getHeight() {
        return height;
    }

    public void setHeight(int height) {
        this.height = height;
    }

    public double getLat() {
        return lat;
    }

    public void setLat(double lat) {
        this.lat = lat;
    }

    public double getLon() {
        return lon;
    }

    public void setLon(double lon) {
        this.lon = lon;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getDistance() {
        return distance;
    }

    public void setDistance(int distance) {
        this.distance = distance;
    }

    @Override
    public int compareTo(@NonNull NearbyItem other) {
        return Integer.compare(distance, other.getDistance());
    }
}
