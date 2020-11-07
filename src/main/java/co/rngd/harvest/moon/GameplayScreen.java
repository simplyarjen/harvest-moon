package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameplayScreen extends ScreenAdapter {
  private final ModelBatch modelBatch;
  private final SpriteBatch spriteBatch;

  private GameMap map;

  private PerspectiveCamera camera;
  private Environment environment;

  private ModelInstance mapModel;
  private ModelInstance gridModel;
  private boolean showGrid;

  private Texture pauseButton;
  private boolean pauseMode;
  private Texture metalWindow;
  private BitmapFont messageFont;

  public GameplayScreen(ModelBatch modelBatch, SpriteBatch spriteBatch) {
    this.modelBatch = modelBatch;
    this.spriteBatch = spriteBatch;
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

    pauseButton = new Texture("pause.png");
    metalWindow = new Texture("metalPanel.png");
    messageFont = new BitmapFont(true);
  }

  @Override
  public void resize(int width, int height) {
    if (camera == null) return;
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    camera.update();
    spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, height, width, -height));
  }

  @Override
  public void dispose() {
    if (mapModel != null) mapModel.model.dispose();
    if (gridModel != null) gridModel.model.dispose();
    pauseButton.dispose();
    messageFont.dispose();
  }

  @Override
  public void render(float delta) {
    int width = Gdx.graphics.getWidth();
    int height = Gdx.graphics.getHeight();
    Gdx.gl.glViewport(0, 0, width, height);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    modelBatch.begin(camera);
    modelBatch.render(mapModel, environment);
    if (showGrid) modelBatch.render(gridModel, environment);
    modelBatch.end();

    spriteBatch.begin();
    spriteBatch.draw(pauseButton, width - 50, 10, 40, 40); 
    if (pauseMode) {
      NinePatch patch = new NinePatch(metalWindow, 10, 10, 10, 10);
      patch.draw(spriteBatch, (width - 200) / 2, (height - 200) / 2, 200, 200);
      messageFont.draw(spriteBatch, "space to resume", (width - 180) / 2, (height + 100) / 2);
      messageFont.draw(spriteBatch, "q to quit", (width - 180) / 2, (height + 140) / 2);
    }
    spriteBatch.end();
  }

  private class Controller extends InputAdapter {
    @Override
    public boolean keyTyped(char key) {
      if (!pauseMode && key == 'g') showGrid = !showGrid;
      else if (!pauseMode && key == 'p') pauseMode = true;
      else if (pauseMode && key == ' ') pauseMode = false;
      else if (pauseMode && key == 'q') Gdx.app.exit();
      else return false;
      return true;
    }

    @Override
    public boolean touchUp(int screenX, int screenY, int pointer, int button) {
      if (button == Input.Buttons.LEFT && screenX > Gdx.graphics.getWidth() - 50 && screenY < 50) {
        pauseMode = true;
      }
      else return false;
      return true;
    }
  }
}
