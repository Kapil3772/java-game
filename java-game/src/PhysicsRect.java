public class PhysicsRect extends Rect {
    public PhysicsRect(double x, double y, int w, int h) {
        super(x, y, w, h);
    }

    // Every Collision logic is based the the perspective of the Physics Rect who is
    // calling these functions
    public boolean intersects(PhysicsRect r) {
        return this.yPos + this.h > r.yPos &&
                this.xPos < r.xPos + r.w &&
                this.yPos < r.yPos + r.h &&
                this.xPos + this.w > r.xPos;
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
