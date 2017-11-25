package net.seansitter.mcsvr.cache.listener;

public enum Event {
    CACHE_HIT,
    CACHE_MISS,
    PUT_ENTRY,
    UPDATE_ENTRY,
    DELETE_ENTRY,
    DESTROY_ENTRIES
}
