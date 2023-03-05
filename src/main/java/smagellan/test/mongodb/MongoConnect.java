package smagellan.test.mongodb;

import com.mongodb.*;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.MongoIterable;
import de.bwaldvogel.mongo.MongoBackend;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.ServerVersion;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.List;

public class MongoConnect {
    private static final org.slf4j.Logger logger = LoggerFactory.getLogger(MongoConnect.class);

    public static void main(String[] args) throws InterruptedException {
        //BasicDBObject.parse();

        MongoClientOptions opts = new MongoClientOptions.Builder()
                .applicationName("testApp")
                .addServerListener(new TracingServerListener())
                .addServerMonitorListener(new TracingServerMonitorListener())
                .compressorList(Arrays.asList(MongoCompressor.createZlibCompressor(), MongoCompressor.createSnappyCompressor()))
                .heartbeatFrequency(10_000)
                .build();
        logger.info("compressor list: {}", opts.getCompressorList());
        //MongoCredential credential = MongoCredential.createCredential("mongouser", "admin", "someothersecret".toCharArray());
        //MongoCredential credential = MongoCredential.createCredential("mongoadmin", "admin", "secret".toCharArray());
        //MongoCredential credential = MongoCredential.createPlainCredential("mongoadmin", "admin", "secret".toCharArray());
        MongoCredential credential = null;

        MongoServer srv = null;
        try {
            MongoBackend backend = new MemoryBackend()
                    .version(ServerVersion.MONGO_3_6);
            srv = new MongoServer(backend);
            InetSocketAddress serverAddress = srv.bind();
            logger.info("server status: {}", backend.getServerStatus());
            try (MongoClient client = new MongoClient(new ServerAddress("localhost", serverAddress.getPort()), credential, opts)) {
                MongoIterable<String> namesIterable = client.listDatabaseNames();
                List<String> dbNames = MongoUtils.pull(namesIterable);
                logger.info("listDatabaseNames: {}", dbNames);
                for (String dbName : dbNames) {
                    MongoDatabase db = client.getDatabase(dbName);
                    traceUsers(db);
                    List<String> collectionNames = MongoUtils.pull(db.listCollectionNames());
                    logger.info("listCollectionNames({}): {}", dbName, collectionNames);
                }
                //Thread.sleep(30 * 1000);
            }
        } finally {
            if (srv != null) {
                srv.shutdown();
            }
        }
    }

    private static void traceUsers(MongoDatabase db) {
        BasicDBObject dbStats = new BasicDBObject("usersInfo", 1);
        Document command = db.runCommand(dbStats);
        System.out.println(command.get("users"));
        List users = (List) command.get("users");
        logger.info("users of {}", db.getName());
        for (Object u : users) {
            logger.info("user: {}", u);
        }
    }
}
