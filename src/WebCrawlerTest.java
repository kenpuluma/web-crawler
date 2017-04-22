/**
 * Created by Lanslot on 2017/4/9.
 */

class MyCrawlerConfig extends CrawlerConfig {
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getUrl().toLowerCase();

        return href.contains("http://sports.sina.com.cn");
    }
}


public class WebCrawlerTest {
    public static void main(String[] args) {
        MyCrawlerConfig config = new MyCrawlerConfig();
        config.setNumberOfCrawler(7);
        config.setVisitDelay(-1);
        config.setMaxPages(-1);
        config.setFilePath("N:\\Temps\\en");


        config.addSeedURL("http://sports.sina.com.cn/");


        CrawlerMonitor crawlerMonitor = new CrawlerMonitor(config);
        crawlerMonitor.start();
    }
}
