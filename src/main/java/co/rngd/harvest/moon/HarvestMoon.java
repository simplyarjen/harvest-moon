package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.assets.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.backends.lwjgl3.*;

public class HarvestMoon extends Game {
  private ModelBatch modelBatch;
  private SpriteBatch spriteBatch;
  private TextureCache textureCache;
  private AssetManager assetManager;

  public static void main(String... args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		new Lwjgl3Application(new HarvestMoon(), config);
  }

  @Override public void create() {
    modelBatch = new ModelBatch();
    spriteBatch = new SpriteBatch();
    textureCache = new TextureCache();
    assetManager = new AssetManager();

    GameMap.Store.setAssetManager(assetManager);
    GameplayScreen gameplayScreen = new GameplayScreen(modelBatch, spriteBatch, textureCache);

    RandomMap rm = new RandomMap(assetManager);
    rm.width = 200;
    rm.height = 200;
    GameMap map = rm.generate();
    gameplayScreen.setMap(map);
    setScreen(gameplayScreen);
  }

  @Override public void dispose() {
    modelBatch.dispose();
    spriteBatch.dispose();
    textureCache.dispose();
    assetManager.dispose();
  }
}
