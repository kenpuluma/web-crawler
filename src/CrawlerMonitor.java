import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 2017/4/9.
 */
public class CrawlerMonitor {

    private Frontier frontier;
    CrawlerConfig config;
    Object mutex = new Object();

    public CrawlerMonitor(CrawlerConfig config) {
        this.config = config;
        this.frontier = new Frontier(this.config.getMaxPages());
    }

    public void start() {

        frontier.setWorkQueue(config.getSeedURL());

        List<Thread> threads = new ArrayList<>();
        List<WebCrawler> crawlers = new ArrayList<>();

        for (int i = 0; i < config.getNumberOfCrawler(); i++) {
            WebCrawler crawler = new WebCrawler(config, frontier);
            Thread thread = new Thread(crawler, "Crawler " + i);
            thread.start();
            crawlers.add(crawler);
            threads.add(thread);
            System.out.printf("Crawler %d started\n", i);
        }

        Thread monitor = new Thread(() -> {
            try {
                synchronized (mutex) {
                    while (true) {
                        Thread.sleep(10000);
                        boolean isWorking = false;
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
                            } else if (!crawlers.get(i).isWaitingForURL()) {
                                isWorking = true;
                            }
                        }
                        if (!isWorking) {
                            System.out.println("No one is working");
                            Thread.sleep(10000);
                            for (int i = 0; i < threads.size(); i++) {
                                if (threads.get(i).isAlive() && !crawlers.get(i).isWaitingForURL()) {
                                    isWorking = true;
                                }
                            }

                            if (!isWorking) {
                                System.out.println("Shutting down");
                                saveOnExit();
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

    // TODO: 2017/4/9 resume from exit
    public void saveOnExit() {

    }


}
