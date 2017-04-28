import com.sleepycat.je.*;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Lanslot on 4/27/2017.
 */
public class WorkQueueDB {
    private Database workdb;
    private Environment env;
    private WebURLTupleBinding tupleBinding;
    private final Object mutex = new Object();

    private boolean resumable;

    /**
     * The count of urls
     */
    private int pageNumber;

    public WorkQueueDB(EnvironmentConfig envConfig, DatabaseConfig dbConfig, String path) {
        pageNumber = 0;
        resumable = envConfig.getTransactional();
        tupleBinding = new WebURLTupleBinding();

        File file = new File(path);
        this.env = new Environment(file, envConfig);
        this.workdb = env.openDatabase(null, "workDB", dbConfig);
    }

    /**
     * Close the database
     */
    public void closeDB() {
        if (workdb != null)
            workdb.close();
        if (env != null)
            env.close();
    }

    /**
     * Put url and its id into db
     *
     * @param url Url for website in String
     */
    public void put(WebURL url) {
        synchronized (mutex) {
            Transaction tnx = getTransaction();
            DatabaseEntry key = new DatabaseEntry(intToByte(pageNumber));
            DatabaseEntry value = new DatabaseEntry();
            tupleBinding.objectToEntry(url, value);

            workdb.putNoOverwrite(tnx, key, value);
            pageNumber++;
            commit(tnx);
        }
    }


    public List<WebURL> getWebURLs(int size) {
        synchronized (mutex) {
            Transaction tnx = getTransaction();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();
            List<WebURL> urlList = new ArrayList<>();

            try (Cursor cursor = workdb.openCursor(tnx, null)) {
                OperationStatus result = cursor.getFirst(key, value, null);
                int count = 0;
                while ((count < size) && (result == OperationStatus.SUCCESS)) {
                    if (value.getData().length > 0) {
                        urlList.add(tupleBinding.entryToObject(value));
                        count++;
                    }
                    result = cursor.getNext(key, value, null);
                }
            }

            commit(tnx);
            return urlList;
        }
    }

    public void delete(int size) {
        synchronized (mutex) {
            Transaction tnx = getTransaction();
            DatabaseEntry key = new DatabaseEntry();
            DatabaseEntry value = new DatabaseEntry();

            try (Cursor cursor = workdb.openCursor(tnx, null)) {
                OperationStatus result = cursor.getFirst(key, value, null);
                int count = 0;
                while ((count < size) && (result == OperationStatus.SUCCESS)) {
                    cursor.delete();
                    count++;
                    result = cursor.getNext(key, value, null);
                }
            }

            commit(tnx);
        }
    }

    public Transaction getTransaction() {
        return resumable ? env.beginTransaction(null, null) : null;
    }

    protected static void commit(Transaction tnx) {
        if (tnx != null) {
            tnx.commit();
        }
    }

    public byte[] intToByte(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }

    public int getPageNumber(){
        synchronized (mutex){
            return pageNumber;
        }
    }
}
