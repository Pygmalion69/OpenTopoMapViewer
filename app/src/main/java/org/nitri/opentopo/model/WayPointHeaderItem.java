package org.nitri.opentopo.model;

public class WayPointHeaderItem  implements WayPointListItem {

    private String header;

    public WayPointHeaderItem(String header) {
        this.header = header;
    }

    public String getHeader() {
        return header;
    }

    public void setHeader(String header) {
        this.header = header;
    }

    @Override
    public int getListItemType() {
        return HEADER;
    }
}
