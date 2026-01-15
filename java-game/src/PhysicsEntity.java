public class PhysicsEntity {
    double prevX, prevY, alphaX, alphaY;
    PhysicsRect rect; // in pixels
    int gridX, gridY;

    public PhysicsEntity(double x, double y, int w, int h) {
        this.rect = new PhysicsRect(x, y, w, h);
        this.alphaX = x;
        this.alphaY = y;
    }

    public void updateGridPos(int tileSize) {
        gridX = (int) (rect.getCenterX() / tileSize);
        gridY = (int) (rect.getCenterY() / tileSize);
    }
}

class RenderOffset {
    int x, y, w, h;

    public RenderOffset(int x, int y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
    }
}