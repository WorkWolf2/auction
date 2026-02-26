package fr.hyping.hypingauctions.database;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.result.InsertOneResult;
import fr.hyping.hypingauctions.HypingAuctions;
import fr.hyping.hypingauctions.manager.AuctionManager;
import fr.hyping.hypingauctions.manager.BanManager;
import fr.hyping.hypingauctions.manager.object.Auction;
import fr.hyping.hypingauctions.manager.object.AuctionPlayer;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.logging.Level;
import org.bson.BsonValue;
import org.bson.Document;
import org.bukkit.OfflinePlayer;

public class AuctionDatabase extends Database {

    private final ScheduledExecutorService service;

    private final MongoCollection<Document> auctions;
    private final MongoCollection<Document> bought;
    private final MongoCollection<Document> banned;
    private final MongoCollection<Document> playerSessions;

    public AuctionDatabase() {
        super();
        this.auctions = db.getCollection("auctions");
        this.bought = db.getCollection("bought");
        this.banned = db.getCollection("banned");
        this.playerSessions = db.getCollection("player_sessions");
        this.service = Executors.newScheduledThreadPool(5);

        try {
            this.bought.createIndex(new Document("item_fingerprint", 1).append("sale_date", -1));
            this.bought.createIndex(new Document("item_fingerprint", 1).append("purchase_date", -1));
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Failed to create indexes for bought collection", e);
        }
    }

    /**
     * Attempts to cancel an auction by atomically removing it from the active
     * auctions collection.
     * Returns true when the listing existed and was removed; false when it was
     * already bought or
     * otherwise absent. Does not touch the "bought" collection.
     */
    public boolean cancelAuction(Auction auction) {
        try {
            Document deleted = auctions.findOneAndDelete(auction.toSearchDocument());
            return deleted != null;
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Failed to cancel auction " + auction.getId(), e);
            return false;
        }
    }

    /**
     * Atomically moves an auction from the "auctions" collection to the "bought"
     * collection. The
     * method first attempts to delete (and retrieve) the auction document by its
     * id. If the auction
     * is not present (already bought or expired), an IllegalStateException is
     * thrown so the caller
     * can react accordingly (typically by informing the player that the auction is
     * no longer
     * available).
     *
     * <p>
     * When the move succeeds we also proactively warm the {@link
     * fr.hyping.hypingauctions.service.AveragePriceService} cache so that
     * subsequent average-price
     * placeholder look-ups return instantly instead of timing-out and displaying 0.
     */
    public void buyAuction(AuctionPlayer player, Auction auction) {
        try {
            // Atomically fetch & delete – returns the deleted document or null if not
            // present
            Document deleted = auctions.findOneAndDelete(auction.toSearchDocument());
            if (deleted == null) {
                throw new IllegalStateException("Auction already bought or no longer exists in DB");
            }

            // Insert into the "bought" collection with buyer information
            // Record the exact purchase time so offline-sales queries are accurate
            long purchaseTime = System.currentTimeMillis();
            InsertOneResult result = bought.insertOne(
                    auction
                            .toDocument()
                            .append("buyer_id", player.getPlayer().getUniqueId().toString())
                            .append("purchase_date", purchaseTime)
                            .append("item_fingerprint", auction.getItemFingerprint())
                            .append("seller_credited", false));

            BsonValue id = result.getInsertedId();
            if (result.wasAcknowledged() && id != null) {
                auction.setId(id.asObjectId().getValue());
            }

            // Warm average-price cache asynchronously (fire-and-forget)
            fr.hyping.hypingauctions.service.AveragePriceService.getInstance()
                    .forceRecalculatePrice(auction.getItem());
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(
                            Level.SEVERE,
                            "Failed to complete buyAuction database operation for auction " + auction.getId(),
                            e);
            throw new RuntimeException("Database operation failed", e);
        }
    }

    /**
     * Mark a bought auction as credited to the seller. Safe to call multiple times.
     *
     * @param auction The auction to mark
     */
    public void markSellerCredited(Auction auction) {
        this.service.execute(
                () -> {
                    try {
                        bought.updateOne(
                                auction.toSearchDocument(),
                                new Document(
                                        "$set",
                                        new Document("seller_credited", true)
                                                .append("seller_credited_time", System.currentTimeMillis())));
                    } catch (Exception e) {
                        HypingAuctions.getInstance()
                                .getLogger()
                                .log(
                                        Level.WARNING,
                                        "Failed to mark seller as credited for auction " + auction.getId(),
                                        e);
                    }
                });
    }

