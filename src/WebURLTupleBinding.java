import com.sleepycat.bind.tuple.TupleBinding;
import com.sleepycat.bind.tuple.TupleInput;
import com.sleepycat.bind.tuple.TupleOutput;

/**
 * Created by Lanslot on 4/27/2017.
 */
public class WebURLTupleBinding extends TupleBinding<WebURL> {

    @Override
    public WebURL entryToObject(TupleInput tupleInput) {
        return new WebURL(tupleInput.readString(), tupleInput.readShort());
    }

    @Override
    public void objectToEntry(WebURL webURL, TupleOutput tupleOutput) {
        tupleOutput.writeString(webURL.getUrl());
        tupleOutput.writeShort(webURL.getDepth());
    }
}
