package co.rngd.harvest.moon;

import java.util.*;
import com.badlogic.gdx.utils.*;
import com.badlogic.gdx.graphics.*;
import com.badlogic.gdx.graphics.g2d.*;

public class TextureCache implements Disposable {
  private final Map<String, TextureRegion> cachedRegions = new HashMap<>();
  private final List<Disposable> disposables = new ArrayList<>();

  public TextureRegion get(String name) {
    String key = name.lastIndexOf('.') > 0 ? name.substring(0, name.lastIndexOf('.') - 1) : name;
    if (!cachedRegions.containsKey(key)) {
      Texture texture = new Texture(name);
      disposables.add(texture);
      cachedRegions.put(key, new TextureRegion(texture));
    }
    return cachedRegions.get(key);
  }

  public void preloadAtlas(String atlasName) {
    TextureAtlas atlas = new TextureAtlas(atlasName);
    disposables.add(atlas);
    for (TextureAtlas.AtlasRegion region : atlas.getRegions()) {
      // does not handle sequences correctly
      cachedRegions.put(region.name, region);
    }
  }

  @Override
  public void dispose() {
    disposables.forEach(Disposable::dispose);
    disposables.clear();
    cachedRegions.clear();
  }
}