    /**
     * Returns sales for a seller that have not yet been credited. When
     * sinceTimestampMs > 0, only
     * returns those with purchase_date greater than that (or sale_date fallback if
     * purchase_date is
     * absent for legacy records).
     */
    public List<Auction> getUncreditedSales(UUID sellerId, long sinceTimestampMs) {
        List<Auction> uncredited = new ArrayList<>();
        try {
            Document base = new Document("owner_id", sellerId.toString())
                    .append("seller_credited", new Document("$ne", true));

            Document timeFilter;
            if (sinceTimestampMs > 0) {
                timeFilter = new Document(
                        "$or",
                        List.of(
                                new Document("purchase_date", new Document("$gt", sinceTimestampMs)),
                                new Document(
                                        "$and",
                                        List.of(
                                                new Document("purchase_date", new Document("$exists", false)),
                                                new Document("sale_date", new Document("$gt", sinceTimestampMs))))));
            } else {
                timeFilter = new Document();
            }

            Document query = new Document(base);
            if (!timeFilter.isEmpty()) {
                query.append("$and", List.of(timeFilter));
            }

            bought
                    .find(query)
                    .forEach(
                            document -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);
                                    uncredited.add(auction);
                                } catch (Exception e) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .log(
                                                    Level.WARNING, "Error parsing uncredited sale for seller " + sellerId, e);
                                }
                            });
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Error querying uncredited sales for seller " + sellerId, e);
        }
        return uncredited;
    }

    public void registerAllAuctions() {
        try {
            final java.util.List<Auction> loaded = new java.util.ArrayList<>();
            auctions
                    .find()
                    .forEach(
                            (document) -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);
                                    AuctionPlayer seller = auction.getSeller();

                                    if (auction.isExpired()) {
                                        seller.getExpired().add(auction);
                                        return;
                                    }

                                    AuctionManager.addAuction(auction);
                                    seller.getSales().add(auction);
                                    loaded.add(auction);
                                } catch (Exception e) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .log(Level.WARNING, "Erreur lors du chargement d'une enchère", e);
                                }
                            });
            // Warm cache for all loaded auctions (fire-and-forget)
            java.util.List<org.bukkit.inventory.ItemStack> itemsToWarm = new java.util.ArrayList<>();
            for (Auction a : loaded)
                itemsToWarm.add(a.getItem());
            if (!itemsToWarm.isEmpty()) {
                warmCacheForMultipleItems(itemsToWarm);
            }
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, "Erreur lors du chargement des enchères", e);
        }
    }

    public CompletableFuture<List<Auction>> getPlayerHistory(UUID playerId) {
        return CompletableFuture.supplyAsync(
                () -> {
                    List<Auction> playerHistory = new ArrayList<>();

                    try {
                        // Find auctions where the player is the buyer
                        FindIterable<Document> buyerDocuments = bought.find(new Document("buyer_id", playerId.toString()));
                        buyerDocuments.forEach(
                                document -> {
                                    try {
                                        Auction auction = Auction.fromDocument(document);
                                        playerHistory.add(auction);
                                    } catch (Exception e) {
                                        HypingAuctions.getInstance()
                                                .getLogger()
                                                .log(
                                                        Level.WARNING,
                                                        "Erreur lors du chargement d'une enchère achetée depuis la DB pour "
                                                                + playerId,
                                                        e);
                                    }
                                });

                        // Find auctions where the player is the seller
                        FindIterable<Document> sellerDocuments = bought.find(new Document("owner_id", playerId.toString()));
                        sellerDocuments.forEach(
                                document -> {
                                    try {
                                        Auction auction = Auction.fromDocument(document);
                                        playerHistory.add(auction);
                                    } catch (Exception e) {
                                        HypingAuctions.getInstance()
                                                .getLogger()
                                                .log(
                                                        Level.WARNING,
                                                        "Erreur lors du chargement d'une enchère vendue depuis la DB pour "
                                                                + playerId,
                                                        e);
                                    }
                                });

                        playerHistory.sort((a1, a2) -> Long.compare(a2.getSaleDate(), a1.getSaleDate()));
                    } catch (Exception e) {
                        HypingAuctions.getInstance()
                                .getLogger()
                                .log(
                                        Level.WARNING,
                                        "Erreur lors du chargement de l'historique depuis la DB pour " + playerId,
                                        e);
                    }

                    return playerHistory;
                },
                service);
    }

    public void registerAllBought() {
        try {
            bought
                    .find()
                    .forEach(
                            (document) -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);

                                    if (auction.isPickedUp()) {
                                        return;
                                    }

                                    AuctionPlayer buyer = auction.getBuyer();
                                    if (buyer != null) {
                                        buyer.getPurchases().add(auction);
                                    }
                                } catch (Exception e) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .log(Level.WARNING, "Erreur lors du chargement d'un achat", e);
                                }
                            });
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, "Erreur lors du chargement des achats", e);
        }
    }

    public void registerAllBanned() {
        try {
            banned
                    .find()
                    .forEach(
                            (document) -> {
                                try {
                                    OfflinePlayer player = HypingAuctions.getInstance()
                                            .getServer()
                                            .getOfflinePlayer(UUID.fromString(document.getString("player_id")));
                                    BanManager.banPlayer(player, document.getLong("expiration"), false);
                                } catch (Exception e) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .log(Level.WARNING, "Erreur lors du chargement d'un ban", e);
                                }
                            });
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.SEVERE, "Erreur lors du chargement des joueurs bannis", e);
        }
    }

    public void addAuction(Auction auction) {
        InsertOneResult result = auctions.insertOne(auction.toDocument());
        BsonValue id = result.getInsertedId();
        if (result.wasAcknowledged() && id != null) {
            auction.setId(id.asObjectId().getValue());
        }
        // Pre-warm cache for the listed item
        try {
            fr.hyping.hypingauctions.service.AveragePriceService.getInstance().calculateAveragePrice(auction.getItem());
        } catch (Throwable ignored) {
        }
    }

    public void expireAuction(Auction auction) {
        this.service.execute(
                () -> {
                    auctions.updateOne(
                            auction.toSearchDocument(),
                            new Document("$set", new Document("expiration_time", 0L)));
                });
    }

    public void removeAuction(Auction auction) {
        this.service.execute(
                () -> {
                    auctions.deleteOne(auction.toSearchDocument());
                    bought.deleteOne(auction.toSearchDocument());
                });
    }

    public void removeBought(Auction auction) {
        this.service.execute(
                () -> {
                    bought.updateOne(
                            auction.toSearchDocument(), new Document("$set", new Document("picked_up", true)));
                });
    }

    public void banPlayer(UUID player, long expiration) {
        this.service.execute(
                () -> {
                    banned.insertOne(
                            new Document()
                                    .append("player_id", player.toString())
                                    .append("expiration", expiration));
                });
    }

    public void unbanPlayer(UUID player) {
        this.service.execute(
                () -> {
                    banned.deleteMany(new Document("player_id", player.toString()));
                });
    }

    /**
     * Get the bought collection for direct querying
     *
     * @return MongoCollection for bought auctions
     */
    public MongoCollection<Document> getBoughtCollection() {
        return bought;
    }

    /**
     * Warm average-price cache for multiple items in parallel.
     */
    public java.util.concurrent.CompletableFuture<Void> warmCacheForMultipleItems(
            java.util.List<org.bukkit.inventory.ItemStack> items) {
        if (items == null || items.isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }
        java.util.Map<String, org.bukkit.inventory.ItemStack> unique = new java.util.HashMap<>();
        for (org.bukkit.inventory.ItemStack is : items) {
            if (is == null)
                continue;
            org.bukkit.inventory.ItemStack single = is.clone();
            single.setAmount(1);
            try {
                String key = fr.hyping.hypingauctions.service.AveragePriceService.getInstance()
                        .getSafeFingerprint(single);
                unique.putIfAbsent(key, single);
            } catch (Throwable ignored) {
                unique.putIfAbsent("MATERIAL:" + single.getType().name(), single);
            }
        }

        java.util.List<java.util.concurrent.CompletableFuture<Integer>> futures = new java.util.ArrayList<>();
        for (org.bukkit.inventory.ItemStack is : unique.values()) {
            futures.add(
                    fr.hyping.hypingauctions.service.AveragePriceService.getInstance()
                            .calculateAveragePrice(is));
        }
        if (futures.isEmpty()) {
            return java.util.concurrent.CompletableFuture.completedFuture(null);
        }

        return java.util.concurrent.CompletableFuture.allOf(
                futures.toArray(new java.util.concurrent.CompletableFuture[0]));
    }

    /**
     * Update player's last logout time
     *
     * @param playerId   The player's UUID
     * @param logoutTime The logout timestamp
     */
    public void updatePlayerLogoutTime(UUID playerId, long logoutTime) {
        this.service.execute(
                () -> {
                    playerSessions.replaceOne(
                            new Document("player_id", playerId.toString()),
                            new Document()
                                    .append("player_id", playerId.toString())
                                    .append("last_logout", logoutTime)
                                    .append("last_login", System.currentTimeMillis()),
                            new com.mongodb.client.model.ReplaceOptions().upsert(true));
                });
    }

    /**
     * Get player's last logout time
     *
     * @param playerId The player's UUID
     * @return The last logout timestamp, or 0 if never logged out
     */
    public long getPlayerLastLogout(UUID playerId) {
        try {
            Document session = playerSessions.find(new Document("player_id", playerId.toString())).first();
            if (session != null && session.containsKey("last_logout")) {
                return session.getLong("last_logout");
            }
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Error getting last logout time for player " + playerId, e);
        }
        return 0;
    }

    /**
     * Update player's login time
     *
     * @param playerId  The player's UUID
     * @param loginTime The login timestamp
     */
    public void updatePlayerLoginTime(UUID playerId, long loginTime) {
        this.service.execute(
                () -> {
                    playerSessions.updateOne(
                            new Document("player_id", playerId.toString()),
                            new Document("$set", new Document("last_login", loginTime)),
                            new com.mongodb.client.model.UpdateOptions().upsert(true));
                });
    }

    /**
     * Get sales that happened while a player was offline
     *
     * @param sellerId       The seller's UUID
     * @param lastLogoutTime The timestamp when the player last logged out
     * @return List of auctions sold while offline
     */
    public List<Auction> getOfflineSales(UUID sellerId, long lastLogoutTime) {
        List<Auction> offlineSales = new ArrayList<>();
        try {
            // Query bought collection for items sold by this player after their last logout
            // Prefer the accurate "purchase_date" field when present; fall back to legacy
            // "sale_date"
            Document query = new Document("owner_id", sellerId.toString())
                    .append(
                            "$or",
                            List.of(
                                    new Document("purchase_date", new Document("$gt", lastLogoutTime)),
                                    new Document(
                                            "$and",
                                            List.of(
                                                    new Document("purchase_date", new Document("$exists", false)),
                                                    new Document("sale_date", new Document("$gt", lastLogoutTime))))));

            bought
                    .find(query)
                    .forEach(
                            document -> {
                                try {
                                    Auction auction = Auction.fromDocument(document);
                                    offlineSales.add(auction);
                                } catch (Exception e) {
                                    HypingAuctions.getInstance()
                                            .getLogger()
                                            .log(Level.WARNING, "Error parsing offline sale for player " + sellerId, e);
                                }
                            });

            // Sort by purchase date when available, otherwise by sale date (newest first)
            offlineSales.sort(
                    (a1, a2) -> Long.compare(
                            (a2.getPurchaseDate() > 0 ? a2.getPurchaseDate() : a2.getSaleDate()),
                            (a1.getPurchaseDate() > 0 ? a1.getPurchaseDate() : a1.getSaleDate())));
        } catch (Exception e) {
            HypingAuctions.getInstance()
                    .getLogger()
                    .log(Level.WARNING, "Error getting offline sales for player " + sellerId, e);
        }
        return offlineSales;
    }

    public void updateAuctionPremium(Auction auction) {
        this.service.execute(
                () -> {
                    auctions.updateOne(
                            auction.toSearchDocument(),
                            new Document(
                                    "$set",
                                    new Document()
                                            .append("premium_expiration", auction.getPremiumExpiration())
                                            .append("premium_slot", auction.getPremiumSlot())));
                });
    }
}
