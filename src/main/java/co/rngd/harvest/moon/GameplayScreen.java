package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameplayScreen extends ScreenAdapter {
  private final ModelBatch batch;
  private GameMap map;

  private PerspectiveCamera camera;
  private Environment environment;

  private ModelInstance mapModel;
  private ModelInstance gridModel;
  private boolean showGrid;

  public GameplayScreen(ModelBatch modelBatch) {
    this.batch = modelBatch;
  }

  public void setMap(GameMap map) {
    this.map = map;
    if (mapModel != null) mapModel.model.dispose();

    ModelBuilder modelBuilder = new ModelBuilder();
    mapModel = new ModelInstance(map.createSurfaceModel(modelBuilder));
    gridModel = new ModelInstance(map.createGridModel(modelBuilder));
  }

  @Override
  public void show() {
    camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.position.set(16f, 10f, 24f);
    camera.lookAt(10, 0, 10);
    camera.near = 1f;
    camera.far = 300f;
    camera.update();

    Gdx.input.setInputProcessor(new Controller());
    environment = new Environment();
    environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
    environment.add(new DirectionalLight().set(0.9f, 0.7f, 0.6f, -1f, -0.8f, -0.2f));
  }

  @Override
  public void resize(int width, int height) {
    if (camera == null) return;
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    camera.update();
  }

  @Override
  public void dispose() {
    if (mapModel != null) mapModel.model.dispose();
    if (gridModel != null) gridModel.model.dispose();
  }

  @Override
  public void render(float delta) {
    Gdx.gl.glViewport(0, 0, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    batch.begin(camera);
    batch.render(mapModel, environment);
    if (showGrid) batch.render(gridModel, environment);
    batch.end();
  }

  private class Controller extends InputAdapter {
    @Override
    public boolean keyTyped(char key) {
      if (key == 'g') showGrid = !showGrid;
      else return false;
      return true;
    }
  }
}
