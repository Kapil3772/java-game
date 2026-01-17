import java.awt.image.BufferedImage;
import java.util.EnumMap;
import java.util.Map;
import java.awt.*;

public class Player extends PhysicsEntity {
    // constants

    double speedFactor;
    double velocityX, velocityY;
    final double jumpTransitionVelocity;
    BufferedImage sprite = null;
    RenderOffset renderOffset = new RenderOffset(0, 0, 0, 0);
    RenderOffset animRenderOffset = new RenderOffset(0, 0, 0, 0);
    double imageScalingFactor = 1.0;
    int spriteW, spriteH;
    PhysicsTilesAround physicsTilesAround;
    OnGridTile topTile = null;
    double topMostTileY;

    // Player Animation States
    Animation currentAnimation;
    Map<PlayerAnimState, AnimationPlayer> animPlayerMap = new EnumMap<PlayerAnimState, AnimationPlayer>(
            PlayerAnimState.class);
    AnimationPlayer currAnimationPlayer;
    PlayerAnimState currAnimState;
    PlayerAnimState nextAnimState;
    boolean lockAnimationThisFrame = false;

    // Player states
    boolean blockingInput = false;
    WallState wallState = WallState.NONE;
    boolean isJumping = false, jumpHandeled = false;
    boolean isMoving, facingRight, isFalling;
    boolean onGround = false, onAir = false, onJumpTransition = false;
    boolean isTouchingSideWall = false;
    double holdingWallTimer = 0.0; // seconds before the player starts sliding downwards on the wall
    boolean climbHandeled = true, isClimbing = false;
    double climbTimer = 0.0;
    final double gravityFactor;
    double fallFactor, frictionalFactor = 1;
    final double terminalVelocity;

    // One time triggers
    boolean jumpTriggered = false;

    int remainingJumps = 2;
    int airTimeFrames = 0;
    App game;

    // Temporary test variables
    int counter = 0;
    double dy = 0, dyAccumulator = 0;

    public Player(App game, double x, double y, int w, int h) {
        super(x, y, w, h);
        this.velocityY = 0.0;
        this.game = game;
        this.currAnimState = PlayerAnimState.IDLE;
        if (animPlayerMap.isEmpty()) {
            setAnimationPlayerMap();
        }
        this.currAnimationPlayer = animPlayerMap.get(PlayerAnimState.IDLE);
        this.isMoving = false;
        this.speedFactor = 1.0;
        this.facingRight = true;
        this.spriteW = 32;
        this.spriteH = 32;
        this.imageScalingFactor = 2;
        sprite = currAnimationPlayer.getCurrentFrame(game.deltaTime);
        updateAnimationRenderOffset();
        // this.renderOffset.x = (int) ((this.rect.w - (spriteW + renderOffset.w)) / 2);
        // this.renderOffset.y = (int) ((this.rect.h - (spriteH + renderOffset.h)));
        this.gravityFactor = 1;
        this.terminalVelocity = game.TERMINAL_VELOCITY * gravityFactor;
        this.velocityX = 67 * imageScalingFactor;
        this.jumpTransitionVelocity = terminalVelocity * 0.3;
    }

    public void setAnimationPlayerMap() {
        animPlayerMap.put(PlayerAnimState.IDLE, new AnimationPlayer(game.playerIdle));
        animPlayerMap.put(PlayerAnimState.RUN, new AnimationPlayer(game.playerRun));
        animPlayerMap.put(PlayerAnimState.WALK, new AnimationPlayer(game.playerWalk));
        animPlayerMap.put(PlayerAnimState.WALL_CLIMB, new AnimationPlayer(game.playerWallClimb));
        animPlayerMap.put(PlayerAnimState.WALL_CONTACT, new AnimationPlayer(game.playerWallContact));
        animPlayerMap.put(PlayerAnimState.WALL_JUMP, new AnimationPlayer(game.playerWallJump));
        animPlayerMap.put(PlayerAnimState.WALL_SLIDE, new AnimationPlayer(game.playerWallSlide));
        animPlayerMap.put(PlayerAnimState.JUMP_START, new AnimationPlayer(game.playerJumpStart));
        animPlayerMap.put(PlayerAnimState.JUMP_FALL, new AnimationPlayer(game.playerJumpFall));
        animPlayerMap.put(PlayerAnimState.JUMP_TRANSITION, new AnimationPlayer(game.playerJumpTransition));
    }

