package co.rngd.harvest.moon;

import java.io.*;

import com.badlogic.gdx.*;
import com.badlogic.gdx.math.*;
import com.badlogic.gdx.math.collision.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.VertexAttributes.Usage;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.graphics.g3d.utils.*;
import com.badlogic.gdx.graphics.g3d.utils.shapebuilders.*;
import com.badlogic.gdx.graphics.g3d.attributes.*;
import com.badlogic.gdx.graphics.g3d.environment.*;

public class GameplayScreen extends ScreenAdapter {
  private final ModelBatch modelBatch;
  private final SpriteBatch spriteBatch;
  private final TextureCache textureCache;

  private State state = new State();

  public static class State {
    private GameMap map;

    private Vector3 cameraFocus = new Vector3(0, 0, 0);
    private float pan = (float) Math.PI / 3, tilt = (float) Math.PI / 6, distance = 30f;
    private boolean pauseMode = false;
    private boolean showGrid = true;

    public static final DataStore<State> Store = new DataStore<State>() {
      private static final int VERSION = 1;

      @Override
      public void writeTo(State state, DataOutput output) throws IOException {
        output.writeInt(VERSION);
        output.writeFloat(state.cameraFocus.x);
        output.writeFloat(state.cameraFocus.y);
        output.writeFloat(state.cameraFocus.z);
        output.writeFloat(state.pan);
        output.writeFloat(state.tilt);
        output.writeFloat(state.distance);
        output.writeBoolean(state.pauseMode);
        output.writeBoolean(state.showGrid);
        GameMap.Store.writeTo(state.map, output);
      }

      @Override
      public State readFrom(DataInput input) throws IOException {
        if (input.readInt() != VERSION) fail("GameplayScreen.State version mismatch");
        State state = new State();
        state.cameraFocus.x = input.readFloat();
        state.cameraFocus.y = input.readFloat();
        state.cameraFocus.z = input.readFloat();
        state.pan = input.readFloat();
        state.tilt = input.readFloat();
        state.distance = input.readFloat();
        state.pauseMode = input.readBoolean();
        state.showGrid = input.readBoolean();
        state.map = GameMap.Store.readFrom(input);
        return state;
      }
    };
  }

  private PerspectiveCamera camera;
  private Environment environment;

  private ModelInstance mapModel;
  private ModelInstance gridModel;
  private ModelInstance selectionModel;
  private boolean showSelection;

  private ImageButton pauseButton;
  private PauseMenu pauseMenu;


  public GameplayScreen(ModelBatch modelBatch, SpriteBatch spriteBatch, TextureCache textureCache) {
    this.modelBatch = modelBatch;
    this.spriteBatch = spriteBatch;
    this.textureCache = textureCache;
  }

  public void setMap(GameMap map) {
    state.map = map;
    state.cameraFocus.set(map.height / 2, 0, map.width / 2);

    rebuildMapModels();
  }

  private void rebuildMapModels() {
    if (mapModel != null) mapModel.model.dispose();
    if (gridModel != null) gridModel.model.dispose();

    ModelBuilder modelBuilder = new ModelBuilder();
    mapModel = new ModelInstance(state.map.createSurfaceModel(modelBuilder));
    gridModel = new ModelInstance(state.map.createGridModel(modelBuilder));
    selectionModel = new ModelInstance(makeSelectionModel(modelBuilder));
    state.map.updateObjectModels();
  }

  Model makeSelectionModel(ModelBuilder builder) {
    builder.begin();
    MeshPartBuilder meshBuilder = builder.part("selection",
        GL20.GL_TRIANGLES, Usage.Position | Usage.Normal,
        new Material(
          ColorAttribute.createDiffuse(0.9f, 0.9f, 0.0f, 1f),
          new BlendingAttribute(0.2f),
          new ColorAttribute(ColorAttribute.AmbientLight, 0.9f, 0.9f, 0.9f, 1f)
          ));
    BoxShapeBuilder.build(meshBuilder, 0f, 0f, 0f, -1f, -2f, -1f);
    BoxShapeBuilder.build(meshBuilder, 0f, 0f, 0f, 1f, 2f, 1f);
    return builder.end();
  }

  public void setState(State state) {
    this.state = state;

    rebuildMapModels();
    if (camera != null) updateCamera();
  }

