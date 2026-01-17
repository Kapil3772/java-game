import java.util.List;
import java.util.Random;
import java.awt.Graphics;
import java.util.ArrayList;
import java.util.Comparator;

public class CloudManager {
    List<Cloud> clouds = new ArrayList<>();
    int cloudVariantCount;
    int cloudCount;
    Random random = new Random();

    public CloudManager(int count, Camera camera, CloudVariantRegistry registry, int FRAME_WIDTH, int FRAME_HEIGHT) {
        this.cloudVariantCount = registry.cloudVariants.size();
        this.cloudCount = count;
        for (int i = 0; i < count; i++) {
            int variant = random.nextInt(cloudVariantCount) + 1;

            clouds.add(new Cloud(
                    FRAME_WIDTH * 2 * random.nextDouble(),
                    FRAME_HEIGHT * 2 * random.nextDouble(),
                    variant,
                    camera,
                    registry.get(variant)));
        }
        sortByDepth();
    }

    public void update(double dt) {
        for(Cloud cloud : clouds){
            cloud.update(dt);
        }
    }

    public void render(Graphics g){
        for(Cloud cloud : clouds){
            cloud.render(g);
        }
    }

    public void sortByDepth(){
        clouds.sort(Comparator.comparingDouble(c -> c.depth)); // I need to understand how this works
    }
    //My Bubble sorting way
    public void sort(){
        Cloud[] tempClouds = new Cloud[cloudCount];
        int i = 0;
        for(Cloud cloud : clouds){
            tempClouds[i++] = cloud;
        }
        clouds.clear();
        Cloud tempCloud;
        for(i =0; i<cloudCount; i++){
            for(int j=i+1; j<cloudCount; j++){
                if(tempClouds[j].depth < tempClouds[i].depth){ //lower the depth,  further away from camera
                    tempCloud = tempClouds[i];
                    tempClouds[i] = tempClouds[j];
                    tempClouds[j] = tempCloud;
                }
            }
        }
        for(i=0; i<cloudCount; i++){
            clouds.add(tempClouds[i]);
        }
    }

}