    boolean canWallClimb() {
        if (!isTouchingSideWall || onGround)
            return false;
        double topMostTileYpos = Double.MAX_VALUE;
        for (OnGridTile tile : physicsTilesAround.tiles) {
            if (tile == null || tile.tileVariant == null)
                continue;

            if (facingRight) {
                if (tile.rect.xPos > this.rect.xPos && tile.rect.yPos < topMostTileYpos) {
                    topMostTileYpos = tile.rect.yPos;
                }
            } else {
                if (tile.rect.xPos < this.rect.xPos && tile.rect.yPos < topMostTileYpos) {
                    topMostTileYpos = tile.rect.yPos;
                }
            }

        }
        if (isTouchingSideWall && (this.rect.yPos <= topMostTileYpos + 32 && this.rect.yPos >= topMostTileYpos + 16)) {
            return true;
        } else
            return false;
    }

    boolean canWallInteract() {
        if (onGround || !isTouchingSideWall) {
            return false;
        }
        double topMostTileYpos = Double.MAX_VALUE;
        for (OnGridTile tile : physicsTilesAround.tiles) {
            if (tile == null || tile.tileVariant == null)
                continue;

            if (facingRight) {
                if (tile.rect.xPos > this.rect.xPos && tile.rect.yPos < topMostTileYpos) {
                    topMostTileYpos = tile.rect.yPos;
                }
            } else {
                if (tile.rect.xPos < this.rect.xPos && tile.rect.yPos < topMostTileYpos) {
                    topMostTileYpos = tile.rect.yPos;
                }
            }
        }
        if (topMostTileYpos <= this.rect.yPos - 32) {
            return true;
        } else {
            return false;
        }
    }

    public void jump() {
        jumpTriggered = false;
        this.remainingJumps = Math.max(0, remainingJumps - 1);
        this.onGround = false;
        this.isJumping = true;
        this.velocityY = -350;
        this.jumpHandeled = false;
    }

    public void update(double dt, int[] moving) {
        if (!blockingInput) {
            if (moving[0] == 1) {
                this.isMoving = true;
                this.facingRight = true;
            } else if (moving[0] == -1) {
                this.isMoving = true;
                this.facingRight = false;
            } else {
                this.isMoving = false;
            }
            if (remainingJumps == 0) {
                jumpTriggered = false;
            }
            if (jumpTriggered && !this.jumpHandeled && remainingJumps != 0) {
                this.jumpHandeled = true;
                jump();
            }

            if (game.inputs.isSprinting) {
                speedFactor = 2.15;
            } else {
                speedFactor = 1;
            }
        } else {
            speedFactor = 0;
            jumpTriggered = false;
        }

        if (Math.abs(velocityY) < jumpTransitionVelocity && (wallState == WallState.NONE)) {
            onJumpTransition = true;
        } else {
            onJumpTransition = false;
        }
        if (!onJumpTransition) {
            isJumping = velocityY < 0;
            isFalling = velocityY > 0;
        } else {
            isJumping = false;
            isFalling = false;
        }
        fallFactor = isFalling ? 1.9 : 1.0;
        updateGridPos(32);
        physicsTilesAround.updatePhysicsTilesAround();

        // System.out.println(" IsJumping :" +isJumping +" IsFalling :" +isFalling +
        // "Transition :" + onJumpTransition+ "," + velocityY);
        // moving in X direction
        prevX = rect.xPos;
        rect.xPos += velocityX * speedFactor * moving[0] * dt;

        // resolving X collision
        isTouchingSideWall = false;
        resolveCollisionX();

        // moving in y direction--
        // (velocityY * moving[1]) needs to be added to move up and down
        prevY = rect.yPos;
        if (wallState.equals(WallState.CLIMBING)) {
            velocityY = 0;
        }
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
        if (wallState.equals(WallState.CLIMBING)) {
            velocityY = 0;
        }
        dy = velocityY * dt / 2;
        rect.yPos += dy;
        dyAccumulator += dy;

        // resolving y collision
        resolveCollisionY();
        if (canWallClimb() && wallState != WallState.CLIMBING) {
            isClimbing = true;
            topMostTileY = Double.MAX_VALUE;
            // finding toptile once only
            for (OnGridTile tile : physicsTilesAround.tiles) {
                if (tile == null || tile.tileVariant == null) {
                    continue;
                }
                if (facingRight && tile.rect.xPos > rect.xPos && tile.rect.yPos < topMostTileY) {
                    topMostTileY = tile.rect.yPos;
                    topTile = tile;
                } else if (!facingRight && tile.rect.xPos < rect.xPos && tile.rect.yPos < topMostTileY) {
                    topMostTileY = tile.rect.yPos;
                    topTile = tile;
                }
            }
        }
        if (isClimbing) {
            blockingInput = true;
            wallState = WallState.CLIMBING;
            climbTimer += dt;
            // wall climb logic
            if (topTile != null && climbTimer >= 1) {
                rect.yPos = topMostTileY - rect.h;
                if (facingRight) {
                    rect.xPos = topTile.rect.left() + 10;
                } else {
                    rect.xPos = topTile.rect.right() - 10;
                }
                wallState = WallState.NONE;
                isTouchingSideWall = false;
                onGround = true;
                onAir = false;
                isClimbing = false;
                blockingInput = false;
                climbTimer = 0;
                currAnimState = PlayerAnimState.IDLE;
                currAnimationPlayer = animPlayerMap.get(currAnimState);
                currAnimationPlayer.reset();
                sprite = currAnimationPlayer.getCurrentFrame(dt);
                lockAnimationThisFrame = false;
                return;
            }

        } else if (canWallInteract()) {
            if (wallState == WallState.NONE) {
                wallState = WallState.HOLDING;
                holdingWallTimer = 0;
                climbTimer = 0;
            }

            holdingWallTimer += dt;

            if (holdingWallTimer < 0.5) {
                // System.out.println("Hold");

                // HOLD
                velocityY = 0;
            } else {
                // SLIDE63
                // System.out.println("Slide");
                wallState = WallState.SLIDING;
                velocityY = Math.min(velocityY, 80); // slide speed
            }

        } else {
            // reset
            wallState = WallState.NONE;
            holdingWallTimer = 0;
            climbTimer = 0;
        }

        // Ressetting
        if (!onGround) {
            airTimeFrames++;
            onAir = true;
        } else {
            airTimeFrames = 0;
            onAir = false;
        }
        updateGridPos(32);
    }

