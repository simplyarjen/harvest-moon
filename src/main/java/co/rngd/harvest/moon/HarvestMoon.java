package co.rngd.harvest.moon;

import com.badlogic.gdx.*;
import com.badlogic.gdx.graphics.g2d.*;
import com.badlogic.gdx.graphics.g3d.*;
import com.badlogic.gdx.backends.lwjgl3.*;

public class HarvestMoon extends Game {
  private ModelBatch modelBatch;
  private SpriteBatch spriteBatch;
  private TextureCache textureCache;

  public static void main(String... args) {
		Lwjgl3ApplicationConfiguration config = new Lwjgl3ApplicationConfiguration();
		new Lwjgl3Application(new HarvestMoon(), config);
  }

  @Override public void create() {
    modelBatch = new ModelBatch();
    spriteBatch = new SpriteBatch();
    textureCache = new TextureCache();

    GameplayScreen gameplayScreen = new GameplayScreen(modelBatch, spriteBatch, textureCache);

    RandomMap rm = new RandomMap();
    rm.width = 500;
    rm.height = 500;
    gameplayScreen.setMap(rm.generate());
    setScreen(gameplayScreen);
  }

  @Override public void dispose() {
    modelBatch.dispose();
    spriteBatch.dispose();
    textureCache.dispose();
  }
}
