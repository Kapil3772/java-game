import java.awt.Graphics;
import java.awt.image.BufferedImage;

public class Background {
    BufferedImage[] layers;

    public Background() {
        layers = new BufferedImage[5];
        layers[4] = new GameImage().loadImage("normalBg/5.png");
    }

    public void render(Graphics g) {
        g.drawImage(layers[4], 0, 0, 1200, 800, null);
    }
}
