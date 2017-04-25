import com.sleepycat.je.*;

import java.io.File;
import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;

/**
 * Created by Lanslot on 2017/4/25.
 */
public class UrlDB {
    private Database urldb;
    private Environment env;
    private int lastUrlID;

    private final Object mutex = new Object();

    public UrlDB(EnvironmentConfig envConfig, DatabaseConfig dbConfig, String path) {
        lastUrlID = 0;
        File file = new File(path);

        try {
            if (!file.exists()) {
                file.mkdir();
                System.out.println("Created folder: " + file.getAbsolutePath());
            }
        } catch (Exception e) {
            System.out.println("Failed to create working folder.");
        }

        this.env = new Environment(file, envConfig);
        this.urldb = env.openDatabase(null, "urlDB", dbConfig);
    }

    public void closeDB() {
        if (urldb != null)
            urldb.close();
        if (env != null)
            env.close();
    }

    public void put(String url) throws UnsupportedEncodingException {
        synchronized (mutex) {
            DatabaseEntry key = new DatabaseEntry(url.getBytes("UTF-8"));
            DatabaseEntry value = new DatabaseEntry(intToByte(lastUrlID));
            lastUrlID++;

            urldb.putNoOverwrite(null, key, value);
        }
    }

    public int getID(String url) throws UnsupportedEncodingException {
        synchronized (mutex) {
            DatabaseEntry key = new DatabaseEntry(url.getBytes("UTF-8"));
            DatabaseEntry value = new DatabaseEntry();

            OperationStatus result;
            result = urldb.get(null, key, value, null);

            if (result == OperationStatus.SUCCESS && value.getData().length > 0) {
                return byteToInt(value.getData());
            }

            return -1;
        }
    }

    public int byteToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    public byte[] intToByte(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }
}
