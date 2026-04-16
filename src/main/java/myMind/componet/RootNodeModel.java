package myMind.componet;

import lombok.Getter;
import lombok.Setter;
import myMind.constants.PosConstants;

import java.util.ArrayList;
import java.util.List;

public class RootNodeModel extends NodeModel {
    @Getter
    @Setter
    private final List<NodeModel> leftChildren = new ArrayList<>();

    public RootNodeModel(int id, String text, double x, double y) {
        super(id, text, x, y, PosConstants.MIDDLE);
    }
}
