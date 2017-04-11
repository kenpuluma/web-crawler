import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/3/25.
 */


public class WebCrawler implements Runnable {


    private CrawlerConfig config;
    private Frontier frontier;
    private HttpResponseClient responseClient;
    private final Object mutex = new Object();

    /**
     * Whether the thread is waiting for frontier to assign url
     */
    private boolean waitingForURL;

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
    }

    public boolean isWaitingForURL() {
        return waitingForURL;
    }

    @Override
    public void run() {
        while (true) {

            // create a sub work list to improve performance
            List<WebURL> workQueue = new ArrayList<>(50);

            // get work list from frontier
            this.waitingForURL = true;
            frontier.getNextURL(workQueue.size(), workQueue);
            this.waitingForURL = false;

            // wait until next round
            if (workQueue.isEmpty()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {

                // visit each url
                for (WebURL curURL : workQueue) {
                    if (curURL != null) {
                        visit(curURL);
                        frontier.setProgressed(curURL);
                    }
                }
            }

            // // do something when result is large
            // // TODO: 2017/4/9 save to disk maybe
            // if (resultPages.size() > 50) {
            //     saveToDisk();
            // }
        }   // end of while loop
    }

    /**
     * Process the content of page
     *
     * @param url Link should be visited
     */
    public void visit(WebURL url) {
        try {

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
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * Remove all the html tags and undesired contents from html text
     *
     * @param html Html text from response
     * @param url  Url of the page
     * @return Outgoing links
     * @throws NoSuchAlgorithmException on wrong hash method
     */
    public Elements parse(String html, String url) throws NoSuchAlgorithmException {

        WebPage page = new WebPage();

        // use Jsoup library to parse text
        Document document = Jsoup.parse(html, "ISO-8859-15");

        // get plain text
        String plaintext = document.text();
        // remove all the line separator or quotation mark
        plaintext = plaintext.replaceAll("\\s+|\"+", " ");
        // get the description from the text
        String description = findDescription(plaintext);
        // get the hashcode from the url
        String hashCode = frontier.getHash(url);

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
        if (text.length() < 60)
            return text;

        String[] strings = text.split(" ");
        String subStr = "";

        // find the first sentence longer than 60 words
        // or add up the first several sentence when no one is long enough
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

    /**
     * Save JSON results to disk
     */
    public void saveToDisk() {
        synchronized (mutex) {

            try {

                // use utf-8 as encoder
                CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
                OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream("./tmp.dat", true), encoder);

                // get a JSON object from the result list
                JSONObject main = parseToJSON();

                // remove all unicode characters
                String string = main.toString();
                string = StringEscapeUtils.unescapeJava(string);

                // write to disk
                out.write(string + '\n');
                out.close();

                // clear the result list
                resultPages.clear();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Results saved");
    }

    /**
     * Parse results into JSON object
     *
     * @return JSON result
     */
    public JSONObject parseToJSON() {
        JSONObject main = new JSONObject();
        JSONArray page = new JSONArray();

        // put all the results in page array
        for (WebPage webPage : resultPages) {
            JSONObject object = new JSONObject();
            object.put("hash", webPage.getHash());
            object.put("url", webPage.getUrl());
            object.put("title", webPage.getTitle());
            object.put("description", webPage.getDescription());
            object.put("text", webPage.getText());
            page.put(object);
        }

        main.put("pages", page);
        return main;
    }
}
