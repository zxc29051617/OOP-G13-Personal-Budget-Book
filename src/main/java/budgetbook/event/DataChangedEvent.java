package budgetbook.event;

import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.Set;

/** 資料已變更。發布者不需要知道有哪些畫面在監聽（Observer 模式）。 */
public record DataChangedEvent(Set<Topic> topics) {
    public DataChangedEvent {
        topics = topics.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(topics));
    }

    public static DataChangedEvent of(Topic... topics) {
        return new DataChangedEvent(Set.copyOf(Arrays.asList(topics)));
    }

    public static DataChangedEvent everything() {
        return new DataChangedEvent(EnumSet.allOf(Topic.class));
    }

    public boolean affects(Topic topic) {
        return topics.contains(topic);
    }

    public boolean affectsAny(Collection<Topic> others) {
        return others.stream().anyMatch(topics::contains);
    }
}
