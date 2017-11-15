/**
 * Created by Lanslot on 2017/4/9.
 */

//  override the origin filter rule
class MyCrawlerConfig extends CrawlerConfig {
    @Override
    public boolean shouldVisit(WebURL url) {
        String href = url.getUrl().toLowerCase();

        return href.contains("www.espn.com/nba/");
    }
}


public class WebCrawlerTest {
    public static void main(String[] args) {
        MyCrawlerConfig config = new MyCrawlerConfig();

        // do some configuration
        // connection config
        config.setNumberOfCrawler(7);
        config.addSeedURL("http://www.espn.com/nba/");
        config.setVisitDelay(-1);
        config.setMaxPages(-1);

        // processing config
        config.setNumberOfProcess(5);
        config.setDescriptionLength(75);

        // mode config
        config.setOffline(true);
        config.setResumable(false);
        config.setFilePath("C:\\Cache\\cs521\\result.dat");
        config.setWorkPath("C:\\Cache\\cs521\\frontier");


        CrawlerMonitor crawlerMonitor = new CrawlerMonitor(config);
        crawlerMonitor.start();
    }
}
