import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

//for json reader
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.GsonBuilder;

//for image and file handels
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;

import java.io.FileReader;
import java.io.IOException;

class Rect {
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
}

class PhysicsRect extends Rect {
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
}

class OnGridTile extends PhysicsEntity {
    final TileVariant tileVariant;
    int gridX, gridY;

    public OnGridTile(TileVariant tileVariant, double x, double y, int w, int h) {
        super(x, y, w, h);
        this.tileVariant = tileVariant;
    }
}

class TileVariant {
    final String type;
    final int variant;
    final BufferedImage image;

    public TileVariant(String type, int variant, BufferedImage image) {
        this.type = type;
        this.variant = variant;
        this.image = image;
    }
}

class TileVariantRegistry {
    private final Map<String, TileVariant> tileVariants = new HashMap<>();

    private String key(String type, int variant) {
        return type + ":" + variant;
    }

    public void register(String type, int variant, BufferedImage img) {
        tileVariants.put(key(type, variant), new TileVariant(type, variant, img));
    }

    public TileVariant get(String type, int variant) {
        return tileVariants.get(key(type, variant));
    }
}

class TileMap {
    OnGridTile[] onGridTiles;
    int tilesCount;
    TileVariantRegistry registry;
    Camera camera;

    public TileMap(MapData map, TileVariantRegistry registry, Camera camera) {
        this.registry = registry;
        loadMapData(map);
        this.camera = camera;
    }

    public void render(Graphics g) {
        g.setColor(Color.BLACK);
        for (OnGridTile tile : onGridTiles) {
            if (tile != null) {
                g.drawImage(tile.tileVariant.image, (int) (tile.rect.xPos + camera.cameraOffsetX),
                        (int) (tile.rect.yPos + camera.cameraOffsetY), tile.rect.w,
                        tile.rect.h, null);
                // rendering actual position of tiles
                g.setColor(Color.BLACK);
                g.drawRect((int) tile.rect.xPos, (int) tile.rect.yPos, tile.rect.w, tile.rect.h);
            }
        }
    }

    public void loadMapData(MapData mapData) {
        if (mapData == null)
            return;

        tilesCount = mapData.tiles.size(); // Yesko meaning bujhnu xa
        onGridTiles = new OnGridTile[tilesCount];

        int i = 0;
        for (TileData tile : mapData.tiles) {
            TileVariant variant = registry.get(tile.type, tile.variant);
            if (variant == null) {
                throw new RuntimeException(
                        "TileVariant not regestered: " + tile.type + " variant " + tile.variant);
            }
            onGridTiles[i++] = new OnGridTile(variant, tile.gridX * mapData.tileSize, tile.gridY * mapData.tileSize,
                    mapData.tileSize, mapData.tileSize);
        }
    }
}

class TileData {
    String type;
    int variant;
    int gridX;
    int gridY;
}

class MapData {
    int tileSize;
    List<TileData> tiles;
}

class Maploader {
    public static MapData loadMap(String path) {
        Gson gson = new Gson();
        try (FileReader reader = new FileReader(path)) {
            MapData map = gson.fromJson(reader, MapData.class);
            return map;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }
}

class PhysicsEntity {
    double prevX, prevY, alphaX, alphaY;
    PhysicsRect rect; // in pixels

