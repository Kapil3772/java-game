public class Rect {
    double xPos, yPos;
    int w, h;

    public Rect(double x, double y, int w, int h) {
        this.xPos = x;
        this.yPos = y;
        this.w = w;
        this.h = h;
    }

    public double getCenterX() {
        return this.xPos + this.w / 2.0;
    }

    public double getCenterY() {
        return this.yPos + this.h / 2.0;
    }
    public double top() {
        return yPos;
    }

    public double bottom() {
        return yPos + h;
    }

    public double left() {
        return xPos;
    }

    public double right() {
        return xPos + w;
    }
}
