/**
 * Created by Lanslot on 2017/3/28.
 */
public class WebPage {
    private int hash;
    private String url;
    private String title;
    private String description;
    private String text;

    public int getHash() {
        return hash;
    }

    public String getUrl() {
        return url;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public String getText() {
        return text;
    }

    public void setHash(int hash) {
        this.hash = hash;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setText(String text) {
        this.text = text;
    }

}
