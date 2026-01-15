import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.IOException;


public class GameImage {
    public BufferedImage loadImage(String path) {
        try {
            var url = getClass().getResource("/" + path);
            if (url == null) {
                throw new RuntimeException("Resource not Found : " + path);
            }
            return ImageIO.read(url);

        } catch (IOException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to load image: " + path, e);
        }
    }

    public BufferedImage[] loadImagesFromFolder(String folderPath, int n) {
        BufferedImage[] images = new BufferedImage[n];
        for (int i = 0; i < n; i++) {
            images[i] = loadImage(folderPath + "/" + (int) (i + 1) + ".png");
        }
        return images;
    }

}
