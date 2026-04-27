package com.algosys.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public class EventBus {
    private static final EventBus INSTANCE = new EventBus();
    private final Map<String, List<Consumer<Object>>> listeners = new HashMap<>();

    public static EventBus getInstance() {
        return INSTANCE;
    }

    public void subscribe(String event, Consumer<Object> listener) {
        listeners.computeIfAbsent(event, k -> new ArrayList<>()).add(listener);
    }

    public void publish(String event, Object payload) {
        List<Consumer<Object>> subs = listeners.getOrDefault(event, List.of());
        for (Consumer<Object> sub : subs) {
            sub.accept(payload);
        }
    }
}
