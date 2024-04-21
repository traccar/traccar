package org.traccar.session.cache;

import org.traccar.model.BaseModel;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

public class CacheNode {

    private BaseModel value;

    private final Map<Class<? extends BaseModel>, Set<CacheNode>> links = new HashMap<>();
    private final Map<Class<? extends BaseModel>, Set<CacheNode>> backlinks = new HashMap<>();

    public CacheNode(BaseModel value) {
        this.value = value;
    }

    public BaseModel getValue() {
        return value;
    }

    public void setValue(BaseModel value) {
        this.value = value;
    }

    public Set<CacheNode> getLinks(Class<? extends BaseModel> clazz, boolean forward) {
        var map = forward ? links : backlinks;
        return map.computeIfAbsent(clazz, k -> new HashSet<>());
    }

    public Stream<CacheNode> getAllLinks(boolean forward) {
        var map = forward ? links : backlinks;
        return map.values().stream().flatMap(Set::stream);
    }

}
