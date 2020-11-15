package co.rngd.harvest.moon;

import java.util.*;
import com.badlogic.gdx.assets.*;

public class RandomMap {
  private final Random random = new Random(System.currentTimeMillis());
  private final AssetManager assetManager;

  public RandomMap(AssetManager assetManager) {
    this.assetManager = assetManager;
  }

  public int width = 128;
  public int height = 128;
  public int craters = -1;
  public int rocks = -1;
  public int crystalPercent = 30;

  private void smallCrater(GameMap map) {
    int r = random.nextInt(height + 1), c = random.nextInt(width + 1);
    int rInner = random.nextInt(7) + 3;
    int rOuter = rInner + random.nextInt(3) + 1;
    map.crater(r, c, rOuter, rInner, 1 + random.nextInt(2), - 1);
  }

  private void bigCrater(GameMap map) {
    int r = random.nextInt(height + 1), c = random.nextInt(width + 1);
    int rInner = random.nextInt(20) + 20;
    int rOuter = rInner + random.nextInt(3) + 4;
    map.crater(r, c, rOuter, rInner, 3 + random.nextInt(2), -2);
    map.crater(r, c, 5, 0, -1, -1);
  }

  private void rock(GameMap map) {
    while(true) {
      int r = random.nextInt(height), c = random.nextInt(width);
      if (!map.isFlat(r, c) || map.getObjectId(r, c) != 0) continue;
      map.setObject(r, c, random.nextInt(100) >= crystalPercent ? 1 : 2);
      return;
    }
  }

  public GameMap generate() {
    GameMap map = new GameMap(width, height, assetManager);
    int craterCount = craters >= 0 ? craters : (int) (Math.sqrt(width * height) * 0.7);
    for (int i = 0; i < craterCount / 10; i++) bigCrater(map);
    for (int i = 0; i < craterCount; i++) smallCrater(map);
    int rockCount = rocks >= 0 ? rocks : (width * height / 25);
    for (int i = 0; i < rockCount; i++) rock(map);
    map.updateControlPoints();
    map.updateObjectModels();
    return map;
  }
}
