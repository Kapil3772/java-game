public class Camera {
    double xPos, yPos, alphaX, alphaY;
    int frameH, frameW;
    double cameraOffsetX, cameraOffsetY;
    Player player;

    public Camera(Player player, int FRAME_WIDTH, int FRAME_HEIGHT) {
        xPos = player.rect.getCenterX();
        yPos = player.rect.getCenterY();
        this.player = player;
        frameH = FRAME_HEIGHT;
        frameW = FRAME_WIDTH;
        alphaX = 0;
        alphaY = 0;
        cameraOffsetX = 0;
        cameraOffsetY = 0;
        updateCameraOffset();
    }

    public void updateCameraOffset() {
        cameraOffsetX = -(player.rect.getCenterX() - frameW / 2);
        cameraOffsetY = -(player.rect.getCenterY() - frameH / 2);
    }
}