package fr.hyping.hypingauctions.manager.object;

import fr.hyping.hypingauctions.manager.CategoryManager;
import org.bukkit.OfflinePlayer;

public class Filter {

    private SortType sortType;
    private SortOrder sortOrder;
    private Category category;
    private OfflinePlayer player;
    private String search;
    private MaterialData itemFilter;

    public Filter() {
        reset();
    }

    public Category getCategory() {
        return category;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public SortType getSortType() {
        return sortType;
    }

    public OfflinePlayer getPlayer() {
        return player;
    }

    public String getSearch() {
        return search;
    }

    public MaterialData getItemFilter() {
        return itemFilter;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
    }

    public void setSortType(SortType sortType) {
        this.sortType = sortType;
    }

    public void setPlayer(OfflinePlayer player) {
        this.player = player;
    }

    public void setSearch(String search) {
        this.search = search;
    }

    public void setItemFilter(MaterialData itemFilter) {
        this.itemFilter = itemFilter;
    }

    public void reset() {
        this.category = CategoryManager.getDefaultCategory();
        this.sortOrder = SortOrder.ASCENDING;
        this.sortType = SortType.DATE;
        this.player = null;
        this.search = null;
        this.itemFilter = null;
    }

    public enum SortType {
        NAME,
        PRICE,
        DATE
    }

    public enum SortOrder {
        ASCENDING,
        DESCENDING
    }
}