    public void resolveCollisionX() {
        // for debug only
        physicsTilesAround.intersectedTiles.clear();
        for (OnGridTile tile : physicsTilesAround.tiles) {
            if (tile != null) {
                if (this.rect.intersects(tile.rect)) {
                    physicsTilesAround.intersectedTiles.add(tile);
                    // moving right
                    if (rect.xPos > prevX) {
                        rect.xPos = tile.rect.xPos - rect.w;
                        isTouchingSideWall = true;
                    }
                    // moving left
                    else if (rect.xPos < prevX) {
                        rect.xPos = tile.rect.xPos + tile.rect.w;
                        isTouchingSideWall = true;
                    }
                }
            }

        }
    }

    public void resolveCollisionY() {
        boolean groundedThisStep = false;
        for (OnGridTile tile : physicsTilesAround.tiles) {
            if (tile != null) {
                if (this.rect.intersects(tile.rect)) {
                    physicsTilesAround.intersectedTiles.add(tile);
                    // moving down in a tile
                    if (velocityY > 0) {
                        rect.yPos = tile.rect.yPos - rect.h;
                        groundedThisStep = true;
                        this.velocityY = 0;
                        this.remainingJumps = 100;
                    }
                    // moving up in a tile
                    else if (velocityY < 0) {
                        rect.yPos = tile.rect.yPos + tile.rect.h;
                        this.velocityY = 0;
                    }
                }
            }
        }
        onGround = groundedThisStep;
    }

    public void updateAnimation(double dt) {
        if (lockAnimationThisFrame) {
            currAnimationPlayer = animPlayerMap.get(currAnimState);
            currAnimationPlayer.reset();
            sprite = currAnimationPlayer.getCurrentFrame(dt);
            lockAnimationThisFrame = false;
            return;
        }
        if (!wallState.equals(WallState.NONE)) {
            if (wallState.equals(WallState.CLIMBING)) {
                this.nextAnimState = PlayerAnimState.WALL_CLIMB;
            } else if (wallState.equals(WallState.HOLDING)) {
                this.nextAnimState = PlayerAnimState.WALL_CONTACT;
            } else if (wallState.equals(WallState.SLIDING)) {
                this.nextAnimState = PlayerAnimState.WALL_SLIDE;
            }
        } else if (onAir) {
            if (onJumpTransition) {
                this.nextAnimState = PlayerAnimState.JUMP_TRANSITION;
            } else if (isJumping) {
                this.nextAnimState = PlayerAnimState.JUMP_START;
            } else {
                this.nextAnimState = PlayerAnimState.JUMP_FALL;
            }
        } else if (isMoving && game.inputs.isSprinting) {
            this.nextAnimState = PlayerAnimState.RUN;
        } else if (isMoving) {
            this.nextAnimState = PlayerAnimState.WALK;
        } else {
            this.nextAnimState = PlayerAnimState.IDLE;
        }
        if (nextAnimState != currAnimState) {
            currAnimState = nextAnimState;
            currAnimationPlayer = animPlayerMap.get(currAnimState);
            currAnimationPlayer.reset();
        }
        sprite = currAnimationPlayer.getCurrentFrame(dt);
    }

