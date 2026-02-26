package tomas.gonzalez.ConvertidorUnificadorJavaSwing.app.event;

import javax.swing.SwingUtilities;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * Simple synchronous event bus. Dispatches all events on the EDT.
 */
public class EventBus {

    private static final EventBus INSTANCE = new EventBus();
    private final Map<Class<?>, List<Consumer<AppEvent>>> listeners = new ConcurrentHashMap<>();

    private EventBus() {}

    public static EventBus get() { return INSTANCE; }

    @SuppressWarnings("unchecked")
    public <T extends AppEvent> void subscribe(Class<T> eventType, Consumer<T> listener) {
        listeners.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>())
                 .add((Consumer<AppEvent>) listener);
    }

    public <T extends AppEvent> void unsubscribe(Class<T> eventType, Consumer<T> listener) {
        List<Consumer<AppEvent>> list = listeners.get(eventType);
        if (list != null) list.remove(listener);
    }

    public void publish(AppEvent event) {
        List<Consumer<AppEvent>> list = listeners.get(event.getClass());
        if (list == null || list.isEmpty()) return;

        List<Consumer<AppEvent>> snapshot = new ArrayList<>(list);
        if (SwingUtilities.isEventDispatchThread()) {
            snapshot.forEach(l -> l.accept(event));
        } else {
            SwingUtilities.invokeLater(() -> snapshot.forEach(l -> l.accept(event)));
        }
    }
}
