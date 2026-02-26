package fr.hyping.hypingauctions.sessions;

public interface PaginatedSession {

    int getPage();

    void setPage(int page);

    int getFirstPage();

    int getLastPage();

    default boolean hasPreviousPage() {
        return this.getPage() > this.getFirstPage();
    }

    default boolean hasNextPage() {
        return this.getPage() < this.getLastPage();
    }

    default int previousPage() {
        int previousPage = this.getPage() - 1;
        return Math.max(previousPage, this.getFirstPage());
    }

    default int nextPage() {
        int nextPage = this.getPage() + 1;
        return Math.min(nextPage, this.getLastPage());
    }

}
