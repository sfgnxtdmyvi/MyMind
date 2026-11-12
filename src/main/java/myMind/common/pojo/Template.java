package myMind.common.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import myMind.componet.MapNode;

@Data
@AllArgsConstructor
public class Template {

    private MapNode mapNode;
    private String name;
    private String preview;
    private double ratio;

    public Template(String preview, double ratio) {
        this.preview = preview;
        this.ratio = ratio;
    }
}
