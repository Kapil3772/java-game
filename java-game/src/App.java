import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

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
    double speed;
    double []velocity;

    public Player(double x, double y, int w, int h) {
        super(x, y, w, h);
        this.velocity = new double[]{100,0};
    }

    public void update(double dt, int []moving) {
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
        g.setColor(Color.RED);
        g.fillRect((int) alphaX, (int) alphaY, w, h);
    }
}

class App extends JFrame implements MouseListener {
    // Class Constants
    private static final int FRAME_HEIGHT = 500;
    private static final int FRAME_WIDTH = 500;
    private static final int FPS = 60;

    private static final double UPDATE_FREQUENCY = FPS;
    private static final double UPDATE_STEP_DURATION = 1.0 / UPDATE_FREQUENCY;
    private double frameStepAccumulator;
    private double interpolationFactor;
    private int updateCounter = 0;

    // instance variables
    private boolean running = true;
    private long lastNs = System.nanoTime();
    int framescount = 1;

    boolean movingRight = false, movingLeft = false;

    JPanel panel;
    Player player;

    public App() {
        setTitle("Game");
        setSize(FRAME_WIDTH, FRAME_HEIGHT);
        this.player = new Player(10, 10, 100, 100);
        // Add a custom drawing panel
        this.panel = new JPanel() {
            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g); // clear paper
                // Tiles, other render for future

                // Player render
                player.render(g);
            }
        };
        add(panel);
        panel.repaint();
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setVisible(true);
        addMouseListener(this);
        run(running);
    }

    public void run(boolean isRunning) {
        while (isRunning) {
            updateCounter = 0;
            long nowNs = System.nanoTime();
            double deltaTime = (nowNs - lastNs) / 1000_000_000.0;
            lastNs = nowNs;
            frameStepAccumulator += deltaTime;

            // fixed updates
            while (frameStepAccumulator >= UPDATE_STEP_DURATION) {
                update(UPDATE_STEP_DURATION);
                frameStepAccumulator -= UPDATE_STEP_DURATION;
            }

            System.out.println("Physics Updates in this frame: " + updateCounter);

            interpolationFactor = frameStepAccumulator / UPDATE_STEP_DURATION;
            updateInterpolation(interpolationFactor);

            // Render
            render();
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
        if (framescount % 60 == 0) {
            // lagSpike();
        }
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

    public void mousePressed(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            movingRight = true;
        } else {
            movingLeft = true;
        }
    };

    public void mouseReleased(MouseEvent e) {
        if (SwingUtilities.isLeftMouseButton(e)) {
            movingRight = false;
        } else {
            movingLeft = false;
        }
    };

    public void mouseClicked(MouseEvent e) {
    };

    public void mouseEntered(MouseEvent e) {
    };

    public void mouseExited(MouseEvent e) {
    };

    public static void main(String[] args) {
        new App();
    }
}
