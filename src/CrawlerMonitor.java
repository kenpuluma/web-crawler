import org.apache.commons.lang.StringEscapeUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
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
    private final Object mutex = new Object();
    private List<WebPage> resultPages;

    /**
     * Default constructor
     * Store the config in memory and initialize the frontier
     *
     * @param config The config of crawler defined by user
     */
    public CrawlerMonitor(CrawlerConfig config) {
        this.config = config;

        // delete previous session if not resumable
        if (!this.config.isResumable()) {
            deleteFiles(this.config.getWorkPath());
            deleteFiles(this.config.getFilePath());
            System.out.println("Cleaned folder because crawler is not resumable");
        }

        // create work folders
        createFiles(this.config.getWorkPath(), true);

        // create the result file in offline mode
        if (this.config.isOffline()) {
            createFiles(this.config.getFilePath(), false);
        }

        this.frontier = new Frontier(this.config);
        this.responseClient = new HttpResponseClient(this.config);
        this.resultPages = new ArrayList<>();
    }

    public List<WebPage> getResultPages() {
        return resultPages;
    }

    /**
     * Start all the crawler threads and the monitor threads
     */
    public void start() {

        // Fill frontier with seed urls
        frontier.setWorkQueue(config);

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
                            } else if (!crawlers.get(i).isWaitingForURL()) {    // thread is working
                                isWorking = true;
                            }
                        }

                        // process result
                        if (resultSize > config.getNumberOfProcess()) {

                            // wait for worker thread stop
                            for (WebCrawler crawler : crawlers) {
                                crawler.setWaitingForSave(true);
                                while (!crawler.isWaiting()) {
                                    Thread.sleep(10);
                                }

                                // take over all the results
                                resultPages.addAll(crawler.getResultPages());
                                crawler.clearResults();
                            }

                            // save json object to disk in offline mode
                            // or wait for analyzer in online mode
                            if (config.isOffline()) {
                                saveToDisk(resultPages, config.getFilePath());
                            } else {
                                Global.hasJSON = true;
                                System.out.println("Waiting for analyzer");
                                while (Global.hasJSON) {
                                    Thread.sleep(config.getThreadMonitorDelay());
                                }
                            }

                            // wake up all the worker threads
                            for (WebCrawler crawler : crawlers) {
                                crawler.setWaitingForSave(false);
                            }
                        }

                        // wait for a period of time to make sure
                        if (!isWorking) {
                            System.out.println("No one is working");
                            Thread.sleep(config.getThreadMonitorDelay());
                            for (int i = 0; i < threads.size(); i++) {
                                if (threads.get(i).isAlive() && !crawlers.get(i).isWaitingForURL()) {
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
            } catch (InterruptedException e) {
                System.out.println("Monitor thread failed to sleep!");
            }
        });

        monitor.start();
    }

    /**
     * Action before shutdown
     */
    public void shutdown() {
        frontier.shutdown();
        saveOnExit();
    }

    // TODO: 2017/4/9 resume from exit
    public void saveOnExit() {

    }

    /**
     * Save JSON results to disk
     */
    public synchronized void saveToDisk(List<WebPage> resultPages, String filePath) {
        try {
            // use utf-8 as encoder
            CharsetEncoder encoder = Charset.forName("UTF-8").newEncoder();
            OutputStreamWriter out = new OutputStreamWriter(new FileOutputStream(filePath, true), encoder);

            // get a JSON object from the result list
            JSONObject main = parseToJSON(resultPages);

            // remove all unicode characters
            String string = main.toString();
            string = StringEscapeUtils.unescapeJava(string);

            // write to disk
            out.write(string + '\n');
            out.close();

            System.out.println("Results saved");
        } catch (Exception e) {
            System.out.println("Failed to save results");
        }
    }

    /**
     * Create the file or folder and folders along the path if needed
     *
     * @param path Path of the file or folder
     */
    public void createFiles(String path, boolean isFolder) {
        try {
            File file = new File(path);
            if (!file.exists()) {
                if (isFolder) {
                    file.mkdirs();
                } else {
                    file.getParentFile().mkdirs();
                    file.createNewFile();
                }
                System.out.println("Created: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Failed to create files!");
        }
    }

    /**
     * Delete all the files and folders under the path
     *
     * @param path Path of a file or folder
     */
    public void deleteFiles(String path) {
        try {
            File dir = new File(path);

            if (dir.exists()) {
                for (File file : dir.listFiles()) {
                    file.delete();
                }
                dir.delete();
                System.out.println("Deleted: " + dir.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Failed to delete files!");
        }
    }

    /**
     * Parse results into JSON object
     *
     * @return JSON result
     */
    public JSONObject parseToJSON(List<WebPage> resultPages) {
        JSONObject main = new JSONObject();
        JSONArray page = new JSONArray();

        // put all the results in page array
        while (!resultPages.isEmpty()) {
            WebPage webPage = resultPages.remove(0);
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
