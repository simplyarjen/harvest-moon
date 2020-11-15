package co.rngd.harvest.moon;

import java.io.*;
import java.util.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameMap {
  public static class GameMapDataStore implements DataStore<GameMap> {
    private static final int VERSION = 4;
    private AssetManager assetManager;

    public void setAssetManager(AssetManager value) { this.assetManager = value; }

    @Override
    public void writeTo(GameMap map, DataOutput output) throws IOException {
      output.writeInt(VERSION);
      output.writeInt(map.width);
      output.writeInt(map.height);
      for (int i = 0; i < map.heightMap.length; i++)
        output.writeInt(map.heightMap[i]);
      for (int i = 0; i < map.objectMap.length; i++)
        output.writeInt(map.objectMap[i]);
    }

    @Override
    public GameMap readFrom(DataInput input) throws IOException {
      int version = input.readInt();
      if (version != VERSION) fail("GameMap version mismatch");
      int width = input.readInt(), height = input.readInt();
      GameMap result = new GameMap(width, height, assetManager);
      for (int i = 0; i < result.heightMap.length; i++)
        result.heightMap[i] = input.readInt();
      for (int i = 0; i < result.objectMap.length; i++)
        result.objectMap[i] = input.readInt();
      result.updateControlPoints();
      result.updateObjectModels();
      return result;
    }
  };
  public static final GameMapDataStore Store = new GameMapDataStore();

  private static final String OBJECT_NAMES[] = new String[] { null, "models/rock.g3dj", "models/rock_crystals.g3dj" };

  public final int width, height;
  private final AssetManager assetManager;
  private final int[] heightMap;
  private final int[] objectMap;
  private final Vector3[] controlPoints;
  private final List<ModelInstance> objectModels;
  private int minHeight, maxHeight;

  public GameMap(int width, int height, AssetManager assetManager) {
    this.width = width;
    this.height = height;
    this.assetManager = assetManager;
    this.controlPoints = new Vector3[width * height * 5];
    for (int i = 0; i < this.controlPoints.length; i++) this.controlPoints[i] = new Vector3();
    this.heightMap = new int[(width + 1) * (height + 1)];
    this.objectMap = new int[width * height];
    this.objectModels = new ArrayList<>();
  }

  public void raise(int row, int column, int toLevel) {
    if (row < 0 || row > height) return;
    if (column < 0 || column > width) return;
    int hmIndex = row * (width + 1) + column;
    if (heightMap[hmIndex] >= toLevel) return;
    heightMap[hmIndex] = toLevel;
    raise(row - 1, column - 1, toLevel - 1);
    raise(row - 1, column    , toLevel - 1);
    raise(row - 1, column + 1, toLevel - 1);
    raise(row    , column - 1, toLevel - 1);
    raise(row    , column + 1, toLevel - 1);
    raise(row + 1, column - 1, toLevel - 1);
    raise(row + 1, column    , toLevel - 1);
    raise(row + 1, column + 1, toLevel - 1);
  }

  public void lower(int row, int column, int toLevel) {
    if (row < 0 || row > height) return;
    if (column < 0 || column > width) return;
    int hmIndex = row * (width + 1) + column;
    if (heightMap[hmIndex] <= toLevel) return;
    heightMap[hmIndex] = toLevel;
    lower(row - 1, column - 1, toLevel + 1);
    lower(row - 1, column    , toLevel + 1);
    lower(row - 1, column + 1, toLevel + 1);
    lower(row    , column - 1, toLevel + 1);
    lower(row    , column + 1, toLevel + 1);
    lower(row + 1, column - 1, toLevel + 1);
    lower(row + 1, column    , toLevel + 1);
    lower(row + 1, column + 1, toLevel + 1);
  }

  public void crater(int r, int c, int rOuter, int rInner, int peak, int floor) {
    int rOuterSq = rOuter * rOuter;
    int rInnerSq = rInner * rInner;
    for (int rr = r - rOuter; rr <= r + rOuter; rr++) {
      for (int cc = c - rOuter; cc <= c + rOuter; cc++) {
        int distSq = (rr - r) * (rr - r) + (cc - c) * (cc - c);
        if (distSq < rOuterSq) raise(rr, cc, peak);
      }
    }

    for (int rr = r - rInner; rr <= r + rInner; rr++) {
      for (int cc = c - rInner; cc <= c + rInner; cc++) {
        int distSq = (rr - r) * (rr - r) + (cc - c) * (cc - c);
        if (distSq < rInnerSq) lower(rr, cc, floor);
      }
    }
  }

  public void setObject(int row, int column, int objectId) {
    if (objectId >= OBJECT_NAMES.length) throw new IllegalArgumentException("Bad objectId: " + objectId);
    objectMap[row * width + column] = objectId;
  }

  public int getObjectId(int row, int column) {
    return objectMap[row * width + column];
  }

  public void updateControlPoints() {
    int index = 0;
    minHeight = Integer.MAX_VALUE;
    maxHeight = Integer.MIN_VALUE;
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        int hmIndex = row * (width + 1) + column;
        int   h00 = heightMap[hmIndex],
              h01 = heightMap[hmIndex + 1],
              h10 = heightMap[hmIndex + width + 1],
              h11 = heightMap[hmIndex + width + 2];
        if (h00 > maxHeight) maxHeight = h00;
        if (h01 > maxHeight) maxHeight = h01;
        if (h10 > maxHeight) maxHeight = h10;
        if (h11 > maxHeight) maxHeight = h11;
        if (h00 < minHeight) minHeight = h00;
        if (h01 < minHeight) minHeight = h01;
        if (h10 < minHeight) minHeight = h10;
        if (h11 < minHeight) minHeight = h11;
        float h55 = centerHeight(h00, h01, h10, h11);
        controlPoints[index++].set(row,        h00, column       );
        controlPoints[index++].set(row,        h01, column +   1f);
        controlPoints[index++].set(row +   1f, h11, column +   1f);
        controlPoints[index++].set(row +   1f, h10, column       );
        controlPoints[index++].set(row + 0.5f, h55, column + 0.5f);
      }
    }
  }

  // computes the center height, is only guaranteed to work if height differs by at most 1 within a cell
  private float centerHeight(int h00, int h01, int h10, int h11) {
    int high = 0;
    int lowValue = h00 < h01 ? h00 : h01 < h10 ? h01 : h10 < h11 ? h10 : h11;
    int highValue = h00 > h01 ? h00 : h01 > h10 ? h01 : h10 > h11 ? h10 : h11;
    if (h00 > lowValue) high += 1;
    if (h01 > lowValue) high += 1;
    if (h10 > lowValue) high += 1;
    if (h11 > lowValue) high += 1;
    if (high > 2) return highValue;
    if (high < 2) return lowValue;
    if (h00 == h11) return highValue; // diagonal ridges look best this way
    return (highValue + lowValue) * 0.5f;
  }

  public int height(int row, int column) {
    return heightMap[row * (width + 1) + column];
  }

  public boolean isFlat(int row, int column) {
    int index = row * (width + 1) + column;
    return heightMap[index] == heightMap[index + 1] &&
           heightMap[index] == heightMap[index + width + 1] &&
           heightMap[index] == heightMap[index + width + 2];
  }

  public void updateObjectModels() {
    for (String name : OBJECT_NAMES) {
      if (name != null && !assetManager.contains(name)) assetManager.load(name, Model.class);
    }
    objectModels.clear();
    int index = 0;
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        String objectName = OBJECT_NAMES[objectMap[index++]];
        if (objectName != null) {
          Model objectModel = assetManager.finishLoadingAsset(objectName);
          Matrix4 position = new Matrix4().setToTranslation(controlPoints[controlIndex(row, column) + 4]);
          objectModels.add(new ModelInstance(objectModel, position));
        }
      }
    }
  }

  public List<ModelInstance> getObjectModels() {
    return objectModels;
  }

  private static final float EPS = 1e-9f;
  public Vector3 intercept(Ray ray) {
    Vector3 min = new Vector3(-1, minHeight - 1, -1),
            max = new Vector3().set(width + 1, maxHeight + 1, height + 1),
            rBase = new Vector3(ray.origin),
            rDir = new Vector3(ray.direction),
            s = new Vector3(1, 1, 1);
    // normalise to positive directions
    if (rDir.x < 0) { rDir.x = -rDir.x; rBase.x = -rBase.x; float tmp = min.x; min.x = -max.x; max.x = -tmp; s.x = -s.x; }
    if (rDir.y < 0) { rDir.y = -rDir.y; rBase.y = -rBase.y; float tmp = min.y; min.y = -max.y; max.y = -tmp; s.y = -s.y; }
    if (rDir.z < 0) { rDir.z = -rDir.z; rBase.z = -rBase.z; float tmp = min.z; min.z = -max.z; max.z = -tmp; s.z = -s.z; }

    Vector3 invDir = new Vector3( 1.0f / rDir.x, 1.0f / rDir.y, 1.0f / rDir.z);

    min.sub(rBase).scl(invDir);
    max.sub(rBase).scl(invDir);
    float tMin = 0;
    if (min.x > tMin) tMin = min.x;
    if (min.y > tMin) tMin = min.y;
    if (min.z > tMin) tMin = min.z;
    float tMax = max.x;
    if (max.y < tMax) tMax = max.y;
    if (max.z < tMax) tMax = max.z;

    // march the ray segment
    float time = tMin;
    Vector3 pos = new Vector3(), tmp = new Vector3();

    while (time < tMax) {
      pos.set(rDir).scl(time).add(rBase);
      int r = (int) pos.x,
          h = (int) pos.y,
          c = (int) pos.z;
      int row = (int) (pos.x * s.x),
          ht = (int) (pos.y * s.y),
          col = (int) (pos.z * s.z);
      if (row >= 0 && row < height)
      if (col >= 0 && col < width)
      if (Math.abs(ht - height(row, col)) <= 3) {
        int index = controlIndex(row, col);
        if (Intersector.intersectRayTriangle(ray, controlPoints[index + 0], controlPoints[index + 1], controlPoints[index + 4], tmp)) return tmp;
        if (Intersector.intersectRayTriangle(ray, controlPoints[index + 1], controlPoints[index + 2], controlPoints[index + 4], tmp)) return tmp;
        if (Intersector.intersectRayTriangle(ray, controlPoints[index + 2], controlPoints[index + 3], controlPoints[index + 4], tmp)) return tmp;
        if (Intersector.intersectRayTriangle(ray, controlPoints[index + 3], controlPoints[index + 0], controlPoints[index + 4], tmp)) return tmp;
      }

      tmp.set(pos).sub(r, h, c);
      if (tmp.x < EPS) tmp.x = 1f;
      if (tmp.y < EPS) tmp.y = 1f;
      if (tmp.z < EPS) tmp.z = 1f;
      tmp.scl(invDir);
      float dt = tmp.x <= tmp.y ? (tmp.x <= tmp.z ? tmp.x : tmp.z) : (tmp.y <= tmp.z ? tmp.y : tmp.z);
      time += dt;
    }
    return null;
  }

  private static final int TILE_SIZE = 50;

  public Model createSurfaceModel(ModelBuilder modelBuilder) {
    modelBuilder.begin();
    for (int rowBase = 0; rowBase < height; rowBase += TILE_SIZE) {
      int h = rowBase + TILE_SIZE > height ? height - rowBase : TILE_SIZE;
      for (int columnBase = 0; columnBase < width; columnBase += TILE_SIZE) {
        int w = columnBase + TILE_SIZE > width ? width - columnBase : TILE_SIZE;
        MeshPartBuilder meshBuilder = modelBuilder.part(String.format("surface-%d,%d", rowBase, columnBase),
            GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(0.7f, 0.7f, 0.8f, 1.0f)));
        for (int row = 0; row < h; row++)
          for (int column = 0; column < w; column++)
            addCell(row + rowBase, column + columnBase, meshBuilder);
      }
    }
    return modelBuilder.end();
  }

  public Model createGridModel(ModelBuilder modelBuilder) {
    modelBuilder.begin();
    for (int rowBase = 0; rowBase < height; rowBase += TILE_SIZE) {
      int h = rowBase + TILE_SIZE > height ? height - rowBase : TILE_SIZE;
      for (int columnBase = 0; columnBase < width; columnBase += TILE_SIZE) {
        int w = columnBase + TILE_SIZE > width ? width - columnBase : TILE_SIZE;
        MeshPartBuilder meshBuilder = modelBuilder.part(String.format("grid-%d,%d", rowBase, columnBase),
            GL20.GL_LINES, Usage.Position, new Material(ColorAttribute.createDiffuse(0.6f, 0.5f, 0.2f, 1.0f)));
        for (int row = 0; row < h; row++)
          for (int column = 0; column < w; column++)
            addGridCell(row + rowBase, column + columnBase, meshBuilder);
      }
    }
    return modelBuilder.end();
  }

  private void addCell(int row, int column, MeshPartBuilder meshBuilder) {
    int index = controlIndex(row, column);
    triangle(controlPoints[index + 0], controlPoints[index + 1], controlPoints[index + 4], meshBuilder);
    triangle(controlPoints[index + 1], controlPoints[index + 2], controlPoints[index + 4], meshBuilder);
    triangle(controlPoints[index + 2], controlPoints[index + 3], controlPoints[index + 4], meshBuilder);
    triangle(controlPoints[index + 3], controlPoints[index + 0], controlPoints[index + 4], meshBuilder);
  }

  private void addGridCell(int row, int column, MeshPartBuilder meshBuilder) {
    int index = controlIndex(row, column);
    meshBuilder.line(controlPoints[index + 0], controlPoints[index + 1]);
    meshBuilder.line(controlPoints[index + 1], controlPoints[index + 2]);
    meshBuilder.line(controlPoints[index + 2], controlPoints[index + 3]);
    meshBuilder.line(controlPoints[index + 3], controlPoints[index + 0]);
  }

  private int controlIndex(int row, int column) { return 5 * (row * width + column); }

  private void triangle(Vector3 a, Vector3 b, Vector3 c, MeshPartBuilder meshBuilder) {
    Vector3 normal = new Vector3().set(a).sub(c), aux = new Vector3().set(b).sub(c);
    normal.crs(aux).nor();
    meshBuilder.triangle(
        meshBuilder.vertex(a, normal, null, null),
        meshBuilder.vertex(b, normal, null, null),
        meshBuilder.vertex(c, normal, null, null));
  }
}
