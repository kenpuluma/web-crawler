import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/3/26.
 */
public class CrawlerConfig {

    /**
     * Maximum depth for crawling
     * Set -1 for unlimited
     */
    private int maxDepth = -1;

    /**
     * Maximum pages for crawling
     * Set -1 for unlimited
     */
    private int maxPages = -1;

    /**
     * Delay between two requests for single worker thread, default is 1 millisecond
     */
    private int visitDelay = 1000;

    /**
     * Delay between two scans for monitor thread, default is 10 seconds
     */
    private int threadMonitorDelay = 10000;

    /**
     * Number of crawler threads, default is 1
     */
    private int numberOfCrawler = 1;

    /**
     * Seed urls before crawling
     */
    private List<WebURL> seedURL = new ArrayList<>();

    /**
     * Fake as a browser to web site, default is Chrome
     */
    private String userAgent = "Mozilla/5.0 (Windows; U; Windows NT 5.1; en-US) AppleWebKit/525.19 (KHTML, like Gecko) Chrome/1.0.154.53 Safari/525.19";

    /**
     * Proxy host to hide ip
     * Set null to disable proxy
     */
    private String proxyHost;

    /**
     * Port number for proxy runs on
     */
    private int proxyPort;


    private int socketTimeout = 20000;
    private int connectionTimeout = 30000;

    public int getMaxDepth() {
        return maxDepth;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public int getVisitDelay() {
        return visitDelay;
    }

    public int getThreadMonitorDelay() {
        return threadMonitorDelay;
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

    public void setThreadMonitorDelay(int threadMonitorDelay) {
        this.threadMonitorDelay = threadMonitorDelay;
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

    /**
     * Add a seed url at the beginning
     *
     * @param seedURL The web url in string
     */
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

    /**
     * Should the crawler visit the page? Always true by default
     *
     * @param url A WebURL object
     * @return True if the page should be visited
     */
    public boolean shouldVisit(WebURL url) {
        // TODO: 2017/4/9 robots rule
        return true;
    }
}
