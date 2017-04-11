import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/4/9.
 */
public class CrawlerMonitor {

    private Frontier frontier;
    private CrawlerConfig config;
    private Object mutex = new Object();

    /**
     * Default constructor
     * Store the config in memory and initialize the frontier
     *
     * @param config The config of crawler defined by user
     */
    public CrawlerMonitor(CrawlerConfig config) {
        this.config = config;
        this.frontier = new Frontier(this.config.getMaxPages());
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
            WebCrawler crawler = new WebCrawler(config, frontier);
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

                        // scan all worker threads
                        for (int i = 0; i < threads.size(); i++) {
                            if (!threads.get(i).isAlive()) {
                                System.out.printf("Crawler %d dead\n", i);
                                WebCrawler crawler = new WebCrawler(config, frontier);
                                Thread thread = new Thread(crawler, "Crawler " + i);
                                thread.start();
                                threads.remove(i);
                                threads.add(i, thread);
                                crawlers.remove(i);
                                crawlers.add(i, crawler);
                                System.out.printf("Crawler %d started\n", i);
                            } else if (!crawlers.get(i).isWaitingForURL()) {
                                isWorking = true;
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

}
