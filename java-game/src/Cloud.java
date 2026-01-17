import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.awt.Color;

public class Cloud {
    double xPos, yPos, alphaX, alphaY, wrappedX, wrappedY;
    int w, h;
    BufferedImage img;
    Camera camera;
    double velocityX = 20; // Base speed
    double depth;
    Random random = new Random();
    double parallexFactor;

    public Cloud(double xPos, double yPos, int imgtype, Camera camera, BufferedImage img) {
        this.xPos = xPos;
        this.yPos = yPos;
        this.camera = camera;
        this.img = img;
        this.h = img.getHeight();
        this.w = img.getWidth();
        depth = random.nextDouble() * 0.6 + 0.2;
        //velocityX = (random.nextDouble() * 0.05 + 0.05) * 60;
        velocityX = velocityX * depth;
    }

    public void update(double dt) {
        xPos += velocityX * dt;
    }

    public void render(Graphics g) {
        alphaX = xPos + camera.cameraOffsetX * depth;
        alphaY = yPos + camera.cameraOffsetY * depth;
        if (alphaX >= 0) {
            // System.out.println("Cloud Going right");
            wrappedX = alphaX % (camera.viewPort.w);

        } else {
            // System.out.println("Cloud Going left");
            wrappedX = ((camera.viewPort.w) + (alphaX % camera.viewPort.w));
        }
        if (alphaY >= 0) {
            // Cloud going Down
            wrappedY = (alphaY % (camera.viewPort.h));
        } else {
            // cloud going up
            wrappedY = (camera.viewPort.h - (Math.abs(alphaY) % camera.viewPort.h));
        }
        // Actual Position of clouds in memory
        // g.setColor(Color.RED);
        // g.fillRect((int) xPos, (int) yPos, w, h);
        // Position of cloud in screen (using camera offset)
        // g.setColor(Color.blue);
        // g.fillRect((int) alphaX, (int) alphaY, w, h);
        //Position of cloud in the viewPort (wrapped position, loops in the viewport)
        g.drawImage(
                img,
                (int) (wrappedX - w),
                (int) (wrappedY - h),
                w, h, null);
    }
}

class CloudVariantRegistry {
    Map<Integer, BufferedImage> cloudVariants = new HashMap<>();

    public void register(int type, BufferedImage img) {
        cloudVariants.put(type, img);
    }

    public BufferedImage get(int type) {
        if (cloudVariants.get(type) == null) {
            System.err.println("Error : Cloud variant : " + type + " is not regestered");
            throw new RuntimeException();
        } else {
            return cloudVariants.get(type);
        }
    }
}