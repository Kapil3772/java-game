import java.util.List;
import java.util.ArrayList;


public class PhysicsTilesAround {
    List<OnGridTile> tiles = new ArrayList<>();
    List<OnGridTile> debugTiles = new ArrayList<>();
    List<OnGridTile> intersectedTiles = new ArrayList<>();
    int tilesCount = 0;
    int tilesCountY, tilesCountX;
    PhysicsEntity entity;
    int tileSize;
    int array[];
    TileMap map;

    public PhysicsTilesAround(PhysicsEntity entity, TileMap map, int tileSize) {
        this.entity = entity;
        this.map = map;
        this.tileSize = tileSize;
    }

    public void updatePhysicsTilesAround() {
        tiles.clear();
        debugTiles.clear();
        int leftTile = (int) Math.floor(entity.rect.xPos / tileSize);
        int rightTile = (int) Math.floor((entity.rect.xPos + entity.rect.w - 1) / tileSize);
        int topTile = (int) Math.floor(entity.rect.yPos / tileSize);
        int bottomTile = (int) Math.floor((entity.rect.yPos + entity.rect.h - 1) / tileSize);

        // Expanding collision search area
        int startX = leftTile - 2;
        int endX = rightTile + 2;
        int startY = topTile - 2;
        int endY = bottomTile + 2;

        for (int y = startY; y <= endY; y++) {
            for (int x = startX; x <= endX; x++) {
                OnGridTile tile = map.ongridTilesMap.get(x + "," + y);
                if (tile != null) {
                    tiles.add(tile);
                    debugTiles.add(tile);
                } else {
                    debugTiles.add(new OnGridTile(null, x * tileSize, y * tileSize, tileSize, tileSize));
                }

            }
        }

    }
}
