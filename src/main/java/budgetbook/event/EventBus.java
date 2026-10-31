package budgetbook.event;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

/**
 * 簡單的事件匯流排（Observer 模式）。
 *
 * <p>service 在資料改變後發布事件，畫面訂閱自己關心的事件並重新整理；
 * 兩邊互不認識，新增畫面時不用修改 service。訂閱父型別也會收到子型別的事件。</p>
 */
public final class EventBus {
    private static final System.Logger LOG = System.getLogger(EventBus.class.getName());

    private final Map<Class<?>, List<Consumer<Object>>> handlers = new ConcurrentHashMap<>();

    /** 取消訂閱用的控制代碼。 */
    @FunctionalInterface
    public interface Subscription extends AutoCloseable {
        @Override
        void close();
    }

    public <E> Subscription subscribe(Class<E> type, Consumer<? super E> handler) {
        Consumer<Object> wrapper = event -> handler.accept(type.cast(event));
        List<Consumer<Object>> list = handlers.computeIfAbsent(type, key -> new CopyOnWriteArrayList<>());
        list.add(wrapper);
        return () -> list.remove(wrapper);
    }

    /** 同步通知所有訂閱者；某個訂閱者出錯不影響其他訂閱者。 */
    public void publish(Object event) {
        handlers.forEach((type, list) -> {
            if (type.isInstance(event)) {
                for (Consumer<Object> handler : list) {
                    try {
                        handler.accept(event);
                    } catch (RuntimeException e) {
                        LOG.log(System.Logger.Level.WARNING, "Event handler failed for " + event, e);
                    }
                }
            }
        });
    }

    public int subscriberCount() {
        return handlers.values().stream().mapToInt(List::size).sum();
    }
}
