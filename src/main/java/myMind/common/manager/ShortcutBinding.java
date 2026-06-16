package myMind.common.manager;

import lombok.Data;

@Data
public class ShortcutBinding {
    private Runnable action;
    private String description;
    private boolean enabled;

    public ShortcutBinding(Runnable action, String description) {
        this.action = action;
        this.description = description;
    }
}