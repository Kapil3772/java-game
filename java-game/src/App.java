import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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

class Player extends PhysicsEntity {
    double[] velocity;
    BufferedImage sprite;
    RenderOffset renderOffset = new RenderOffset(-50, -45, 100, 80);

    public Player(double x, double y, int w, int h) {
        super(x, y, w, h);
        this.velocity = new double[] { 100, 0 };
    }

    public void update(double dt, int[] moving) {
        prevX = xPos;
        prevY = yPos;

        xPos += velocity[0] * moving[0] * dt;
        yPos += velocity[1] * moving[1] * dt;
    }

    public void updateInterpolation(double ipf) {
        alphaX = prevX + (xPos - prevX) * ipf;
        alphaY = prevY + (yPos - prevY) * ipf;
    }

    public void render(Graphics g) {
        if (sprite != null) {
            g.drawImage(sprite, ((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y, w + renderOffset.w,
                    h + renderOffset.h, null);
        } else {
            g.setColor(Color.RED); // fallback
            g.fillRect((int) alphaX, (int) alphaY, w, h);
        }
        g.setColor(Color.BLUE);
        g.drawRect((int) alphaX, (int) alphaY, w, h);
    }
}

class App extends JFrame {
    // Class Constants
    private static final int FRAME_HEIGHT = 500;
    private static final int FRAME_WIDTH = 500;
    private static final int FPS = 60;

    private static final long GAME_LOOP_FREQUENCY = 80;
    private static final long LOOP_DURATION_NS = 1_000_000_000 / GAME_LOOP_FREQUENCY;

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

    boolean movingRight = false, movingLeft = false, movingUp = false, movingDown = false;

    JPanel panel;

    // Entities
    Player player;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.player = new Player(10, 10, 40, 80);
        // Add a custom drawing panel
        this.panel = new JPanel() {
            {
                setFocusable(true);
                requestFocusInWindow(); // important
                setupKeyBindings(this);
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // clear paper
                // Tiles, other render for future

                // Player render
                player.render(g);
            }

            void setupKeyBindings(JComponent comp) {
                InputMap im = comp.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
                ActionMap am = comp.getActionMap();

                // LEFT
                im.put(KeyStroke.getKeyStroke("pressed A"), "leftPressed");
                im.put(KeyStroke.getKeyStroke("released A"), "leftReleased");

                am.put("leftPressed", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        movingLeft = true;
                    }
                });

                am.put("leftReleased", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        movingLeft = false;
                    }
                });

                // RIGHT
                im.put(KeyStroke.getKeyStroke("pressed D"), "rightPressed");
                im.put(KeyStroke.getKeyStroke("released D"), "rightReleased");

                am.put("rightPressed", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        movingRight = true;
                    }
                });

                am.put("rightReleased", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        movingRight = false;
                    }
                });

                // JUMP space
                im.put(KeyStroke.getKeyStroke("pressed Space"), "spaceBarPressed");

                am.put("spaceBarPressed", new AbstractAction() {
                    public void actionPerformed(ActionEvent e) {
                        // jump = true;
                    }
                });
            }

        };
        add(panel);
        addMouseListener(new MouseAdapter() {
            @Override
            public void mousePressed(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    movingRight = true;
                } else {
                    movingLeft = true;
                }
            };

            @Override
            public void mouseReleased(MouseEvent e) {
                if (SwingUtilities.isRightMouseButton(e)) {
                    movingRight = false;
                } else {
                    movingLeft = false;
                }
            };
        });
        addWindowFocusListener(new WindowAdapter() {
            @Override
            public void windowLostFocus(WindowEvent e) {
                movingLeft = false;
                movingRight = false;
                // jump = false;
            }
        });

        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        Thread gameThread = new Thread(() -> run(running));
        gameThread.start();
    }

    public void loadAll() {
        player.sprite = loader.loadImage("player/player.png");

    }

    public void run(boolean isRunning) {
        long computedFrameDuration, sleepDuration;
        long millis;
        int nanos;
        loadAll();
        // long frames = 0;
        // long start = System.nanoTime();
        while (isRunning) {
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
            double deltaTime = (nowNs - lastNs) / 1000_000_000.0; // Means Previous Frame Duration
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

        int moving[] = { (movingRight ? 1 : 0) - (movingLeft ? 1 : 0), 0 };
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