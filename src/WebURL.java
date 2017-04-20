/**
 * Created by Lanslot on 2017/4/8.
 */
public class WebURL {

    private String url;
    private short depth;

    public WebURL(String url, short depth) {
        this.url = url;
        this.depth = depth;
    }

    public String getUrl() {
        return this.url.replace(" ","%20");
    }

    public short getDepth() {
        return depth;
    }

    public void setUrl(String url) {
        this.url = url;
    }


    public void setDepth(short depth) {
        this.depth = depth;
    }
}
