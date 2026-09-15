package com.eaut.canteen.model;

public class Banner {

    private int bannerId;
    private String position;
    private String title;
    private String subtitle;
    private String linkUrl;
    /** Whether this banner has media set — the bytes themselves are only fetched by BannerImageServlet, never loaded into listing queries. */
    private boolean hasImage;
    /**
     * MIME type of the uploaded media: an image type, or a video type since banners accept short
     * clips too. Backed by the column still named image_content_type — renaming it would mean an
     * ALTER on the live database with the old code still serving traffic, which is not worth the
     * cosmetic gain.
     */
    private String mediaContentType;
    private int sortOrder;
    private boolean active;

    public int getBannerId() {
        return bannerId;
    }

    public void setBannerId(int bannerId) {
        this.bannerId = bannerId;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSubtitle() {
        return subtitle;
    }

    public void setSubtitle(String subtitle) {
        this.subtitle = subtitle;
    }

    public String getLinkUrl() {
        return linkUrl;
    }

    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }

    public boolean isHasImage() {
        return hasImage;
    }

    public void setHasImage(boolean hasImage) {
        this.hasImage = hasImage;
    }

    public String getMediaContentType() {
        return mediaContentType;
    }

    public void setMediaContentType(String mediaContentType) {
        this.mediaContentType = mediaContentType;
    }

    /** Lets the view pick between a &lt;video&gt; and an &lt;img&gt; without parsing MIME types in EL. */
    public boolean isVideo() {
        return mediaContentType != null && mediaContentType.startsWith("video/");
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
