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
    double x, y; // actual Pos after each update
    double alphaX, alphaY; // allpha means interpolated pos
    int w, h;
    double prevX, prevY; // previous Pos during recent update

    public Rect(double x, double y, int w, int h) {
        this.x = x;
        this.y = y;
        this.w = w;
        this.h = h;
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

class PhysicsEntity {
    double xPos, yPos, alphaX, alphaY, prevX, prevY;
    int w, h; // in pixels

    public PhysicsEntity(double x, double y, int w, int h) {
        this.xPos = x;
        this.yPos = y;
        this.w = w;
        this.h = h;
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
    }

    public void update(double dt, int[] moving) {
        prevX = xPos;
        prevY = yPos;

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
            speedFactor = 1.75;
        } else {
            speedFactor = 1;
        }

        xPos += velocityX * speedFactor * moving[0] * dt;
        yPos += velocityY * moving[1] * dt;
        updateAnimation();
    }

    public void updateAnimation() {
        if (isMoving && game.inputs.isSprinting) {
            this.nextAnimState = PlayerAnimState.RUN;
        } else if (isMoving) {
            this.nextAnimState = PlayerAnimState.WALK;
        } else {
            this.nextAnimState = PlayerAnimState.IDLE;
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

    public void updateInterpolation(double ipf) {
        alphaX = prevX + (xPos - prevX) * ipf;
        alphaY = prevY + (yPos - prevY) * ipf;
    }

    public void render(Graphics g) {
        if (sprite != null) {
            if (facingRight) {
                g.drawImage(sprite, ((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y,
                        w + renderOffset.w,
                        h + renderOffset.h, null);
            } else {
                g.drawImage(sprite, ((int) alphaX) + renderOffset.x + w, ((int) alphaY) + renderOffset.y,
                        -w - renderOffset.w,
                        h + renderOffset.h, null);
            }

        } else {
            System.out.println("Sprite is null " + currAnimState);
            g.setColor(Color.RED); // fallback
            g.fillRect((int) alphaX, (int) alphaY, w, h);
        }
        g.setColor(Color.BLUE);
        g.drawRect((int) alphaX, (int) alphaY, w, h);
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
    JPanel panel;

    // Entities
    Player player;
    Asset assets;
    Animation playerIdle, playerWalk, playerRun;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
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
                player.render(g);
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

        loadAll();

        this.player = new Player(this, 10, 10, 64, 64);

        Thread gameThread = new Thread(() -> run(running));
        gameThread.start();
    }

    public void loadAll() {
        // assets.load("playerIdle", "player/IDLE");
        playerIdle = new Animation("player/IDLE.png", new int[] { 32, 32 }, new int[] { 96, 96 }, 10, 10);
        // player.sprite = playerIdle.getCurrentFrame(computedFrameDuration);
        playerWalk = new Animation("player/WALK.png", new int[] { 32, 32 }, new int[] { 96, 96 }, 12, 10);

        playerRun = new Animation("player/RUN.png", new int[] { 32, 32 }, new int[] { 96, 96 }, 16, 16);
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

        int moving[] = { (inputs.movingRight ? 1 : 0) - (inputs.movingLeft ? 1 : 0), 0 };
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
    int framesCount, canvasW, canvasH;
    int[] spriteSize;
    BufferedImage[] frames;
    GameImage loader;
    double frameDuration, timer = 0;
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
        yOffset = (canvasH - spriteSize[1]) / 2;
        for (int i = 0; i < framesCount; i++) {
            bufferedImageArray[i] = spriteSheet.getSubimage((i * canvasW) + xOffset, 0 + 48, spriteSize[0],
                    spriteSize[1]);
        }
        return bufferedImageArray;
    }

    public BufferedImage getCurrentFrame(double dt) {
        timer += dt;
        if (timer >= this.frameDuration) {
            currentFrame = (currentFrame + 1) % framesCount;
            timer -= frameDuration;
        }
        return frames[currentFrame];
    }

    public void reset() {
        timer = 0;
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