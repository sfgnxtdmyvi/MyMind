package myMind.common.manager;

import lombok.AllArgsConstructor;
import lombok.Data;
import myMind.componet.MindNode;

@Data
@AllArgsConstructor
public class Quote {
    private int subjectIndex;
    private MindNode mindNode;
}
