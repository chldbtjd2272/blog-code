package item10;

import java.util.Objects;

public class ColorPoint extends Point {

    private Color color;

    public ColorPoint(int x, int y, Color color) {
        super(x, y);
        this.color = color;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }

        if (!(o instanceof ColorPoint)) {
            return super.equals(o);
        }

        ColorPoint colorPoint = (ColorPoint) o;
        return super.equals(o) && colorPoint.color == color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), color);
    }
}
