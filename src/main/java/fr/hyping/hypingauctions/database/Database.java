package fr.hyping.hypingauctions.database;

import com.mongodb.MongoClientSettings;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;
import fr.hyping.hypingauctions.util.Configs;
import java.util.List;
import org.bukkit.configuration.file.FileConfiguration;

public class Database {

    protected final MongoClient client;
    protected final MongoDatabase db;

    public Database() {
        FileConfiguration config = Configs.getConfig("storage");

        String database = config.getString("database", "database");

        String uri = config.getString("uri", "");
        if(uri.isBlank()) {
            int port = config.getInt("port", 27017);
            String host = config.getString("host", "localhost");
            String authDatabase = config.getString("auth-database", "admin");
            String username = config.getString("user", "user");
            String password = config.getString("password", "passwd");

            try {
                MongoCredential credential =
                        MongoCredential.createCredential(username, authDatabase, password.toCharArray());
                this.client =
                        MongoClients.create(
                                MongoClientSettings.builder()
                                        .applyToClusterSettings(
                                                builder -> builder.hosts(List.of(new ServerAddress(host, port))))
                                        .credential(credential)
                                        .build());
            } catch (Exception e) {
                throw new RuntimeException("Failed to connect to the database", e);
            }
        } else {
            this.client = MongoClients.create(
                    MongoClientSettings.builder()
                            .applyConnectionString(new com.mongodb.ConnectionString(uri))
                            .build()
            );
        }

        this.db = client.getDatabase(database);
    }

    public void close() {
        client.close();
    }
}
