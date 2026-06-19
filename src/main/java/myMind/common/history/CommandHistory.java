package myMind.common.history;

import java.util.ArrayDeque;
import java.util.Deque;

public class CommandHistory {

    private final Deque<Command> undoStack = new ArrayDeque<>();
    private final Deque<Command> redoStack = new ArrayDeque<>();
    // 可撤销的最大步数
    private final int maxSize = 50;

    public void execute(Command command) {
        command.execute();
        undoStack.push(command);
        //新操作后不能再 redo
        redoStack.clear();
        if (undoStack.size() > maxSize) {
            undoStack.removeLast();
        }
    }


    public void undo() {
        if (undoStack.isEmpty()) {
            return;
        }
        Command command = undoStack.pop();
        command.undo();
        redoStack.push(command);
    }

    public void redo() {
        if (redoStack.isEmpty()) {
            return;
        }
        Command command = redoStack.pop();
        command.execute();
        // 重做也可以被撤销
        undoStack.push(command);
    }
}