  @Override
  public void show() {
    camera = new PerspectiveCamera(30, Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.near = 1f;
    camera.far = 1000f;
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
    camera.position.set((float) (Math.sin(state.pan) * Math.cos(state.tilt) * state.distance),
                        (float) (                      Math.sin(state.tilt) * state.distance),
                        (float) (Math.cos(state.pan) * Math.cos(state.tilt) * state.distance))
                   .add(state.cameraFocus);
    camera.lookAt(state.cameraFocus);
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

  private void updateSelectionModel() {
    Ray ray = camera.getPickRay(Gdx.input.getX(), Gdx.input.getY());
    Vector3 selected = state.map.intercept(ray);
    if (selected == null) {
      showSelection = false;
      return;
    }
    showSelection = true;

    int r = (int) selected.x, c = (int) selected.z;
    if (r < 0) r = 0;
    if (r >= state.map.height) r = state.map.height - 1;
    if (c < 0) c = 0;
    if (c >= state.map.width) c = state.map.width - 1;
    int h00 = state.map.height(r, c);
    int h01 = state.map.height(r, c + 1);
    int h10 = state.map.height(r + 1, c);
    int h11 = state.map.height(r + 1, c + 1);
    int hmin = h00 < h01 ? h00 : h01 < h10 ? h01 : h10 < h11 ? h10 : h11;
    selectionModel.transform.setToTranslation(r + 0.5f, hmin + 1f, c + 0.5f);
  }

  @Override
  public void dispose() {
    if (mapModel != null) mapModel.model.dispose();
    if (gridModel != null) gridModel.model.dispose();
  }

  private static final float MIN_DIST = 5f, MAX_DIST = 200f, V_DIST = 25f;
  private static final float V_MOVE = 20f;
  private static final float MIN_TILT = (float) Math.PI * 0.1f, MAX_TILT = (float) Math.PI * 0.45f, V_TILT = 1f;
  private static final float V_PAN = 1f;

  @Override
  public void render(float delta) {
    int width = Gdx.graphics.getWidth();
    int height = Gdx.graphics.getHeight();
    Gdx.gl.glViewport(0, 0, width, height);
    Gdx.gl.glClear(GL20.GL_COLOR_BUFFER_BIT | GL20.GL_DEPTH_BUFFER_BIT);

    if (!state.pauseMode) {
      if (Gdx.input.isKeyPressed(Input.Keys.UP))
        state.cameraFocus.sub((float) Math.sin(state.pan) * delta * V_MOVE, 0 , (float) Math.cos(state.pan) * delta * V_MOVE);
      if (Gdx.input.isKeyPressed(Input.Keys.DOWN))
        state.cameraFocus.add((float) Math.sin(state.pan) * delta * V_MOVE, 0 , (float) Math.cos(state.pan) * delta * V_MOVE);
      if (Gdx.input.isKeyPressed(Input.Keys.LEFT))
        state.cameraFocus.add((float) -Math.cos(state.pan) * delta * V_MOVE, 0 , (float) Math.sin(state.pan) * delta * V_MOVE);
      if (Gdx.input.isKeyPressed(Input.Keys.RIGHT))
        state.cameraFocus.sub((float) -Math.cos(state.pan) * delta * V_MOVE, 0 , (float) Math.sin(state.pan) * delta * V_MOVE);
      if (Gdx.input.isKeyPressed(Input.Keys.PAGE_UP)) state.distance -= delta * V_DIST;
      if (Gdx.input.isKeyPressed(Input.Keys.PAGE_DOWN)) state.distance += delta * V_DIST;
      if (Gdx.input.isKeyPressed(Input.Keys.A)) state.pan -= delta * V_PAN;
      if (Gdx.input.isKeyPressed(Input.Keys.D)) state.pan += delta * V_PAN;
      if (Gdx.input.isKeyPressed(Input.Keys.W)) state.tilt += delta * V_TILT;
      if (Gdx.input.isKeyPressed(Input.Keys.S)) state.tilt -= delta * V_TILT;

      if (state.cameraFocus.x < 0) state.cameraFocus.x = 0;
      if (state.cameraFocus.x > state.map.height) state.cameraFocus.x = state.map.height;
      if (state.cameraFocus.z < 0) state.cameraFocus.z = 0;
      if (state.cameraFocus.z > state.map.width) state.cameraFocus.z = state.map.width;
      if (state.distance < MIN_DIST) state.distance = MIN_DIST;
      if (state.distance > MAX_DIST) state.distance = MAX_DIST;
      if (state.tilt < MIN_TILT) state.tilt = MIN_TILT;
      if (state.tilt > MAX_TILT) state.tilt = MAX_TILT;
      updateCamera();
    }
    if (pauseButton.wasPressed()) state.pauseMode = !state.pauseMode;
    if (pauseMenu.wasClosePressed()) state.pauseMode = false;
    if (pauseMenu.wasExitPressed()) Gdx.app.exit();
    if (pauseMenu.wasLoadPressed()) {
      setState(State.Store.read(Gdx.files.external("savegame.dat")));
    }
    if (pauseMenu.wasSavePressed()) {
      State.Store.write(state, Gdx.files.external("savegame.dat"));
    }

    pauseButton.update();
    if (state.pauseMode) pauseMenu.update();

    updateSelectionModel();

    modelBatch.begin(camera);
    modelBatch.render(mapModel, environment);
    for (ModelInstance instance : state.map.getObjectModels())
      modelBatch.render(instance, environment);
    if (!state.pauseMode) modelBatch.render(selectionModel, environment);
    if (state.showGrid) {
      gridModel.transform.setToTranslation(0, state.distance / 1000f, 0);
      modelBatch.render(gridModel, environment);
    }
    modelBatch.end();

    spriteBatch.begin();
    pauseButton.draw(spriteBatch);
    if (state.pauseMode) pauseMenu.draw(width, height);
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
      if (!state.pauseMode && key == 'g') state.showGrid = !state.showGrid;
      else if (!state.pauseMode && key == 'p') state.pauseMode = true;
      else if (state.pauseMode && key == ' ') state.pauseMode = false;
      else if (state.pauseMode && key == 'q') Gdx.app.exit();
      else return false;
      return true;
    }
  }
}
