/**
 * Created by Lanslot on 2017/4/9.
 */

class MyCrawlerConfig extends CrawlerConfig {
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getUrl().toLowerCase();

        return href.contains("cn.dealmoon.com");
    }
}


public class WebCrawlerTest {
    public static void main(String[] args) {
        MyCrawlerConfig config = new MyCrawlerConfig();
        config.setNumberOfCrawler(5);
        config.setVisitDelay(200);


        config.addSeedURL("http://cn.dealmoon.com/");
        config.addSeedURL("http://cn.dealmoon.com/Electronics");
        config.addSeedURL("http://cn.dealmoon.com/Home-Garden");

        CrawlerMonitor crawlerMonitor = new CrawlerMonitor(config);
        crawlerMonitor.start();
    }
}
