package myMind.manager;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import myMind.controller.ContextMenuController;
import myMind.controller.TitleBarController;
import myMind.util.MessageUtil;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class ShortcutManager {
    private final Map<KeyCombination, ShortcutBinding> keyMap = new HashMap<>();
    private final Scene scene;

    public ShortcutManager(Scene scene, ContextMenuController contextMenuController, TitleBarController titleBarController) {
        this.scene = scene;
        // getAccelerators 在目标节点处理完之后，且不消费事件时才触发
        EventHandler<KeyEvent> keyEventHandler = this::handleKeyEvent;
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);

        register(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                contextMenuController::copy, "复制");
        register(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                contextMenuController::cut, "剪切");
        register(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN),
                contextMenuController::delete, "删除");
        register(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN),
                contextMenuController::deleteRemainChildren, "删除（保留子节点）");
        register(new KeyCodeCombination(KeyCode.DELETE, KeyCombination.ALT_DOWN),
                contextMenuController::deleteEmpty, "删除空白节点");

        register(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                titleBarController::moveRight, "右移");
        register(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                titleBarController::moveLeft, "左移");
        register(new KeyCodeCombination(KeyCode.UP, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                titleBarController::moveUp, "上移");
        register(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                titleBarController::moveDown, "下移");
//        load();
    }

    public void register(KeyCombination keyCombination, Runnable action, String description) {
        keyMap.put(keyCombination, new ShortcutBinding(action, description));
    }

    private void handleKeyEvent(KeyEvent event) {
        for (KeyCombination keyCombination : keyMap.keySet()) {
            if (keyCombination.match(event)) {
                event.consume();
                keyMap.get(keyCombination).getAction().run();
                break;
            }
        }
    }

    public void disable(KeyCombination keyCombination) {
        keyMap.get(keyCombination).setEnabled(false);
    }

    public void updateShortcut(KeyCombination oldCombination, KeyCombination newCombination) {
        if (keyMap.containsKey(newCombination)) {
            MessageUtil.showMessage("快捷键已存在");
        } else {
            File file = new File(ConfigManager.SHORTCUTS);
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = br.readLine()) != null) {
                    String[] split = line.split("=");
//                    combinationToString(oldCombination), combinationToString(newCombination)
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            keyMap.put(newCombination, keyMap.remove(oldCombination));
        }
    }

    //—————————————————————————————————————————持久化—————————————————————————————————————————
    public void load() {
        File file = new File(ConfigManager.SHORTCUTS);
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("=");
                keyMap.put(parseKeyCombination(split[1]), keyMap.remove(parseKeyCombination(split[0])));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String combinationToString(KeyCombination kc) {
        if (kc instanceof KeyCodeCombination kcc) {
            StringBuilder sb = new StringBuilder();
            if (kcc.getShift() == KeyCombination.ModifierValue.DOWN) sb.append("Shift+");
            if (kcc.getControl() == KeyCombination.ModifierValue.DOWN) sb.append("Ctrl+");
            if (kcc.getAlt() == KeyCombination.ModifierValue.DOWN) sb.append("Alt+");
            if (kcc.getMeta() == KeyCombination.ModifierValue.DOWN) sb.append("Meta+");
            if (kcc.getShortcut() == KeyCombination.ModifierValue.DOWN) sb.append("Shortcut+");
            sb.append(kcc.getCode().getName());
            return sb.toString();
        }
        return kc.getDisplayText();
    }

    public static KeyCodeCombination parseKeyCombination(String str) {
        if (str == null || str.isEmpty()) return null;
        String[] parts = str.toUpperCase().split("\\+");
        boolean shift = false, ctrl = false, alt = false, meta = false, shortcut = false;
        KeyCode keyCode = null;

        for (String part : parts) {
            part = part.trim();
            switch (part) {
                case "SHIFT" -> shift = true;
                case "CTRL", "CONTROL" -> ctrl = true;
                case "ALT" -> alt = true;
                case "META", "WINDOWS", "COMMAND" -> meta = true;
                case "SHORTCUT" -> shortcut = true;
                default -> {
                    try {
                        keyCode = KeyCode.valueOf(part);
                    } catch (IllegalArgumentException e) {
                        System.err.println("Unknown key code: " + part);
                    }
                }
            }
        }

        if (keyCode == null) return null;
        return new KeyCodeCombination(
                keyCode,
                shift ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                ctrl ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                alt ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                meta ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP,
                shortcut ? KeyCombination.ModifierValue.DOWN : KeyCombination.ModifierValue.UP
        );
    }
}