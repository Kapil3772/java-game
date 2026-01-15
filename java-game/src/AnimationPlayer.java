import java.awt.image.BufferedImage;

class AnimationPlayer {
    Animation animation;
    double animationTime = 0;
    int currentFrame = 0;
    boolean isDone = false;

    public AnimationPlayer(Animation anim) {
        animation = anim;
    }

    public void reset() {
        animationTime = 0;
        currentFrame = 0;
        isDone = false;
    }

    public BufferedImage getCurrentFrame(double dt) {
        animationTime += dt;
        currentFrame = (int) (animationTime / animation.frameDuration) % (animation.framesCount + 1);
        if (!isDone && currentFrame == animation.framesCount) {
            isDone = true;
        }
        if (!animation.looping && isDone) {
            return animation.frames[animation.framesCount];
        } else {
            return animation.frames[currentFrame];
        }

    }
}