    public PhysicsEntity(double x, double y, int w, int h) {
        this.rect = new PhysicsRect(x, y, w, h);
        this.alphaX = x;
        this.alphaY = y;
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

class InputState {
    boolean movingRight = false; // d
    boolean movingLeft = false; // a
    boolean movingUp = false; // w
    boolean movingDown = false; // s
    boolean isSprinting = false; // shift
    boolean jumpPressed = false; // w
    boolean jumpHandeled = false;
}

enum PlayerAnimState {
    IDLE,
    WALK,
    RUN
}

class Player extends PhysicsEntity {
    // constants

    double speedFactor;
    double velocityX, velocityY;
    BufferedImage sprite;
    RenderOffset renderOffset = new RenderOffset(0, 0, 0, 0);
    RenderOffset animRenderOffset = new RenderOffset(0, 0, 0, 0);
    double imageScalingFactor = 1.0;
    int spriteW, spriteH;

    // Player Animation States
    Animation currentAnimation;
    PlayerAnimState currAnimState;
    PlayerAnimState nextAnimState;

    // Player states
    boolean isJumping = false;
    boolean isMoving, facingRight, isFalling;
    boolean onGround = false;
    double gravityFactor;
    double fallFactor;
    double terminalVelocity;
    int jumps = 2;
    int airTime = 0;
    App game;

    // Temporary test variables
    int counter = 0;
    double dy = 0, dyAccumulator = 0;

    public Player(App game, double x, double y, int w, int h) {
        super(x, y, w, h);
        this.velocityX = 80.0;
        this.velocityY = 0.0;
        this.game = game;
        this.currAnimState = PlayerAnimState.IDLE;
        this.currentAnimation = game.playerIdle;
        this.isMoving = false;
        this.speedFactor = 1.0;
        this.facingRight = true;
        this.spriteW = 48;
        this.spriteH = 32;
        this.imageScalingFactor = 2;
        this.renderOffset.w = (int) (spriteW * imageScalingFactor / 2);
        this.renderOffset.h = (int) (spriteH * imageScalingFactor / 2);
        this.renderOffset.x = (int) ((this.rect.w - (spriteW + renderOffset.w)) / 2);
        this.renderOffset.y = (int) ((this.rect.h - (spriteH + renderOffset.h)));
        this.gravityFactor = 1;
        this.terminalVelocity = game.TERMINAL_VELOCITY * gravityFactor;
    }

    public void jump() {
        this.jumps = Math.max(0, jumps - 1);
        this.onGround = false;
        this.isJumping = true;
        this.velocityY = -350;
    }

    public void update(double dt, int[] moving) {
        if (moving[0] == 1) {
            this.isMoving = true;
            this.facingRight = true;
        } else if (moving[0] == -1) {
            this.isMoving = true;
            this.facingRight = false;
        } else {
            this.isMoving = false;
        }

        if (game.inputs.jumpPressed && !game.inputs.jumpHandeled && jumps < 10) {
            game.inputs.jumpHandeled = true;
            jump();
        }

        if (game.inputs.isSprinting) {
            speedFactor = 2.15;
        } else {
            speedFactor = 1;
        }

        isFalling = velocityY > 0;
        fallFactor = isFalling ? 1.9 : 1.0;

        // moving in X direction
        prevX = rect.xPos;
        rect.xPos += velocityX * speedFactor * moving[0] * dt;

        // resolving X collision
        resolveCollisionX();

        // moving in y direction--
        // (velocityY * moving[1]) needs to be added to move up and down
        prevY = rect.yPos;

        // calculating displacement using initial velocity of the frame
        dy = velocityY * dt / 2;
        rect.yPos += dy;
        dyAccumulator += dy;
        resolveCollisionY();

        onGround = false;

        prevY = rect.yPos;
        // calculating displacement using final velocity of the frame
        velocityY = Math.min(velocityY + (this.game.ACCLN_DUE_TO_GRAVITY * fallFactor * gravityFactor * dt),
                terminalVelocity);
        dy = velocityY * dt / 2;
        rect.yPos += dy;
        dyAccumulator += dy;

        // resolving y collision
        resolveCollisionY();

        // Ressetting
        if (!onGround) {
            airTime++;
        } else {
            airTime = 0;
        }
    }

    public void resolveCollisionX() {
        for (OnGridTile tile : game.tileMap.onGridTiles) {
            if (this.rect.intersects(tile.rect)) {

                // moving right
                if (rect.xPos > prevX) {
                    rect.xPos = tile.rect.xPos - rect.w;
                }
                // moving left
                else if (rect.xPos < prevX) {
                    rect.xPos = tile.rect.xPos + tile.rect.w;
                }
            }
        }
    }

    public void resolveCollisionY() {
        boolean groundedThisStep = false;
        for (OnGridTile tile : game.tileMap.onGridTiles) {
            if (this.rect.intersects(tile.rect)) {
                // moving down in a tile
                if (velocityY > 0) {
                    rect.yPos = tile.rect.yPos - rect.h;
                    groundedThisStep = true;
                    this.velocityY = 0;
                    this.jumps = 2;
                }
                // moving up in a tile
                else if (velocityY < 0) {
                    rect.yPos = tile.rect.yPos + tile.rect.h;
                    this.velocityY = 0;
                }
            }
        }
        onGround = groundedThisStep;
    }

    public void updateAnimation() {
        if (isMoving && game.inputs.isSprinting) {
            this.nextAnimState = PlayerAnimState.RUN;
            animRenderOffset.x = -10;
        } else if (isMoving) {
            this.nextAnimState = PlayerAnimState.WALK;
            animRenderOffset.x = -4;
        } else {
            this.nextAnimState = PlayerAnimState.IDLE;
            animRenderOffset.x = 0;
        }
        if (nextAnimState != currAnimState) {
            currAnimState = nextAnimState;
            switch (currAnimState) {
                case RUN:
                    this.currentAnimation = game.playerRun;
                    break;
                case WALK:
                    this.currentAnimation = game.playerWalk;
                    break;
                case IDLE:
                    this.currentAnimation = game.playerIdle;
                    break;
                default:
                    break;
            }
            currentAnimation.reset();
        }
        sprite = currentAnimation.getCurrentFrame(game.deltaTime);
    }

    public void updateAnimationRenderOffset() {
        this.renderOffset.x = (int) ((this.rect.w - (spriteW + renderOffset.w)) / 2) + animRenderOffset.x;
        this.renderOffset.y = (int) ((this.rect.h - (spriteH + renderOffset.h))) + animRenderOffset.y;
    }

    public void updateInterpolation(double ipf) {
        alphaX = prevX + (rect.xPos - prevX) * ipf;
        alphaY = prevY + (rect.yPos - prevY) * ipf;
    }

    public void render(Graphics g) {
        if (sprite != null) {
            if (facingRight) {
                g.drawImage(sprite, ((int) alphaX) + renderOffset.x + (int) (game.camera.cameraOffsetX),
                        ((int) alphaY) + renderOffset.y + (int) (game.camera.cameraOffsetY),
                        spriteW + renderOffset.w,
                        spriteH + renderOffset.h, null);

                g.setColor(Color.BLUE);
                g.drawRect(((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y,
                        spriteW + renderOffset.w,
                        spriteH + renderOffset.h);

            } else {
                g.drawImage(sprite, ((int) alphaX) - renderOffset.x + rect.w + (int) (game.camera.cameraOffsetX),
                        ((int) alphaY) + renderOffset.y + (int) (game.camera.cameraOffsetY),
                        -spriteW - renderOffset.w,
                        spriteH + renderOffset.h, null);

                g.setColor(Color.BLUE);
                g.drawRect(((int) alphaX) - renderOffset.x + rect.w - renderOffset.w -
                        spriteW,
                        ((int) alphaY) + renderOffset.y,
                        spriteW + renderOffset.w, spriteH + renderOffset.h);

            }

        } else {
            // System.out.println("Sprite is null " + currAnimState);
            g.setColor(Color.RED); // fallback
            g.fillRect((int) alphaX, (int) alphaY, rect.w, rect.h);
        }
        g.setColor(Color.GREEN);
        g.drawRect((int) alphaX, (int) alphaY, rect.w, rect.h);

    }
}

class Camera {
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

class App extends JFrame {
    // Class Constants
    private static final int FRAME_HEIGHT = 700;
    private static final int FRAME_WIDTH = 700;
    private static final int FPS = 60;

    private static final long GAME_LOOP_FREQUENCY = 80;
    private static final long LOOP_DURATION_NS = 1_000_000_000 / GAME_LOOP_FREQUENCY;
    public long computedFrameDuration;

    public double deltaTime;
    private static final double UPDATE_FREQUENCY = FPS;
    private static final double UPDATE_STEP_DURATION = 1.0 / UPDATE_FREQUENCY;
    public static int updateCounter;

    private double frameStepAccumulator;
    private double interpolationFactor;

    public final double ACCLN_DUE_TO_GRAVITY = 600; // px / second square
    public final double TERMINAL_VELOCITY = 600;

    // image variables
    private final GameImage loader = new GameImage();

    // instance variables
    private boolean running = true;
    private long lastNs = System.nanoTime();
    int framescount = 1;

    InputState inputs = new InputState();
    int[] moving = { 0, 0 };
    JPanel panel;

    // Entities
    Player player;
    Camera camera;
    Asset assets;
    Animation playerIdle, playerWalk, playerRun;

    // tiles Variables
    TileVariantRegistry tileVariantRegistry = new TileVariantRegistry();
    TileMap tileMap;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadAll();

        MapData map = Maploader.loadMap("map1.json");

        // tiles
        // player
        this.player = new Player(this, 300, 50, 24, 60);

        // camera
        camera = new Camera(player, FRAME_WIDTH, FRAME_HEIGHT);
        tileMap = new TileMap(map, tileVariantRegistry, camera);

        // Add a custom drawing panel
        this.panel = new JPanel() {
            {
                setFocusable(true);
                requestFocusInWindow(); // important
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // clear paper
                // Tiles, other render for future
                tileMap.render(g);

                // Player render
                if (player != null) {
                    player.render(g);
                }

            }
        };
        add(panel);

        // Keyboard Inputs Handeling
        KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(e -> {
            boolean pressed = e.getID() == KeyEvent.KEY_PRESSED;

            switch (e.getKeyCode()) {
                case KeyEvent.VK_A -> inputs.movingLeft = pressed;
                case KeyEvent.VK_D -> inputs.movingRight = pressed;
                case KeyEvent.VK_SHIFT -> inputs.isSprinting = pressed;
                case KeyEvent.VK_W -> {
                    inputs.jumpPressed = pressed;
                    if (pressed) {
                    } else { // w is released
                        inputs.jumpHandeled = false;
                    }
                }
                case KeyEvent.VK_S -> inputs.movingDown = pressed;
            }
            return false;
        });
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    inputs.movingRight = true;
                } else {
                    inputs.movingLeft = true;
                }
            };

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    inputs.movingRight = false;
                } else {
                    inputs.movingLeft = false;
                }
            };
        });
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                inputs.movingLeft = false;
                inputs.movingRight = false;
                inputs.isSprinting = false;
                inputs.movingDown = false;
                inputs.movingUp = false;
                inputs.jumpPressed = false;
                // jump = false;
            }
        });

        assets = new Asset();

        setVisible(true);

        Thread gameThread = new Thread(() -> run(running));
        gameThread.start();
    }
    public void loadTileAssets(){
        tileVariantRegistry.register("ground", 0, loader.loadImage("tiles/ground/0.png"));
        tileVariantRegistry.register("stone", 0, loader.loadImage("tiles/stone/0.png"));
        //Grass Tiles
        for(int i = 1; i<=41; i++){
            tileVariantRegistry.register("grass", i, loader.loadImage("tiles/grass/"+i+".png"));
        }
    }

    public void loadAll() {
        // loading tiles variants
        loadTileAssets();

        // assets.load("playerIdle", "player/IDLE");
        playerIdle = new Animation("player/IDLE.png", new int[] { 48, 32 }, new int[] { 96, 96 }, 10, 10);

        playerWalk = new Animation("player/WALK.png", new int[] { 48, 32 }, new int[] { 96, 96 }, 12, 12);

        playerRun = new Animation("player/RUN.png", new int[] { 48, 32 }, new int[] { 96, 96 }, 16, 16);

    }

    public void run(boolean running) {
        long sleepDuration;
        long millis;
        int nanos;
        // long frames = 0;
        // long start = System.nanoTime();
        while (running) {
            /*
             * frames++;
             * if (System.nanoTime() - start >= 1_000_000_000L) { // 1 second
             * System.out.println("Loops per second: " + frames);
             * frames = 0;
             * start = System.nanoTime();
             * }
             */
            updateCounter = 0;
            long nowNs = System.nanoTime();
            deltaTime = (nowNs - lastNs) / 1000_000_000.0; // Means Previous Frame Duration
            lastNs = nowNs;
            frameStepAccumulator += deltaTime;

            // fixed updates
            while (frameStepAccumulator >= UPDATE_STEP_DURATION) {
                // Update
                update(UPDATE_STEP_DURATION);
                frameStepAccumulator -= UPDATE_STEP_DURATION;
            }

            // System.out.println("Physics Updates in this frame: " + updateCounter);

            interpolationFactor = frameStepAccumulator / UPDATE_STEP_DURATION;
            updateInterpolation(interpolationFactor);
            updateAnimation();

            // Render
            render();

            // Calculating Sleep Duration
            computedFrameDuration = (System.nanoTime() - nowNs); // is in ns
            if (computedFrameDuration < LOOP_DURATION_NS) {
                sleepDuration = LOOP_DURATION_NS - computedFrameDuration;
                millis = sleepDuration / 1_000_000;
                nanos = (int) (sleepDuration % 1_000_000);
                try {
                    Thread.sleep(millis, nanos);
                } catch (InterruptedException e) {
                }
            }
        }
    }

    public void update(double dt) {
        framescount++;
        updateCounter++;

        // Update Movement

        moving[0] = (inputs.movingRight ? 1 : 0) - (inputs.movingLeft ? 1 : 0);
        moving[1] = (inputs.movingDown ? 1 : 0) - (inputs.movingUp ? 1 : 0);

        // Player Updates
        player.update(dt, moving);

        // Other future entities updates here
        camera.updateCameraOffset();
        /*
         * if (framescount % 60 == 0) {
         * lagSpike();
         * }
         */
    }

    public void updateInterpolation(double ipf) {
        // interpolation for player
        player.updateInterpolation(ipf);
    }

    public void updateAnimation() {
        player.updateAnimationRenderOffset();
        player.updateAnimation();
    }

    public void lagSpike() {
        double x = 0;
        for (int i = 0; i < 20000000; i++) {
            x += Math.sin(i) * Math.cos(i);
        }
    }

    public void render() {
        SwingUtilities.invokeLater(() -> panel.repaint()); // forwards repaint to EDT instead of game thread
    }

    public static void main(String[] args) {
        new App();
    }
}

