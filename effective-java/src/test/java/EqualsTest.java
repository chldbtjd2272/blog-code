import item10.Color;
import item10.ColorPoint;
import item10.SmellPoint;
import org.junit.Test;

public class EqualsTest {

    @Test
    public void name() {
        //given
        ColorPoint colorPoint = new ColorPoint(1, 2, Color.RED);
        SmellPoint smellPoint = new SmellPoint(1, 2, 3);
        //when
        smellPoint.equals(colorPoint);
        //then
    }
}
