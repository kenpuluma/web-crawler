import com.sleepycat.je.DatabaseConfig;
import com.sleepycat.je.EnvironmentConfig;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.List;

/**
 * Created by Lanslot on 2017/4/8.
 */
public class Frontier {

    private WorkQueueDB workDB;

    /**
     * A database to store all the hash for visited pages
     * Use BerkeleyDB to improve performance
     */
    private UrlDB urlDB;

    /**
     * Page number limit for crawling
     */
    private int maxPages;


    private final Object mutex = new Object();


    /**
     * Default constructor
     * Set pageLimit to false if there is no limit on page number
     *
     * @param config The config of crawler defined by user
     */
    public Frontier(CrawlerConfig config) {
        this.maxPages = config.getMaxPages();

        // setup the hash database
        EnvironmentConfig envConfig = new EnvironmentConfig();
        envConfig.setAllowCreate(true);
        envConfig.setTransactional(config.isResumable());

        DatabaseConfig dbConfig = new DatabaseConfig();
        dbConfig.setAllowCreate(true);
        dbConfig.setTransactional(config.isResumable());

        this.urlDB = new UrlDB(envConfig, dbConfig, config.getWorkPath());
        this.workDB = new WorkQueueDB(envConfig, dbConfig, config.getWorkPath());
    }


    /**
     * Init workQueue from seed urls at the beginning
     *
     * @param config The config of crawler defined by user
     */
    public void setWorkQueue(CrawlerConfig config) {
        for (WebURL url : config.getSeedURL()) {
            if (config.shouldVisit(url)) {
                if (urlDB.getID(url.getUrl()) == -1) {
                    workDB.put(url);
                    urlDB.put(url.getUrl());
                }
            }
        }   // end of loop
    }

    /**
     * Schedule all the outgoing links to the workQueue
     * Links will be tested against the shouldVisit rule first
     *
     * @param links  Extracted links from jsoup class
     * @param depth  Depth for these links
     * @param config The config of crawler defined by user
     */
    public void scheduleWork(Elements links, short depth, CrawlerConfig config) {
        synchronized (mutex) {

            // scan all the links
            for (Element link : links) {
                WebURL url = new WebURL(link.attr("abs:href"), depth);

                // test against shouldVisit rule
                if (config.shouldVisit(url)) {

                    // test if page has met before
                    if (urlDB.getID(url.getUrl()) == -1) {
                        workDB.put(url);
                        urlDB.put(url.getUrl());
                    }

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
            if (maxPages < 0 || (workDB.getPageNumber() + size < maxPages)) {
                crawlerQueue.addAll(workDB.getWebURLs(size));
                workDB.delete(size);
            } else if (workDB.getPageNumber() < maxPages) {
                crawlerQueue.addAll(workDB.getWebURLs(maxPages - workDB.getPageNumber()));
                workDB.delete(maxPages - workDB.getPageNumber());
            }
        }
    }

    // /**
    //  * Return the hashcode for a url
    //  *
    //  * @param url Url for website in String
    //  * @return The hashcode for the url
    //  * @throws NoSuchAlgorithmException On wrong hash method
    //  */
    // public String getHash(String url) throws NoSuchAlgorithmException {
    //     // use md5 for now
    //     MessageDigest md = MessageDigest.getInstance("md5");
    //     md.update(url.getBytes());
    //
    //     // convert byte stream to String
    //     return new BigInteger(1, md.digest()).toString(16);
    // }

    /**
     * Return the hashcode for a url
     *
     * @param url Url for website in String
     * @return The hashcode for the url
     */
    public int getHash(String url) {
        int urlID = -1;
        urlID = urlDB.getID(url);

        return urlID;
    }

    /**
     * Action before shutdown
     */
    public void shutdown() {
        urlDB.closeDB();
    }

}
