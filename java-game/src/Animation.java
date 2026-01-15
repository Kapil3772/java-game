import java.awt.image.BufferedImage;

public class Animation {
    int framesCount;
    int canvasW, canvasH; // pixels
    int spriteW, spriteH;
    BufferedImage[] frames;
    boolean looping = true;
    public static GameImage loader = new GameImage();
    double frameDuration;
    int currentFrame = 0;
    RenderOffset animRenderOffset = new RenderOffset(0, 0, 0, 0);

    // For loadin animation from a group of sprites/ from folder
    public Animation(String path, int framesCount, int animFrequency, int spriteW, int spriteH, boolean loop) {
        this.framesCount = framesCount;
        this.frameDuration = (1.0 / animFrequency);
        this.spriteW = spriteW;
        this.spriteH = spriteH;
        this.frames = loadAnimationFromManySprite(path, framesCount + 1);
        this.looping = loop;
    }

    // for loading animation from a single sprite sheet
    public Animation(String path, int spriteW, int spriteH, int[] canvasSize, int framesCount, int animFrequency,
            boolean loop) {
        this.framesCount = framesCount;
        this.canvasW = canvasSize[0];
        this.canvasH = canvasSize[1];
        this.spriteW = spriteW;
        this.spriteH = spriteH;
        this.frameDuration = (1.0 / animFrequency);
        frames = loadAnimationFromSingleSprite(path);
        this.looping = loop;
    }

    public static void setLoaderObject() {
        loader = new GameImage();
    }

    public void setAnimRenderOffset(int x, int y, int w, int h) {
        animRenderOffset.x = x;
        animRenderOffset.y = y;
        animRenderOffset.w = w;
        animRenderOffset.h = h;
    }

    public BufferedImage[] loadAnimationFromSingleSprite(String path) {
        BufferedImage spriteSheet = new GameImage().loadImage(path);
        BufferedImage[] bufferedImageArray = new BufferedImage[framesCount];
        int xOffset, yOffset;
        xOffset = (canvasW - spriteW) / 2;
        yOffset = (canvasH - 16 - spriteH);
        for (int i = 0; i < framesCount; i++) {
            bufferedImageArray[i] = spriteSheet.getSubimage((i * canvasW) + xOffset, 0 + yOffset, spriteH,
                    spriteH);
        }
        return bufferedImageArray;
    }

    public BufferedImage[] loadAnimationFromManySprite(String path, int imgCount) {
        BufferedImage[] imgs = null;
        imgs = loader.loadImagesFromFolder(path, imgCount);
        return imgs;
    }
}
