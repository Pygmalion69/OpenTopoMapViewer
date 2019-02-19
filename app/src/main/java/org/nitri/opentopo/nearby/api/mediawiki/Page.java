package org.nitri.opentopo.nearby.api.mediawiki;

import java.util.List;

public class Page {

    private int pageid;

    private int ns;

    private String title;

    private int index;

    private List<PointCoordinates> coordinates;

    private Thumbnail thumbnail;

    private Terms terms;

    private String fullurl;

    private String canonicalurl;

    public int getPageid() {
        return pageid;
    }

    public void setPageid(int pageid) {
        this.pageid = pageid;
    }

    public int getNs() {
        return ns;
    }

    public void setNs(int ns) {
        this.ns = ns;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public List<PointCoordinates> getCoordinates() {
        return coordinates;
    }

    public void setCoordinates(List<PointCoordinates> coordinates) {
        this.coordinates = coordinates;
    }

    public Thumbnail getThumbnail() {
        return thumbnail;
    }

    public void setThumbnail(Thumbnail thumbnail) {
        this.thumbnail = thumbnail;
    }

    public Terms getTerms() {
        return terms;
    }

    public void setTerms(Terms terms) {
        this.terms = terms;
    }

    public String getFullurl() {
        return fullurl;
    }

    public void setFullurl(String fullurl) {
        this.fullurl = fullurl;
    }

    public String getCanonicalurl() {
        return canonicalurl;
    }

    public void setCanonicalurl(String canonicalurl) {
        this.canonicalurl = canonicalurl;
    }
}
