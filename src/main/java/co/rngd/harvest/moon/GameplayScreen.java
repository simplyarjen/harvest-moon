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
  private final TextureCache textureCache;

  private GameMap map;

  private Vector3 focalPoint = new Vector3(0, 0, 0);
  private float pan = (float) Math.PI / 3, tilt = (float) Math.PI / 6, distance = 30f;
  private PerspectiveCamera camera;

  private Environment environment;

  private ModelInstance mapModel;
  private ModelInstance gridModel;
  private boolean showGrid = true;

  private ImageButton pauseButton;
  private boolean pauseMode;
  private PauseMenu pauseMenu;


  public GameplayScreen(ModelBatch modelBatch, SpriteBatch spriteBatch, TextureCache textureCache) {
    this.modelBatch = modelBatch;
    this.spriteBatch = spriteBatch;
    this.textureCache = textureCache;
  }

  public void setMap(GameMap map) {
    this.map = map;
    if (mapModel != null) mapModel.model.dispose();

    ModelBuilder modelBuilder = new ModelBuilder();
    mapModel = new ModelInstance(map.createSurfaceModel(modelBuilder));
    gridModel = new ModelInstance(map.createGridModel(modelBuilder));
    focalPoint.set(map.height / 2, 0, map.width / 2);
  }

  @Override
  public void show() {
    camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.near = 1f;
    camera.far = 300f;
    updateCamera();

    Gdx.input.setInputProcessor(new Controller());
    environment = new Environment();
    environment.set(new ColorAttribute(ColorAttribute.AmbientLight, 0.4f, 0.4f, 0.4f, 1f));
    environment.add(new DirectionalLight().set(0.9f, 0.7f, 0.6f, -1f, -0.8f, -0.2f));

    pauseButton = ImageButton.create(textureCache.get("icons/pause.png"));
    pauseButton.setPosition(-50, -50);
    pauseButton.setSize(40, 40);

    pauseMenu = new PauseMenu();
  }

  private void updateCamera() {
    camera.position.set((float) (Math.sin(pan) * Math.cos(tilt) * distance),
                        (float) (                Math.sin(tilt) * distance),
                        (float) (Math.cos(pan) * Math.cos(tilt) * distance))
                   .add(focalPoint);
    camera.lookAt(focalPoint);
    camera.up.set(0, 1, 0);
    camera.normalizeUp();
    camera.update();
  }

  @Override
  public void resize(int width, int height) {
    if (camera == null) return;
    camera.viewportWidth = width;
    camera.viewportHeight = height;
    updateCamera();

    spriteBatch.setProjectionMatrix(new Matrix4().setToOrtho2D(0, 0, width, height));
  }

  @Override
  public void dispose() {
    if (mapModel != null) mapModel.model.dispose();
    if (gridModel != null) gridModel.model.dispose();
  }

  @Override
  public void render(float delta) {
    int width = Gdx.graphics.getWidth();
    int height = Gdx.graphics.getHeight();
    Gdx.gl.glViewport(0, 0, width, height);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    if (!pauseMode) {
      if (Gdx.input.isKeyPressed(Input.Keys.UP))
        focalPoint.sub((float) Math.sin(pan) * delta * 10f, 0 , (float) Math.cos(pan) * delta * 10f);
      if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
        focalPoint.add((float) Math.sin(pan) * delta * 10f, 0 , (float) Math.cos(pan) * delta * 10f);
      if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
        focalPoint.add((float) -Math.cos(pan) * delta * 10f, 0 , (float) Math.sin(pan) * delta * 10f);
      if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
        focalPoint.sub((float) -Math.cos(pan) * delta * 10f, 0 , (float) Math.sin(pan) * delta * 10f);
      if (Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) distance -= delta * 10f;
      if (Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) distance += delta * 10f;
      if (Gdx.input.isKeyPressed(Input.Keys.A)) pan -= delta;
      if (Gdx.input.isKeyPressed(Input.Keys.D)) pan += delta;
      if (Gdx.input.isKeyPressed(Input.Keys.W)) tilt += delta;
      if (Gdx.input.isKeyPressed(Input.Keys.S)) tilt -= delta;

      if (focalPoint.x < 0) focalPoint.x = 0;
      if (focalPoint.x > map.height) focalPoint.x = map.height;
      if (focalPoint.z < 0) focalPoint.z = 0;
      if (focalPoint.z > map.width) focalPoint.z = map.width;
      if (distance < 5f) distance = 5f;
      if (distance > 100f) distance = 100f;
      if (tilt < Math.PI * 0.1) tilt = (float) Math.PI * 0.1f;
      if (tilt > Math.PI * 0.45) tilt = (float) Math.PI * 0.45f;
      updateCamera();
    }
    if (pauseButton.wasPressed()) pauseMode = !pauseMode;
    if (pauseMenu.wasClosePressed()) pauseMode = false;
    if (pauseMenu.wasExitPressed()) Gdx.app.exit();
    if (pauseMenu.wasLoadPressed()) System.out.println("Load pressed");
    if (pauseMenu.wasSavePressed()) System.out.println("Save pressed");

    pauseButton.update();
    if (pauseMode) pauseMenu.update();

    modelBatch.begin(camera);
    modelBatch.render(mapModel, environment);
    if (showGrid) {
      gridModel.transform.setToTranslation(0, distance / 1000f, 0);
      modelBatch.render(gridModel, environment);
    }
    modelBatch.end();

    spriteBatch.begin();
    pauseButton.draw(spriteBatch);
    if (pauseMode) pauseMenu.draw(width, height);
    spriteBatch.end();
  }

  private class PauseMenu {
    private NinePatch background;
    private ImageButton closeButton;
    private ImageButton exitButton;
    private ImageButton saveButton;
    private ImageButton loadButton;

    private PauseMenu() {
      background = new NinePatch(textureCache.get("panels/metal.png"), 10, 10, 10, 10);

      closeButton = makeButton("icons/cross.png", 20, 20);
      exitButton = makeButton("icons/exit.png", 50, 50);
      saveButton = makeButton("icons/import.png", 50, 50);
      loadButton = makeButton("icons/export.png", 50, 50);
    }

    private ImageButton makeButton(String name, float w, float h) {
      ImageButton result = ImageButton.create(textureCache.get(name));
      result.setSize(w, h);
      return result;
    }

    void update() {
      closeButton.update();
      exitButton.update();
      saveButton.update();
      loadButton.update();
    }

    void draw(float width, float height) {
      float x = (width - 200) / 2, y = (height - 200) / 2;

      background.draw(spriteBatch, x, y, 200, 200);
      closeButton.setPosition(x + 170, y + 170);
      closeButton.draw(spriteBatch);
      saveButton.setPosition(x + 20, y + 120);
      saveButton.draw(spriteBatch);
      loadButton.setPosition(x + 20, y + 70);
      loadButton.draw(spriteBatch);
      exitButton.setPosition(x + 20, y + 20);
      exitButton.draw(spriteBatch);
    }

    boolean wasClosePressed() { return closeButton.wasPressed(); }
    boolean wasSavePressed() { return saveButton.wasPressed(); }
    boolean wasLoadPressed() { return loadButton.wasPressed(); }
    boolean wasExitPressed() { return exitButton.wasPressed(); }
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
  }
}