    public void updateAnimationRenderOffset() {
        this.animRenderOffset = currAnimationPlayer.animation.animRenderOffset;

        this.renderOffset.x = (int) ((this.rect.w - (sprite.getWidth() + renderOffset.w)) / 2) + animRenderOffset.x;
        this.renderOffset.y = (int) ((this.rect.h - (sprite.getHeight() + renderOffset.h))) + animRenderOffset.y;
        this.renderOffset.w = (int) (sprite.getWidth() * imageScalingFactor) + animRenderOffset.w;
        this.renderOffset.h = (int) (sprite.getHeight() * imageScalingFactor) + animRenderOffset.h;
    }

    public void updateInterpolation(double ipf) {
        if (lockAnimationThisFrame) {
            return;
        }
        alphaX = prevX + (rect.xPos - prevX) * ipf;
        alphaY = prevY + (rect.yPos - prevY) * ipf;
    }

    public void render(Graphics g) {
        if (sprite != null) {
            if (facingRight) {
                g.drawImage(sprite, ((int) alphaX) + renderOffset.x + (int) (game.camera.cameraOffsetX),
                        ((int) alphaY) + renderOffset.y + (int) (game.camera.cameraOffsetY),
                        sprite.getWidth() + renderOffset.w,
                        sprite.getHeight() + renderOffset.h, null);

                // g.setColor(Color.BLUE);
                // g.drawRect(((int) alphaX) + renderOffset.x, ((int) alphaY) + renderOffset.y,
                // sprite.getWidth() + renderOffset.w,
                // sprite.getHeight() + renderOffset.h);

            } else {
                g.drawImage(sprite, ((int) alphaX) - renderOffset.x + rect.w + (int) (game.camera.cameraOffsetX),
                        ((int) alphaY) + renderOffset.y + (int) (game.camera.cameraOffsetY),
                        -sprite.getWidth() - renderOffset.w,
                        sprite.getHeight() + renderOffset.h, null);

                // g.setColor(Color.BLUE);
                // g.drawRect(((int) alphaX) - renderOffset.x + rect.w - renderOffset.w -
                // sprite.getWidth(),
                // ((int) alphaY) + renderOffset.y,
                // sprite.getWidth() + renderOffset.w, sprite.getHeight() + renderOffset.h);

            }
            // g.setColor(new Color(225, 225, 0, 100));
            // for (OnGridTile tile : new ArrayList<>(physicsTilesAround.tiles)) {
            // g.fillRect((int) tile.rect.xPos, (int) tile.rect.yPos, tile.rect.w,
            // tile.rect.h);
            // }
            // g.setColor(Color.BLACK);
            // for (OnGridTile tile : new ArrayList<>(physicsTilesAround.debugTiles)) {
            // g.drawRect((int) tile.rect.xPos, (int) tile.rect.yPos, tile.rect.w,
            // tile.rect.h);
            // }
            // g.setColor(new Color(0, 225, 0, 190));
            // for (OnGridTile tile : new ArrayList<>(physicsTilesAround.intersectedTiles))
            // {
            // g.fillRect((int) tile.rect.xPos, (int) tile.rect.yPos, tile.rect.w,
            // tile.rect.h);
            // }

        } else {
            // System.out.println("Sprite is null " + currAnimState);
            g.setColor(Color.RED); // fallback
            g.fillRect((int) alphaX, (int) alphaY, rect.w, rect.h);
        }
        // g.setColor(new Color(0, 0, 0, 50));
        // g.fillRect((int) alphaX, (int) alphaY, rect.w, rect.h);

    }
}
