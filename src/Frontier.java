import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

/**
 * Created by Lanslot on 2017/4/8.
 */
public class Frontier {
    // TODO: 2017/4/9 NoSQL database

    /**
     * A list to store all the web pages
     * need to be visited in the future
     */
    private List<WebURL> workQueue;

    /**
     * A list to store all the web pages that
     * are processing right now by worker threads
     */
    private List<WebURL> inProgressQueue;

    /**
     * A list to store all the hash for visited pages
     * Use HashSet to improve performance
     */
    private HashSet<String> hashSet;

    /**
     * Whether the crawling is limited by number of pages
     */
    private boolean pageLimit;

    /**
     * Page number limit for crawling
     */
    private int maxPages;


    private final Object mutex = new Object();


    /**
     * Default constructor
     * Set pageLimit to false if there is no limit on page number
     *
     * @param maxPages Maximum number of pages for crawling
     */
    public Frontier(int maxPages) {
        this.maxPages = maxPages;
        if (maxPages < 0) {
            this.pageLimit = false;
        } else this.pageLimit = true;
        this.workQueue = new ArrayList<>();
        this.inProgressQueue = new ArrayList<>();
        this.hashSet = new HashSet<>();
    }

    public List<WebURL> getWorkQueue() {
        return workQueue;
    }

    public List<WebURL> getInProgressQueue() {
        return inProgressQueue;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setWorkQueue(List<WebURL> workQueue) {
        this.workQueue.addAll(workQueue);
    }

    /**
     * Schedule all the outgoing links to the workQueue
     * Links will be tested against the shouldVisit rule first
     *
     * @param links  Extracted links from jsoup class
     * @param depth  Depth for these links
     * @param config The config of crawler defined by user
     * @throws NoSuchAlgorithmException on getHash failed
     */
    public void scheduleWork(Elements links, short depth, CrawlerConfig config) throws NoSuchAlgorithmException {
        synchronized (mutex) {

            // scan all the links
            for (Element link : links) {
                WebURL url = new WebURL(link.attr("abs:href"), depth);

                // test against shouldVisit rule
                if (config.shouldVisit(url)) {
                    // test if page has met before
                    // TODO: 2017/4/9 save hash to disk
                    if (hashSet.add(getHash(url.getUrl())))
                        workQueue.add(url);
                }
            } // end of scan
        }
    }

    /**
     * Return a list of links for worker thread to process next
     *
     * @param size         How many pages should be retrieved for one time
     * @param crawlerQueue Sub workQueue for single thread
     */
    public void getNextURL(int size, List<WebURL> crawlerQueue) {
        synchronized (mutex) {
            // return if there is no jobs in queue
            if (workQueue.size() == 0) {
                return;
            }

            // check whether there is a limit on number of pages
            if (pageLimit) {
                if (maxPages < 0)
                    return;
                maxPages--;
            }

            // assign job to worker thread
            // remove it from main workQueue and add it to inProgressQueue
            // TODO: 2017/4/9 multiple retrieval
            crawlerQueue.add(workQueue.get(0));
            inProgressQueue.addAll(crawlerQueue);
            workQueue.removeAll(crawlerQueue);
        }
    }

    /**
     * Remove the page in inProgressQueue if it has been processed
     *
     * @param url Visited page
     */
    public void setProgressed(WebURL url) {
        inProgressQueue.remove(url);
    }

    /**
     * Return the hashcode for a url
     *
     * @param url Url for website in String
     * @return The hashcode for the url
     * @throws NoSuchAlgorithmException On wrong hash method
     */
    public String getHash(String url) throws NoSuchAlgorithmException {
        // use md5 for now
        MessageDigest md = MessageDigest.getInstance("md5");
        md.update(url.getBytes());

        // convert byte stream to String
        return new BigInteger(1, md.digest()).toString(16);
    }


}
