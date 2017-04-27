/**
 * Created by Lanslot on 2017/4/9.
 */

//  override the origin filter rule
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

        // do some configuration
        // connection config
        config.setNumberOfCrawler(7);
        config.addSeedURL("http://sports.sina.com.cn/");
        config.setVisitDelay(-1);
        config.setMaxPages(-1);

        // processing config
        config.setNumberOfProcess(5);
        config.setDescriptionLength(75);

        // mode config
        config.setOffline(true);
        config.setResumable(false);
        config.setFilePath("C:\\Cache\\cs609\\frontier\\result.dat");
        config.setWorkPath("C:\\Cache\\cs609\\frontier");


        CrawlerMonitor crawlerMonitor = new CrawlerMonitor(config);
        crawlerMonitor.start();
    }
}
