import java.awt.Color;
import java.awt.Graphics;

public class Camera {
    double xPos, yPos;
    final int cameraWidth = 50, cameraHeight = 50;
    double cameraVelocity = 80;
    int frameH, frameW;
    double cameraOffsetX, cameraOffsetY;
    double cameraDestinationX, cameraDestinationY;
    Player player;
    double dt;
    double cameraCatchupTime = 1;
    double smoothness = 3;

    public Camera(Player player, int FRAME_WIDTH, int FRAME_HEIGHT, double dt) {
        xPos = player.rect.getCenterX();
        yPos = player.rect.getCenterY();
        this.player = player;
        frameH = FRAME_HEIGHT;
        frameW = FRAME_WIDTH;
        cameraOffsetX = 0;
        cameraOffsetY = 0;
        this.dt = dt;
        updateCameraOffset();
    }

    public void updateCameraOffsetU() {
        xPos = player.rect.getCenterX();
        yPos = player.rect.getCenterY();
        cameraOffsetX = -(player.rect.getCenterX() - frameW / 2);
        cameraOffsetY = -(player.rect.getCenterY() - frameH / 2);
        //double dy = cameraVelocity * dt;
    }

    public void updateCameraOffset() {
        double targetX = player.rect.getCenterX() - cameraWidth / 2;
        double targetY = player.rect.getCenterY() - cameraHeight / 2;
        double cameraPushX = 0;
        if(player.prevX < player.rect.xPos && player.game.inputs.isSprinting){
            cameraPushX = 150;
        }else if(player.prevX > player.rect.xPos && player.game.inputs.isSprinting){
            cameraPushX = -150;
        }else{
            cameraPushX = 0;
        }
        xPos += (targetX - xPos + cameraPushX) * smoothness * dt;
        yPos += (targetY - yPos) * smoothness * dt;

        cameraOffsetX = -(xPos - frameW / 2);
        cameraOffsetY = -(yPos - frameH / 2);
    }

    public void render(Graphics g) {
        g.setColor(new Color(225, 200, 100, 100));
        g.fillRect((int) (xPos + cameraOffsetX), (int) (yPos + cameraOffsetY), cameraWidth, cameraHeight);
        g.setColor(new Color(0, 0, 0, 225));
        g.drawRect((int) (xPos + cameraOffsetX), (int) (yPos + cameraOffsetY), cameraWidth, cameraHeight);
    }
}