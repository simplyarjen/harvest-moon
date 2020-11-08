package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameMap {
  public final int width, height;

  public GameMap(int width, int height) {
    this.width = width;
    this.height = height;
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
    Vector3 vertex[] = controlPoints(row, column);
    triangle(vertex[0], vertex[1], vertex[4], meshBuilder);
    triangle(vertex[1], vertex[2], vertex[4], meshBuilder);
    triangle(vertex[2], vertex[3], vertex[4], meshBuilder);
    triangle(vertex[3], vertex[0], vertex[4], meshBuilder);
  }

  private void addGridCell(int row, int column, MeshPartBuilder meshBuilder) {
    Vector3 vertex[] = controlPoints(row, column);
    meshBuilder.line(vertex[0], vertex[1]);
    meshBuilder.line(vertex[1], vertex[2]);
    meshBuilder.line(vertex[2], vertex[3]);
    meshBuilder.line(vertex[3], vertex[0]);
  }

  private Vector3[] controlPoints(int row, int column) {
    Vector3 vertex[] = new Vector3[5];
    vertex[0] = new Vector3(row,        0f, column       );
    vertex[1] = new Vector3(row,        0f, column +   1f);
    vertex[2] = new Vector3(row +   1f, 0f, column +   1f);
    vertex[3] = new Vector3(row +   1f, 0f, column       );
    vertex[4] = new Vector3(row + 0.5f, 0f, column + 0.5f);
    return vertex;
  }

  private void triangle(Vector3 a, Vector3 b, Vector3 c, MeshPartBuilder meshBuilder) {
    Vector3 normal = new Vector3().set(a).sub(c), aux = new Vector3().set(b).sub(c);
    normal.crs(aux).nor();
    meshBuilder.triangle(
        meshBuilder.vertex(a, normal, null, null),
        meshBuilder.vertex(b, normal, null, null),
        meshBuilder.vertex(c, normal, null, null));
  }
}
