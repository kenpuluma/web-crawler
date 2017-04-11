import org.apache.http.HttpHost;
import org.apache.http.HttpStatus;
import org.apache.http.client.config.CookieSpecs;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import java.io.IOException;
import java.util.Date;
import java.util.Scanner;

/**
 * Created by Lanslot on 2017/4/11.
 */
public class HttpResponseClient {

    private final Object mutex = new Object();
    private CloseableHttpClient httpClient;

    /**
     * Last timestamp thread is working
     */
    private long lastVisitTime;

    /**
     * Default constructor
     * Set up the default header of HTTP requests according to config
     *
     * @param config The config of crawler defined by user
     */
    public HttpResponseClient(CrawlerConfig config) {
        this.lastVisitTime = 0;
        this.httpClient = preCookHttpClient(config);
    }

    /**
     * Set up the default header of HTTP requests according to config
     *
     * @param config The config of crawler defined by user
     * @return Cooked CloseableHttpClient
     */
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

    /**
     * Establish http connection with the url and get its response
     *
     * @param url        Link should be visited
     * @param visitDelay Delay between two requests
     * @return Result of http request
     * @throws IOException          On failed to execute http request
     * @throws InterruptedException On thread fail to sleep
     */
    public String getResponse(WebURL url, long visitDelay) throws IOException, InterruptedException {

        HttpUriRequest httpUriRequest = null;
        HTTPResponseResult responseResult = new HTTPResponseResult();
        String html = null;

        try {
            // wait for politeness delay
            synchronized (mutex) {
                long now = new Date().getTime();
                if (now - lastVisitTime < visitDelay) {
                    Thread.sleep(visitDelay - (now - lastVisitTime));
                }
                lastVisitTime = new Date().getTime();
            }

            // establish http connection
            httpUriRequest = new HttpGet(url.getUrl());
            CloseableHttpResponse httpResponse = this.httpClient.execute(httpUriRequest);
            responseResult.setStatusCode(httpResponse.getStatusLine().getStatusCode());
            responseResult.setResponseHeaders(httpResponse.getAllHeaders());
            responseResult.setHttpEntity(httpResponse.getEntity());

            // do something according to the response
            // TODO: 2017/4/9 response to other code
            if (responseResult.getStatusCode() == HttpStatus.SC_OK) {   // is 200
                html = fetch(responseResult);
            }

            httpResponse.close();

        } finally {
            if (responseResult.getHttpEntity() == null && httpUriRequest != null)
                httpUriRequest.abort();
        }
        return html;
    }

    /**
     * Get html content from the response
     *
     * @param responseResult Response of the connection
     * @return Html content of the page
     * @throws IOException On failed to read content
     */
    public String fetch(HTTPResponseResult responseResult) throws IOException {
        Scanner scanner = new Scanner(responseResult.getHttpEntity().getContent());
        StringBuilder stringBuilder = new StringBuilder();

        while (scanner.hasNext()) {
            stringBuilder.append(scanner.nextLine() + "\n");
        }
        return stringBuilder.toString();
    }
}
