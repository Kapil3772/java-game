import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.HashMap;

//for image and file handels
import java.awt.image.BufferedImage;
import javax.imageio.ImageIO;
import java.io.File;
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
        return (this.xPos + this.w) / 2.0;
    }

    public double getCenterY() {
        return (this.yPos + this.h) / 2.0;
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

class PhysicsEntity {
    double prevX, prevY, alphaX, alphaY;
    Rect rect; // in pixels

    public PhysicsEntity(double x, double y, int w, int h) {
        this.rect = new Rect(x, y, w, h);
        this.alphaX = x;
        this.alphaY = y;
    }
}

enum PlayerAnimState {
    IDLE,
    WALK,
    RUN
}

class Player extends PhysicsEntity {
    double speedFactor;
    double velocityX, velocityY;
    BufferedImage sprite;
    RenderOffset renderOffset = new RenderOffset(0, 0, 0, 0);
    RenderOffset animRenderOffset = new RenderOffset(0, 0, 0, 0);
    double imageScalingFactor = 1.0;
    int spriteW, spriteH;
    Animation currentAnimation;
    PlayerAnimState currAnimState;
    PlayerAnimState nextAnimState;
    boolean isMoving, facingRight;
    App game;

    public Player(App game, double x, double y, int w, int h) {
        super(x, y, w, h);
        this.velocityX = 80.0;
        this.velocityY = 0;
        this.game = game;
        this.currAnimState = PlayerAnimState.IDLE;
        this.currentAnimation = game.playerIdle;
        this.isMoving = false;
        this.speedFactor = 1.0;
        this.facingRight = true;
        this.spriteW = 48;
        this.spriteH = 32;
        this.imageScalingFactor = 1;
        this.renderOffset.w = (int) (spriteW * imageScalingFactor / 2);
        this.renderOffset.h = (int) (spriteH * imageScalingFactor / 2);
        this.renderOffset.x = (int) ((this.rect.w - (spriteW + renderOffset.w)) / 2);
        this.renderOffset.y = (int) ((this.rect.h - (spriteH + renderOffset.h)));
    }

    public void update(double dt, int[] moving) {
        prevX = rect.xPos;
        prevY = rect.yPos;

        if (moving[0] == 1) {
            this.isMoving = true;
            this.facingRight = true;
        } else if (moving[0] == -1) {
            this.isMoving = true;
            this.facingRight = false;
        } else {
            this.isMoving = false;
        }

        if (game.inputs.isSprinting) {
            speedFactor = 2.15;
        } else {
            speedFactor = 1;
        }

        rect.xPos += velocityX * speedFactor * moving[0] * dt;
        rect.yPos += velocityY * moving[1] * dt;
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

    public void updateAnimationRenderOffset(){
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
                g.drawImage(sprite, ((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y,
                        spriteW + renderOffset.w,
                        spriteH + renderOffset.h, null);

                g.setColor(Color.BLUE);
                g.drawRect(((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y, spriteW + renderOffset.w,
                        spriteH + renderOffset.h);
            } else {
                g.drawImage(sprite, ((int) alphaX) - renderOffset.x + rect.w, ((int) alphaY) + renderOffset.y,
                        -spriteW - renderOffset.w,
                        spriteH + renderOffset.h, null);
                g.setColor(Color.BLUE);
                g.drawRect(((int) alphaX) - renderOffset.x + rect.w - renderOffset.w - spriteW,
                        ((int) alphaY) + renderOffset.y,
                        spriteW + renderOffset.w, spriteH + renderOffset.h);
            }

        } else {
            System.out.println("Sprite is null " + currAnimState);
            g.setColor(Color.RED); // fallback
            g.fillRect((int) alphaX, (int) alphaY, rect.w, rect.h);
        }
        g.setColor(Color.GREEN);
        g.drawRect((int) alphaX, (int) alphaY, rect.w, rect.h);

    }
}

class InputState {
    boolean movingRight = false; // d
    boolean movingLeft = false; // a
    boolean movingUp = false; // w
    boolean movingDown = false; // s
    boolean isSprinting = false; // shift
}

class App extends JFrame {
    // Class Constants
    private static final int FRAME_HEIGHT = 500;
    private static final int FRAME_WIDTH = 500;
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

    // image variables
    private final GameImage loader = new GameImage();

    // instance variables
    private boolean running = true;
    private long lastNs = System.nanoTime();
    int framescount = 1;

    InputState inputs = new InputState();
    int []moving = {0, 0};
    JPanel panel;

    // Entities
    Player player;
    Asset assets;
    Animation playerIdle, playerWalk, playerRun;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadAll();

        this.player = new Player(this, 50, 50, 20, 48);
        
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

                // Player render
                if(player!=null){
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
                // jump = false;
            }
        });
        
        assets = new Asset();

        setVisible(true);

        Thread gameThread = new Thread(() -> run(running));
        gameThread.start();
    }

    public void loadAll() {
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

        // Player Updates
        player.update(dt, moving);

        // Other future entities updates here

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
    public void updateAnimation(){
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
        panel.repaint();
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

    public BufferedImage[] loadImages(String folderPath, int n) {
        BufferedImage[] images = new BufferedImage[n];
        for (int i = 0; i < n; i++) {
            images[i] = loadImage(folderPath + "/" + i + ".png");
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

    public Animation(String path, int[] spriteSize, int[] canvasSize, int framesCount, int animFrequency) {
        this.framesCount = framesCount;
        this.canvasW = canvasSize[0];
        this.canvasH = canvasSize[1];
        this.spriteSize = spriteSize;
        this.frameDuration = (1.0 / animFrequency);
        frames = loadAnimation(path);
    }

    public BufferedImage[] loadAnimation(String path) {
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

    public BufferedImage getCurrentFrame(double dt) {
        animationTime += dt;
        currentFrame = (int)(animationTime / frameDuration) % framesCount;
        return frames[currentFrame];
    }

    public void reset() {
        animationTime = 0;
        currentFrame = 0;
    }
}

class Asset {
    GameImage assetLoader = new GameImage();
    Map<String, BufferedImage> animations = new HashMap<>();

    public void load(String animName, String path) {
        animations.put(animName, assetLoader.loadImage(path + ".png"));
    }
}