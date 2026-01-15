import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Map;
import java.util.HashMap;
import java.util.List;

//for json reader
import com.google.gson.Gson;

//for image and file handels
import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

//local imports


class OnGridTile extends PhysicsEntity {
    final TileVariant tileVariant;
    int gridX, gridY;

    public OnGridTile(TileVariant tileVariant, double x, double y, int w, int h) {
        super(x, y, w, h);
        this.tileVariant = tileVariant;
    }
}

class InputState {
    boolean movingRight = false; // d
    boolean movingLeft = false; // a
    boolean movingUp = false; // w
    boolean movingDown = false; // s
    boolean isSprinting = false; // shift
    boolean jumpPressed = false; // w
}

enum PlayerAnimState {
    IDLE,
    WALK,
    RUN,
    JUMP_START,
    JUMP_FALL,
    JUMP_TRANSITION,
    WALL_SLIDE,
    WALL_CONTACT,
    WALL_JUMP,
    WALL_CLIMB
}

enum WallState {
    NONE,
    HOLDING,
    SLIDING,
    CLIMBING
}

public class App extends JFrame {
    // Class Constants
    private static final int FRAME_WIDTH = 1080;
    private static final int FRAME_HEIGHT = 800;
    private static final int FPS = 60;

    private static final long GAME_LOOP_FREQUENCY = 90;
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
    Player player, player2;
    Camera camera;
    Asset assets;
    Animation playerIdle, playerWalk, playerRun, playerJumpStart, playerJumpFall, playerJumpTransition,
            playerWallContact, playerWallJump, playerWallSlide, playerWallClimb;

    // tiles Variables
    TileVariantRegistry tileVariantRegistry = new TileVariantRegistry();
    TileMap tileMap;
    MapData map;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        loadAll();

        map = Maploader.loadMap("map1.json");

        // tiles
        // player
        player = new Player(this, 300, 50, 30, 90);
        player2 = new Player(this, 300, 40, 30, 90);

        // camera
        camera = new Camera(player, FRAME_WIDTH, FRAME_HEIGHT);
        tileMap = new TileMap(map, tileVariantRegistry, camera);
        player.physicsTilesAround = new PhysicsTilesAround(player, tileMap, 32);
        player2.physicsTilesAround = new PhysicsTilesAround(player2, tileMap, 32);

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
                    player2.render(g);
                }
                Font font = new Font("Arial", Font.BOLD, 20); // 24 is the font size
                g.setFont(font);
                g.setColor(Color.BLACK);
                g.drawString("Game Version: 1.0.1", 30, 30);
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
                    if (!inputs.jumpPressed && pressed) {
                        player.jumpTriggered = true;
                        player2.jumpTriggered = true;
                    }
                    inputs.jumpPressed = pressed;
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

    public void loadTileAssets() {
        // Misellaneous tiles
        tileVariantRegistry.register("ground", 0, loader.loadImage("tiles/ground/0.png"));
        tileVariantRegistry.register("stone", 0, loader.loadImage("tiles/stone/0.png"));

        // Grass Tiles
        for (int i = 1; i <= 41; i++) {
            tileVariantRegistry.register("grass", i, loader.loadImage("tiles/grass/" + i + ".png"));
        }

    }

    public void loadAll() {
        // loading tiles variants
        loadTileAssets();

        // frames count is one less for now, later i will change. This is because
        // loadimages has (i+1) instead of i as tileset images starts from 1.png
        playerIdle = new Animation("player/idle", 9, 10, 32, 32, true);
        playerIdle.setAnimRenderOffset(0, 0, 0, 0);

        playerWalk = new Animation("player/walk", 11, 12, 32, 32, true);
        playerWalk.setAnimRenderOffset(0, 0, 0, 0);

        playerRun = new Animation("player/run", 15, 16, 32, 32, true);
        playerRun.setAnimRenderOffset(0, 0, 0, 0);

        playerJumpStart = new Animation("player/jumpStart", 2, 10, 32, 32, true);
        playerJumpFall = new Animation("player/jumpFall", 2, 10, 32, 32, true);
        playerJumpTransition = new Animation("player/jumpTransition", 2, 4, 32, 32, false);
        playerWallContact = new Animation("player/wallContact", 2, 8, 32, 32, false);
        playerWallContact.setAnimRenderOffset(-3, 0, 0, 0);
        playerWallSlide = new Animation("player/wallSlide", 2, 10, 32, 32, true);
        playerWallSlide.setAnimRenderOffset(-3, 0, 0, 0);
        playerWallClimb = new Animation("player/wallClimb", 7, 8, 32, 32, false);
        playerWallClimb.setAnimRenderOffset(0, 2, 0, 0);
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
            updateAnimation(UPDATE_STEP_DURATION);

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

        player.update(dt, moving);
        player2.update(dt, moving);

        // Player Updates

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
        player2.updateInterpolation(ipf);
    }

    public void updateAnimation(double dt) {
        player.updateAnimationRenderOffset();
        player.updateAnimation(dt);
        player2.updateAnimationRenderOffset();
        player2.updateAnimation(dt);
    }

    public void lagSpike() {
        double x = 0;
        for (int i = 0; i < 20000000; i++) {
            x += Math.sin(i) * Math.cos(i);
        }
        System.out.println(x);
    }

    public void render() {
        SwingUtilities.invokeLater(() -> panel.repaint()); // forwards repaint to EDT instead of game thread
    }

    public static void main(String[] args) {
        new App();
    }
}
class Asset {
    GameImage assetLoader = new GameImage();
    Map<String, Animation> animations = new HashMap<>();

    public void loadAnimationSprite(String animName, String path) {
    }
}