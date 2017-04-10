import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/3/26.
 */
public class CrawlerConfig {
    private int maxDepth = -1;
    private int maxPages = -1;
    private int visitDelay = 1000;
    private int socketTimeout = 20000;
    private int connectionTimeout = 30000;
    private int numberOfCrawler = 1;
    private List<WebURL> seedURL = new ArrayList<>();
    private String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.19 (KHTML, like Gecko) Chrome/1.0.154.53 Safari/525.19";
    private String proxyHost;
    private int proxyPort;

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getVisitDelay() {
        return visitDelay;
    }

    public int getSocketTimeout() {
        return socketTimeout;
    }

    public int getConnectionTimeout() {
        return connectionTimeout;
    }

    public int getNumberOfCrawler() {
        return numberOfCrawler;
    }

    public List<WebURL> getSeedURL() {
        return seedURL;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public String getProxyHost() {
        return proxyHost;
    }

    public int getProxyPort() {
        return proxyPort;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public void setVisitDelay(int visitDelay) {
        this.visitDelay = visitDelay;
    }

    public void setSocketTimeout(int socketTimeout) {
        this.socketTimeout = socketTimeout;
    }

    public void setConnectionTimeout(int connectionTimeout) {
        this.connectionTimeout = connectionTimeout;
    }

    public void setNumberOfCrawler(int numberOfCrawler) {
        this.numberOfCrawler = numberOfCrawler;
    }

    public void addSeedURL(String seedURL) {
        WebURL url = new WebURL(seedURL, (short) 0);
        this.seedURL.add(url);
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }

    public void setProxyHost(String proxyHost) {
        this.proxyHost = proxyHost;
    }

    public void setProxyPort(int proxyPort) {
        this.proxyPort = proxyPort;
    }

    public boolean shouldVisit(WebURL url) {
        return true;
    }
}
