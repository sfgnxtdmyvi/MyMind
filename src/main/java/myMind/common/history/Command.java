package myMind.common.history;

public interface Command {

    void execute();

    void undo();
}