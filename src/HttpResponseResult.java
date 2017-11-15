import org.apache.http.Header;
import org.apache.http.HttpEntity;

/**
 * Created by Lanslot on 2017/3/27.
 */
public class HttpResponseResult {

    private int statusCode;
    private HttpEntity httpEntity;
    private Header[] responseHeaders;


    public int getStatusCode() {
        return statusCode;
    }

    public HttpEntity getHttpEntity() {
        return httpEntity;
    }

    public Header[] getResponseHeaders() {
        return responseHeaders;
    }

    public void setStatusCode(int statusCode) {
        this.statusCode = statusCode;
    }

    public void setHttpEntity(HttpEntity httpEntity) {
        this.httpEntity = httpEntity;
    }

    public void setResponseHeaders(Header[] responseHeaders) {
        this.responseHeaders = responseHeaders;
    }

}
