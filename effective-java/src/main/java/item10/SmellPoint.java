package item10;

import java.util.Objects;

public class SmellPoint extends Point {
    private int z;

    public SmellPoint(int x, int y, int z) {
        super(x, y);
        this.z = z;
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Point)) {
            return false;
        }

        if (!(o instanceof SmellPoint)) {
            return super.equals(o);
        }

        SmellPoint smellPoint = (SmellPoint) o;

        return super.equals(o) && smellPoint.z == z;
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), z);
    }
}
