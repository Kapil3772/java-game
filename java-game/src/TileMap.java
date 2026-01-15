import java.util.Map;
import java.util.HashMap;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.List;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.InputStream;

import com.google.gson.Gson;


class TileMap {
    Map<String, OnGridTile> ongridTilesMap;
    OnGridTile[] onGridTiles;
    int tilesCount;
    TileVariantRegistry registry;
    Camera camera;

    public TileMap(MapData map, TileVariantRegistry registry, Camera camera) {
        this.registry = registry;
        this.ongridTilesMap = new HashMap<String, OnGridTile>();
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
                // g.setColor(new Color(225, 0, 0, 225));
                // g.drawRect((int) tile.rect.xPos, (int) tile.rect.yPos, tile.rect.w,
                // tile.rect.h);
            }
        }
    }

    public void loadMapData(MapData mapData) {
        if (mapData == null)
            return;

        tilesCount = mapData.tiles.size();
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
            ongridTilesMap.put((tile.gridX + "," + tile.gridY),
                    new OnGridTile(variant, tile.gridX * mapData.tileSize, tile.gridY * mapData.tileSize,
                            mapData.tileSize, mapData.tileSize));
        }
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

// class Maploader {
// public static MapData loadMap(String path) {
// Gson gson = new Gson();
// try (FileReader reader = new FileReader(path)) {
// MapData map = gson.fromJson(reader, MapData.class);
// return map;
// } catch (IOException e) {
// e.printStackTrace();
// return null;
// }
// }
// }


class Maploader {
    public static void printJson(String resourcePath) {
        try (InputStream is = App.class.getResourceAsStream("/" + resourcePath);
                InputStreamReader isr = new InputStreamReader(is);
                BufferedReader br = new BufferedReader(isr)) {

            br.lines().forEach(System.out::println);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //
    public static MapData loadMap(String resourcePath) {
        Gson gson = new Gson();
        try (var reader = new InputStreamReader(App.class.getResourceAsStream("/" + resourcePath))) {
            return gson.fromJson(reader, MapData.class);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}

