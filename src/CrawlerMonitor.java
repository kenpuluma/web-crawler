import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.nio.charset.Charset;
import java.nio.charset.CharsetEncoder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/4/9.
 */
public class CrawlerMonitor {

    private Frontier frontier;
    private HttpResponseClient responseClient;
    private CrawlerConfig config;
    private Object mutex = new Object();
    private List<WebPage> resultPages;

    /**
     * Default constructor
     * Store the config in memory and initialize the frontier
     *
     * @param config The config of crawler defined by user
     */
    public CrawlerMonitor(CrawlerConfig config) {
        this.config = config;
        this.frontier = new Frontier(this.config.getMaxPages());
        this.responseClient = new HttpResponseClient(this.config);
        this.resultPages = new ArrayList<>();
    }

    /**
     * Start all the crawler threads and the monitor threads
     */
    public void start() {

        // Fill frontier with seed urls
        frontier.setWorkQueue(config.getSeedURL());

        List<Thread> threads = new ArrayList<>();
        List<WebCrawler> crawlers = new ArrayList<>();

        // start all the worker threads
        for (int i = 0; i < config.getNumberOfCrawler(); i++) {
            WebCrawler crawler = new WebCrawler(config, frontier, responseClient);
            Thread thread = new Thread(crawler, "Crawler " + i);
            thread.start();
            crawlers.add(crawler);
            threads.add(thread);
            System.out.printf("Crawler %d started\n", i);
        }

        // create a monitor thread
        // check all the worker threads periodically
        Thread monitor = new Thread(() -> {
            try {
                synchronized (mutex) {
                    while (true) {
                        Thread.sleep(config.getThreadMonitorDelay());
                        boolean isWorking = false;
                        int resultSize = 0;

                        // scan all worker threads
                        for (int i = 0; i < threads.size(); i++) {

                            // check result size
                            resultSize += crawlers.get(i).getResultPages().size();

                            // thread is dead
                            if (!threads.get(i).isAlive()) {
                                System.out.printf("Crawler %d dead\n", i);
                                WebCrawler crawler = new WebCrawler(config, frontier, responseClient);
                                Thread thread = new Thread(crawler, "Crawler " + i);
                                thread.start();
                                threads.remove(i);
                                threads.add(i, thread);
                                crawlers.remove(i);
                                crawlers.add(i, crawler);
                                System.out.printf("Crawler %d started\n", i);
                            } else if (!crawlers.get(i).isWaiting()) {    // thread is working
                                isWorking = true;
                            }
                        }

                        if (resultSize > 100) {
                            for (WebCrawler crawler : crawlers) {
                                crawler.setWaitingForSave(true);
                                resultPages.addAll(crawler.getResultPages());
                            }
                            saveToDisk(this.resultPages, this.config.getFilePath());
                        }


                        // wait for a period of time to make sure
                        if (!isWorking) {
                            System.out.println("No one is working");
                            Thread.sleep(config.getThreadMonitorDelay());
                            for (int i = 0; i < threads.size(); i++) {
                                if (threads.get(i).isAlive() && !crawlers.get(i).isWaiting()) {
                                    isWorking = true;
                                }
                            }

                            // shutdown the program
                            if (!isWorking) {
                                System.out.println("Shutting down");
                                shutdown();
                                return;
                            }
                        }
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        monitor.start();
    }

    /**
     * Action before shutdown
     */
    public void shutdown() {
        saveOnExit();
    }

    // TODO: 2017/4/9 resume from exit
    public void saveOnExit() {

    }

    /**
     * Save JSON results to disk
     */
    public synchronized void saveToDisk(List<WebPage> pages, String filePath) {
        try {

            // use utf-8 as encoder
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filePath, true), encoder);

            // get a JSON object from the result list
            JSONObject main = parseToJSON(pages);

            // remove all unicode characters
            String string = main.toString();
            string = StringEscapeUtils.unescapeJava(string);

            // write to disk
            out.write(string + '\n');
            out.close();

        } catch (IOException e) {
            e.printStackTrace();
        }
        System.out.println("Results saved");
    }

    /**
     * Parse results into JSON object
     *
     * @return JSON result
     */
    public JSONObject parseToJSON(List<WebPage> pages) {
        JSONObject main = new JSONObject();
        JSONArray page = new JSONArray();

        // put all the results in page array
        while (!pages.isEmpty()) {
            WebPage webPage = pages.remove(0);
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
