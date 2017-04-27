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
    private final Object mutex = new Object();

    /**
     * The unique id for each url
     * This will avoid redundancy
     */
    private int lastUrlID;

    /**
     * Default constructor
     * Create the url database according to user configs
     *
     * @param envConfig The environment config
     * @param dbConfig The database config
     * @param path The path to working folder
     */
    public UrlDB(EnvironmentConfig envConfig, DatabaseConfig dbConfig, String path) {
        lastUrlID = 0;
        File file = new File(path);
        this.env = new Environment(file, envConfig);
        this.urldb = env.openDatabase(null, "urlDB", dbConfig);
    }

    /**
     * Close the database
     */
    public void closeDB() {
        if (urldb != null)
            urldb.close();
        if (env != null)
            env.close();
    }

    /**
     * Put url and its id into db
     *
     * @param url Url for website in String
     */
    public void put(String url) {
        synchronized (mutex) {
            try {
                DatabaseEntry key = new DatabaseEntry(url.getBytes("UTF-8"));
                DatabaseEntry value = new DatabaseEntry(intToByte(lastUrlID));
                lastUrlID++;

                urldb.putNoOverwrite(null, key, value);
            } catch (UnsupportedEncodingException e) {
                System.out.println("Encoding error!");
            }
        }
    }

    /**
     * Return the id of an url
     * Return -1 if it's not in db
     *
     * @param url Url for website in String
     * @return The id of the url
     */
    public int getID(String url) {
        synchronized (mutex) {
            try {
                DatabaseEntry key = new DatabaseEntry(url.getBytes("UTF-8"));
                DatabaseEntry value = new DatabaseEntry();

                OperationStatus result;
                result = urldb.get(null, key, value, null);

                // return the id if url is found
                if (result == OperationStatus.SUCCESS && value.getData().length > 0) {
                    return byteToInt(value.getData());
                }
            } catch (UnsupportedEncodingException e) {
                System.out.println("Encoding error!");
            }
            return -1;
        }
    }

    /**
     * Transfer byte array to integer
     *
     * @param bytes Byte array
     * @return Integer
     */
    public int byteToInt(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getInt();
    }

    /**
     * Transfer integer to byte array
     *
     * @param in Integer
     * @return Byte array
     */
    public byte[] intToByte(int in) {
        return ByteBuffer.allocate(4).putInt(in).array();
    }
}
