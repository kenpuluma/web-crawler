import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.json.JSONArray;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Scanner;

/**
 * Created by Lanslot on 2017/3/25.
 */


public class WebCrawler implements Runnable {

    private CrawlerConfig config;
    private long lastVisitTime;
    private CloseableHttpClient httpClient;
    private final Object mutex = new Object();
    private Frontier frontier;
    private boolean waitingForURL;
    List<WebPage> resultPages;

    public WebCrawler(CrawlerConfig config, Frontier frontier) {
        this.lastVisitTime = 0;
        this.config = config;
        this.httpClient = preCookHttpClient(this.config);
        this.frontier = frontier;
        resultPages = new ArrayList<>();
    }

    public boolean isWaitingForURL() {
        return waitingForURL;
    }

    @Override
    public void run() {
        while (true) {
            List<WebURL> workQueue = new ArrayList<>(50);
            this.waitingForURL = true;
            frontier.getNextURL(workQueue.size(), workQueue);
            this.waitingForURL = false;
            if (workQueue.isEmpty()) {
                try {
                    Thread.sleep(3000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            } else {
                for (WebURL curURL : workQueue) {
                    if (curURL != null) {
                        visit(curURL);
                        frontier.setProgressed(curURL);
                    }
                }
            }

            // TODO: 2017/4/9 save to disk maybe
            if (resultPages.size() > 10) {
                saveToDisk();
            }
        }
    }

    public CloseableHttpClient preCookHttpClient(CrawlerConfig config) {
        RequestConfig customRequest = RequestConfig.custom().setExpectContinueEnabled(false).setCookieSpec(CookieSpecs.STANDARD).setRedirectsEnabled(false).setSocketTimeout(config.getSocketTimeout()).setConnectionRequestTimeout(config.getConnectionTimeout()).build();

        HttpClientBuilder clientBuilder = HttpClientBuilder.create();
        clientBuilder.setDefaultRequestConfig(customRequest);
        clientBuilder.setUserAgent(config.getUserAgent());

        if (config.getProxyHost() != null) {
            HttpHost proxyHost = new HttpHost(config.getProxyHost(), config.getProxyPort());
            clientBuilder.setProxy(proxyHost);
        }

        CloseableHttpClient httpClient;
        httpClient = clientBuilder.build();
        return httpClient;
    }

    public void visit(WebURL url) {

        HttpUriRequest httpUriRequest = null;
        HTTPResponseResult responseResult = new HTTPResponseResult();

        try {
            synchronized (mutex) {
                long now = new Date().getTime();
                if (now - lastVisitTime < this.config.getVisitDelay()) {
                    Thread.sleep(this.config.getVisitDelay() - (now - lastVisitTime));
                }
                lastVisitTime = new Date().getTime();
            }

            httpUriRequest = new HttpGet(url.getUrl());
            CloseableHttpResponse httpResponse = this.httpClient.execute(httpUriRequest);
            responseResult.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            responseResult.setResponseHeaders(httpResponse.getAllHeaders());
            responseResult.setHttpEntity(httpResponse.getEntity());

            // TODO: 2017/4/9 response to other code
            if (responseResult.getStatusCode() == HttpStatus.SC_OK) {
                String html = fetch(responseResult);
                Elements links = parse(html, url.getUrl());

                if (config.getMaxDepth() < 0 || url.getDepth() + 1 < config.getMaxDepth())
                    frontier.scheduleWork(links, (short) (url.getDepth() + 1), config);

            }
            httpResponse.close();

            System.out.println("Visited: " + url.getUrl());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (responseResult.getHttpEntity() == null && httpUriRequest != null)
                httpUriRequest.abort();
        }
    }

    public String fetch(HTTPResponseResult responseResult) throws IOException {
        Scanner scanner = new Scanner(responseResult.getHttpEntity().getContent());
        StringBuffer stringBuffer = new StringBuffer();

        while (scanner.hasNext()) {
            stringBuffer.append(scanner.nextLine() + "\n");
        }
        return stringBuffer.toString();
    }


    public Elements parse(String html, String url) throws NoSuchAlgorithmException {
        Document document = Jsoup.parse(html);
        WebPage page = new WebPage();
        page.setUrl(url);
        page.setTitle(document.title());

        String plaintext = document.text();
        plaintext = plaintext.replaceAll("\\s+|\"+", " ");
        page.setText(plaintext);

        String description = page.findDescription();
        page.setDescription(description);

        String hashCode = frontier.getHash(page.getUrl());
        page.setHash(hashCode);

        // TODO: 2017/4/9 fix missing or garbled text
        if (page.getText() != null)
            resultPages.add(page);

        return document.select("a[href]");
    }

    public void saveToDisk() {
        synchronized (mutex) {
            File file = new File("N:\\Temps\\Save.dat");
            try {
                if (!file.exists()) {
                    file.createNewFile();
                }
                FileWriter out = new FileWriter(file, true);
                // for (WebPage page : resultPages) {
                //     out.write(page.getUrl() + "\n");
                //     resultPages.remove(page);
                // }

                JSONObject main = parseToJSON(resultPages);
                resultPages.clear();
                out.write(main.toString() + '\n');

                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        System.out.println("Results saved");
    }

    public JSONObject parseToJSON(List<WebPage> resultPages) {
        JSONObject main = new JSONObject();
        JSONArray page = new JSONArray();

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
