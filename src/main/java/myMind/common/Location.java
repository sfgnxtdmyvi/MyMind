package myMind.common;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import myMind.componet.MapNode;

@AllArgsConstructor
@Data
@EqualsAndHashCode
public class Location {

    private Long subjectId;
    private double subjectTranslateX;
    private double subjectTranslateY;
    private MapNode mapNode;

}
