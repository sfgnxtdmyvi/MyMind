package myMind.common.manager.shortcut;

import javafx.event.EventHandler;
import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import myMind.common.constants.ConfigConstants;
import myMind.common.manager.ReferenceManager;
import myMind.common.util.MessageUtil;
import myMind.componet.MindMap;
import myMind.controller.ContextMenuController;
import myMind.controller.TitleBarController;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static javafx.scene.input.KeyCode.BACK_SPACE;
import static javafx.scene.input.KeyCode.C;
import static javafx.scene.input.KeyCode.DELETE;
import static javafx.scene.input.KeyCode.DOWN;
import static javafx.scene.input.KeyCode.EQUALS;
import static javafx.scene.input.KeyCode.K;
import static javafx.scene.input.KeyCode.L;
import static javafx.scene.input.KeyCode.LEFT;
import static javafx.scene.input.KeyCode.MINUS;
import static javafx.scene.input.KeyCode.RIGHT;
import static javafx.scene.input.KeyCode.UP;
import static javafx.scene.input.KeyCode.X;
import static javafx.scene.input.KeyCombination.ALT_DOWN;
import static javafx.scene.input.KeyCombination.SHIFT_DOWN;
import static javafx.scene.input.KeyCombination.SHORTCUT_DOWN;

public class ShortcutManager {
    private Scene scene;

    private Map<KeyCombination, ShortcutBinding> keyMap;
    private EventHandler<KeyEvent> keyEventHandler;
    private EventHandler<MouseEvent> mouseEventHandler;

    public ShortcutManager(Scene scene, MindMap mindMap, ContextMenuController contextMenuController, TitleBarController titleBarController) {
        this.scene = scene;
        keyMap = new HashMap<>();
        keyMap.put(new KeyCodeCombination(C, SHORTCUT_DOWN, SHIFT_DOWN),
                new ShortcutBinding(contextMenuController::copy, "复制"));
        keyMap.put(new KeyCodeCombination(X, SHORTCUT_DOWN, SHIFT_DOWN),
                new ShortcutBinding(contextMenuController::cut, "剪切"));

        keyMap.put(new KeyCodeCombination(MINUS, ALT_DOWN),
                new ShortcutBinding(contextMenuController::collapse, "收起"));
        keyMap.put(new KeyCodeCombination(EQUALS, ALT_DOWN),
                new ShortcutBinding(contextMenuController::expand, "展开"));

        keyMap.put(new KeyCodeCombination(BACK_SPACE, ALT_DOWN),
                new ShortcutBinding(contextMenuController::delete, "删除"));
        keyMap.put(new KeyCodeCombination(BACK_SPACE, SHORTCUT_DOWN, ALT_DOWN),
                new ShortcutBinding(contextMenuController::deleteRemainChildren, "删除（保留子节点）"));
        keyMap.put(new KeyCodeCombination(DELETE, ALT_DOWN),
                new ShortcutBinding(contextMenuController::deleteEmpty, "删除空白节点"));

        keyMap.put(new KeyCodeCombination(RIGHT, SHIFT_DOWN, ALT_DOWN),
                new ShortcutBinding(titleBarController::moveRight, "右移"));
        keyMap.put(new KeyCodeCombination(LEFT, SHIFT_DOWN, ALT_DOWN),
                new ShortcutBinding(titleBarController::moveLeft, "左移"));
        keyMap.put(new KeyCodeCombination(UP, SHIFT_DOWN, ALT_DOWN),
                new ShortcutBinding(titleBarController::moveUp, "上移"));
        keyMap.put(new KeyCodeCombination(DOWN, SHIFT_DOWN, ALT_DOWN),
                new ShortcutBinding(titleBarController::moveDown, "下移"));

        keyMap.put(new KeyCodeCombination(L, SHORTCUT_DOWN, ALT_DOWN),
                new ShortcutBinding(mindMap::format, "格式化"));
        keyMap.put(new KeyCodeCombination(K, SHORTCUT_DOWN, ALT_DOWN),
                new ShortcutBinding(mindMap::split, "分割节点"));
        load();

        keyEventHandler = event -> {
            for (KeyCombination keyCombination : keyMap.keySet()) {
                if (keyCombination.match(event)) {
                    event.consume();
                    keyMap.get(keyCombination).getAction().run();
                    break;
                }
            }
        };
        mouseEventHandler = event -> {
            if (event.getButton() == MouseButton.BACK) {
                ReferenceManager.back();
            }
        };
        // getAccelerators 在目标节点处理完之后，且不消费事件时才触发
        scene.addEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
        scene.addEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventHandler);
    }

    public void dispose() {
        scene.removeEventFilter(KeyEvent.KEY_PRESSED, keyEventHandler);
        scene.removeEventFilter(MouseEvent.MOUSE_CLICKED, mouseEventHandler);
        keyEventHandler = null;
        mouseEventHandler = null;
        keyMap.clear();
        keyMap = null;
    }

    public void disable(KeyCombination keyCombination) {
        keyMap.get(keyCombination).setEnabled(false);
    }

    // todo 持久化
    //—————————————————————————————————————————持久化—————————————————————————————————————————

    public void update(KeyCombination oldCombination, KeyCombination newCombination) {
        if (keyMap.containsKey(newCombination)) {
            MessageUtil.showMessage("快捷键已存在");
        } else {
            List<String> shortcutList = new ArrayList<>();
            File file = new File(ConfigConstants.DIR_SHORTCUTS);
            // 如果存在，则替换，否则添加
            try (BufferedReader br = new BufferedReader(new FileReader(file))) {
                String line;
                String newCombinationStr = combinationToString(newCombination);
                boolean updated = false;
                while ((line = br.readLine()) != null) {
                    String[] split = line.split("=");
                    if (split[1].equals(newCombinationStr)) {
                        shortcutList.add(split[0] + "=" + newCombinationStr);
                        updated = true;
                    } else {
                        shortcutList.add(line);
                    }
                }
                if (!updated) {
                    shortcutList.add(combinationToString(oldCombination) + "=" + newCombinationStr);
                }
                keyMap.put(newCombination, keyMap.remove(oldCombination));
            } catch (IOException e) {
                MessageUtil.showMessage("修改失败：" + e.getMessage());
            }

            try (BufferedWriter br = new BufferedWriter(new FileWriter(file))) {
                for (String s : shortcutList) {
                    br.write(s);
                    br.newLine();
                }
            } catch (IOException e) {
                MessageUtil.showMessage("修改失败：" + e.getMessage());
            }
        }
    }

    public void load() {
        try (BufferedReader br = new BufferedReader(new FileReader(ConfigConstants.DIR_SHORTCUTS))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] split = line.split("=");
                keyMap.put(parseKeyCombination(split[1]), keyMap.remove(parseKeyCombination(split[0])));
            }
        } catch (IOException e) {
            MessageUtil.showMessage("加载自定义快捷键失败：" + e.getMessage());
        }
    }

    public String combinationToString(KeyCombination kc) {
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

    public KeyCodeCombination parseKeyCombination(String str) {
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