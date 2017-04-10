/**
 * Created by Lanslot on 2017/3/28.
 */
public class WebPage {
    private String hash;
    private String url;
    private String title;
    private String description;
    private String text;

    public String getHash() {
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

    public void setHash(String hash) {
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

    public String findDescription() {
        if (this.text.length() < 60)
            return this.text;

        String[] strings = this.text.split(" ");
        String subStr = "";
        for (String string : strings) {
            if (string.length() > 60) {
                if (subStr.length() < 60) {
                    subStr += string;
                }
                return string;
            }
        }

        return subStr;
    }
}
