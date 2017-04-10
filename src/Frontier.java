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
    private List<WebURL> workQueue;
    private List<WebURL> inProgressQueue;
    private HashSet<String> hashSet;
    private final Object mutex = new Object();
    private int maxPages;
    private boolean pageLimit;

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

    public void scheduleWork(Elements links, short depth, CrawlerConfig config) throws NoSuchAlgorithmException {
        synchronized (mutex) {
            for (Element link : links) {
                if (pageLimit) {
                    if (maxPages < 0)
                        break;
                    maxPages--;
                }
                WebURL url = new WebURL(link.attr("abs:href"), depth);
                // TODO: 2017/4/9 robots rule
                if (config.shouldVisit(url)) {
                    // TODO: 2017/4/9 save hash to disk
                    if (hashSet.add(getHash(url.getUrl())))
                        workQueue.add(url);
                }
            }
        }

    }

    public void getNextURL(int size, List<WebURL> crawlerQueue) {
        synchronized (mutex) {
            if (workQueue.size() == 0) {
                return;
            }
            // TODO: 2017/4/9 multiple retrieval
            // crawlerQueue.addAll(workQueue.subList(0, size));
            crawlerQueue.add(workQueue.get(0));
            inProgressQueue.addAll(crawlerQueue);
            workQueue.removeAll(crawlerQueue);
        }
    }

    public void setProgressed(WebURL url) {
        inProgressQueue.remove(url);
    }

    public String getHash(String url) throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("md5");
        md.update(url.getBytes());
        return new BigInteger(1, md.digest()).toString(16);
    }


}
