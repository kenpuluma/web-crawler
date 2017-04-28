import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/3/25.
 */


public class WebCrawler implements Runnable {


    private CrawlerConfig config;
    private Frontier frontier;
    private HttpResponseClient responseClient;

    /**
     * Whether the thread is waiting for save results
     */
    private boolean waitingForSave;

    /**
     * Whether the thread is waiting for frontier to assign url
     */
    private boolean waitingForURL;

    private boolean waiting;

    /**
     * Store a list of parsed pages
     */
    private List<WebPage> resultPages;

    /**
     * Default constructor
     * Initial all the local instances
     *
     * @param config         The config of crawler defined by user
     * @param frontier       The frontier created by the monitor
     * @param responseClient The precooked http client
     */
    public WebCrawler(CrawlerConfig config, Frontier frontier, HttpResponseClient responseClient) {
        resultPages = new ArrayList<>();
        this.config = config;
        this.frontier = frontier;
        this.responseClient = responseClient;
        this.waitingForURL = false;
        this.waitingForSave = false;
        this.waiting = false;
    }

    public boolean isWaiting() {
        return this.waiting;
    }

    public boolean isWaitingForURL() {
        return waitingForURL;
    }

    public List<WebPage> getResultPages() {
        return resultPages;
    }

    public void setWaitingForSave(boolean waitingForSave) {
        this.waitingForSave = waitingForSave;
    }

    public void clearResults() {
        this.resultPages.clear();
    }

    @Override
    public void run() {
        while (true) {

            // create a sub work list to improve performance
            List<WebURL> workQueue = new ArrayList<>(config.getSizeOfWorkQueue());

            // get work list from frontier
            this.waitingForURL = true;
            frontier.getNextURL(config.getSizeOfWorkQueue(), workQueue);
            this.waitingForURL = false;

            // visit each url
            for (WebURL curURL : workQueue) {
                // wait until next round
                if (waitingForSave || workQueue.isEmpty()) {
                    this.waiting = true;
                    try {
                        Thread.sleep(3000);
                    } catch (InterruptedException e) {
                        System.out.println("Worker thread failed to sleep!");
                    }
                } else {
                    this.waiting = false;
                    if (curURL != null) {
                        visit(curURL);
                    }
                }
            }   // end of for loop
        }   // end of while loop
    }

    /**
     * Process the content of page
     *
     * @param url Link should be visited
     */
    public void visit(WebURL url) {

        // get html content
        String html = responseClient.getResponse(url, config.getVisitDelay());

        if (html != null) {
            // get outgoing links
            Elements links = parse(html, url.getUrl());

            // schedule links if not exceed the depth
            if (config.getMaxDepth() < 0 || url.getDepth() + 1 < config.getMaxDepth())
                frontier.scheduleWork(links, (short) (url.getDepth() + 1), config);

            System.out.println("Visited: " + url.getUrl());
        }

    }

    /**
     * Remove all the html tags and undesired contents from html text
     *
     * @param html Html text from response
     * @param url  Url of the page
     * @return Outgoing links
     */
    public Elements parse(String html, String url) {

        WebPage page = new WebPage();

        // use Jsoup library to parse text
        Document document = Jsoup.parse(html, "UTF-8");
        Elements tags = document.select("*");

        // get plain text
        StringBuilder stringBuilder = new StringBuilder();

        // find characters not valid in utf-8 meanwhile
        char illegal = 65533;
        for (Element tag : tags) {
            for (TextNode tn : tag.textNodes()) {
                String tagText = tn.text().trim();
                if (tagText.contains(String.valueOf(illegal)))
                    return document.select("a[href]");

                if (tagText.length() > 0) {
                    stringBuilder.append(tagText).append(' ');
                }
            }
        }
        String plaintext = stringBuilder.toString();

        // remove all the line separator or quotation mark
        plaintext = plaintext.replaceAll("\\s+|\"+", " ");
        // get the description from the text
        String description = findDescription(plaintext);
        // get the hashcode from the url
        int hashCode = frontier.getHash(url);

        // store all the results
        page.setUrl(url);
        page.setTitle(document.title());
        page.setDescription(description);
        page.setText(plaintext);
        page.setHash(hashCode);

        // add it to the result list
        // TODO: 2017/4/9 fix missing or garbled text
        if (page.getTitle().length() > 0 && page.getText().length() > 0)
            resultPages.add(page);

        return document.select("a[href]");
    }

    /**
     * Find suitable description text for the page
     *
     * @return A String contains description
     */
    public String findDescription(String text) {

        // return whole page if it's too short
        if (text.length() < config.getDescriptionLength())
            return text;

        String[] strings = text.split(" ");
        String result = "";
        StringBuilder substr = new StringBuilder();
        int maxLength = 0;

        // find the first several sentences in longest paragraph
        // or add up the first several paragraphs when no one is long enough
        for (String string : strings) {
            if (string.length() > maxLength) {
                result = string;
                maxLength = result.length();
            }
            if (substr.length() < config.getDescriptionLength() && string.length() > 4)
                substr.append(string).append(' ');
        }

        // add up first several sentences in longest paragraph
        if (result.length() > config.getDescriptionLength()) {
            String[] strings1 = result.split(" ");
            substr = new StringBuilder();
            for (String string : strings1) {
                if (substr.length() < config.getDescriptionLength())
                    substr.append(string).append(' ');
            }
        }

        return substr.toString();
    }
}
