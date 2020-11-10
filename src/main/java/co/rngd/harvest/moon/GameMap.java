package co.rngd.harvest.moon;

import java.io.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameMap {
  public static final DataStore<GameMap> Store = new DataStore<GameMap>() {
    private static final int VERSION = 2;

    @Override
    public void writeTo(GameMap map, DataOutput output) throws IOException {
      output.writeInt(VERSION);
      output.writeInt(map.width);
      output.writeInt(map.height);
      for (int i = 0; i < map.heightMap.length; i++)
        output.writeInt(map.heightMap[i]);
    }

    @Override
    public GameMap readFrom(DataInput input) throws IOException {
      int version = input.readInt();
      if (version != VERSION) fail("GameMap version mismatch");
      int width = input.readInt(), height = input.readInt();
      GameMap result = new GameMap(width, height, 0);
      for (int i = 0; i < result.heightMap.length; i++)
        result.heightMap[i] = input.readInt();
      result.updateControlPoints();
      return result;
    }
  };

  public final int width, height;
  private final int[] heightMap;
  private final Vector3[] controlPoints;

  public GameMap(int width, int height, int baseHeight) {
    this.width = width;
    this.height = height;
    this.controlPoints = new Vector3[width * height * 5];
    for (int i = 0; i < this.controlPoints.length; i++) this.controlPoints[i] = new Vector3();
    this.heightMap = new int[(width + 1) * (height + 1)];
    for (int i = 0; i <this.heightMap.length; i++) this.heightMap[i] = baseHeight;
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

  public void updateControlPoints() {
    int index = 0;
    for (int row = 0; row < height; row++) {
      for (int column = 0; column < width; column++) {
        int hmIndex = row * (width + 1) + column;
        float h00 = heightMap[hmIndex],
              h01 = heightMap[hmIndex + 1],
              h10 = heightMap[hmIndex + width + 1],
              h11 = heightMap[hmIndex + width + 2],
              h55 = centerHeight(h00, h01, h10, h11);
        controlPoints[index++].set(row,        h00, column       );
        controlPoints[index++].set(row,        h01, column +   1f);
        controlPoints[index++].set(row +   1f, h11, column +   1f);
        controlPoints[index++].set(row +   1f, h10, column       );
        controlPoints[index++].set(row + 0.5f, h55, column + 0.5f);
      }
    }
  }

  // computes the center height, is only guaranteed to work if height differs by at most 1 within a cell
  private float centerHeight(float h00, float h01, float h10, float h11) {
    int high = 0;
    float lowValue = h00 < h01 ? h00 : h01 < h10 ? h01 : h10 < h11 ? h10 : h11;
    float highValue = h00 > h01 ? h00 : h01 > h10 ? h01 : h10 > h11 ? h10 : h11;
    if (h00 > lowValue) high += 1;
    if (h01 > lowValue) high += 1;
    if (h10 > lowValue) high += 1;
    if (h11 > lowValue) high += 1;
    if (high > 2) return highValue;
    if (high < 2) return lowValue;
    if (h00 == h11) return highValue; // diagonal ridges look best this way
    return (highValue + lowValue) * 0.5f;
  }

  public Model createSurfaceModel(ModelBuilder modelBuilder) {
    modelBuilder.begin();
    MeshPartBuilder meshBuilder = modelBuilder.part("surface", GL20.GL_TRIANGLES, Usage.Position | Usage.Normal, new Material(ColorAttribute.createDiffuse(0.7f, 0.7f, 0.8f, 1.0f)));
    for (int row = 0; row < height; row++)
      for (int column = 0; column < width; column++)
        addCell(row, column, meshBuilder);
    return modelBuilder.end();
  }

  public Model createGridModel(ModelBuilder modelBuilder) {
    modelBuilder.begin();
    MeshPartBuilder meshBuilder = modelBuilder.part("grid", GL20.GL_LINES, Usage.Position, new Material(ColorAttribute.createDiffuse(1.0f, 0.0f, 0.0f, 1.0f)));
    for (int row = 0; row < height; row++)
      for (int column = 0; column < width; column++)
        addGridCell(row, column, meshBuilder);
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
