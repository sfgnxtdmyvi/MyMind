package myMind.common.manager;

import javafx.scene.Scene;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyCodeCombination;
import javafx.scene.input.KeyCombination;
import javafx.scene.input.KeyEvent;
import myMind.common.constants.ConfigConstants;
import myMind.controller.ContextMenuController;
import myMind.controller.TitleBarController;
import myMind.common.util.MessageUtil;

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

public class ShortcutManager {
    private Map<KeyCombination, ShortcutBinding> keyMap;

    public ShortcutManager(Scene scene, ContextMenuController contextMenuController, TitleBarController titleBarController) {
        keyMap = new HashMap<>();
        keyMap.put(new KeyCodeCombination(KeyCode.C, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                new ShortcutBinding(contextMenuController::copy, "复制"));
        keyMap.put(new KeyCodeCombination(KeyCode.X, KeyCombination.SHORTCUT_DOWN, KeyCombination.SHIFT_DOWN),
                new ShortcutBinding(contextMenuController::cut, "剪切"));
        keyMap.put(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN),
                new ShortcutBinding(contextMenuController::delete, "删除"));
        keyMap.put(new KeyCodeCombination(KeyCode.BACK_SPACE, KeyCombination.ALT_DOWN, KeyCombination.SHORTCUT_DOWN),
                new ShortcutBinding(contextMenuController::deleteRemainChildren, "删除（保留子节点）"));
        keyMap.put(new KeyCodeCombination(KeyCode.DELETE, KeyCombination.ALT_DOWN),
                new ShortcutBinding(contextMenuController::deleteEmpty, "删除空白节点"));

        keyMap.put(new KeyCodeCombination(KeyCode.RIGHT, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                new ShortcutBinding(titleBarController::moveRight, "右移"));
        keyMap.put(new KeyCodeCombination(KeyCode.LEFT, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                new ShortcutBinding(titleBarController::moveLeft, "左移"));
        keyMap.put(new KeyCodeCombination(KeyCode.UP, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                new ShortcutBinding(titleBarController::moveUp, "上移"));
        keyMap.put(new KeyCodeCombination(KeyCode.DOWN, KeyCombination.SHIFT_DOWN, KeyCombination.ALT_DOWN),
                new ShortcutBinding(titleBarController::moveDown, "下移"));
        load();

        // getAccelerators 在目标节点处理完之后，且不消费事件时才触发
        scene.addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            for (KeyCombination keyCombination : keyMap.keySet()) {
                if (keyCombination.match(event)) {
                    event.consume();
                    keyMap.get(keyCombination).getAction().run();
                    break;
                }
            }
        });
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