class GameImage {
    public BufferedImage loadImage(String path) {
        try {
            var url = getClass().getResource(path);
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

class Animation {
    int framesCount;
    int canvasW, canvasH; // pixels
    int[] spriteSize;
    BufferedImage[] frames;
    GameImage loader;
    double frameDuration, animationTime = 0;
    int currentFrame = 0;

    // For loadin animation from a group of sprites
    public Animation(String path, int framesCount, int animFrequency) {
        this.framesCount = framesCount;
        this.frameDuration = (1.0 / animFrequency);
        this.loader = new GameImage();
        this.frames = loadAnimationFromManySprite(path, framesCount);
    }

    // for loading animation from a single sprite sheet
    public Animation(String path, int[] spriteSize, int[] canvasSize, int framesCount, int animFrequency) {
        this.framesCount = framesCount;
        this.canvasW = canvasSize[0];
        this.canvasH = canvasSize[1];
        this.spriteSize = spriteSize;
        this.frameDuration = (1.0 / animFrequency);
        frames = loadAnimationFromSingleSprite(path);
        this.loader = new GameImage();
    }

    public BufferedImage[] loadAnimationFromSingleSprite(String path) {
        BufferedImage spriteSheet = new GameImage().loadImage(path);
        BufferedImage[] bufferedImageArray = new BufferedImage[framesCount];
        int xOffset, yOffset;
        xOffset = (canvasW - spriteSize[0]) / 2;
        yOffset = (canvasH - 16 - spriteSize[1]);
        for (int i = 0; i < framesCount; i++) {
            bufferedImageArray[i] = spriteSheet.getSubimage((i * canvasW) + xOffset, 0 + yOffset, spriteSize[0],
                    spriteSize[1]);
        }
        return bufferedImageArray;
    }

    public BufferedImage[] loadAnimationFromManySprite(String path, int imgCount) {
        BufferedImage[] imgs = null;
        imgs = loader.loadImagesFromFolder(path, imgCount);
        return imgs;
    }

    public BufferedImage getCurrentFrame(double dt) {
        animationTime += dt;
        currentFrame = (int) (animationTime / frameDuration) % framesCount;
        return frames[currentFrame];
    }

    public void reset() {
        animationTime = 0;
        currentFrame = 0;
    }
}

class Asset {
    GameImage assetLoader = new GameImage();
    Map<String, Animation> animations = new HashMap<>();

    public void loadAnimationSprite(String animName, String path) {
    }
}