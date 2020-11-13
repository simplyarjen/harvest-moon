package co.rngd.harvest.moon;

import java.util.*;

public class RandomMap {
  private final Random random = new Random(System.currentTimeMillis());

  public int width = 128;
  public int height = 128;
  public int baseHeight = 5;
  public int craters = -1;

  private void smallCrater(GameMap map) {
    int r = random.nextInt(height + 1), c = random.nextInt(width + 1);
    int base = map.height(r, c);
    int rInner = random.nextInt(7) + 3;
    int rOuter = rInner + random.nextInt(3) + 1;
    map.crater(r, c, rOuter, rInner, base + 2, base - 1);
  }

  private void bigCrater(GameMap map) {
    int r = random.nextInt(height + 1), c = random.nextInt(width + 1);
    int base = map.height(r, c);
    int rInner = random.nextInt(10) + 20;
    int rOuter = rInner + random.nextInt(3) + 4;
    map.crater(r, c, rOuter, rInner, base + 3, base - 2);
    map.crater(r, c, 5, 0, base -1, base -1);
  }

  public GameMap generate() {
    GameMap map = new GameMap(width, height, baseHeight);
    int craterCount = craters >= 0 ? craters : (int) Math.sqrt(width * height);
    for (int i = 0; i < craterCount / 10; i++) bigCrater(map);
    for (int i = 0; i < craterCount; i++) smallCrater(map);
    map.updateControlPoints();
    return map;
  }
}
