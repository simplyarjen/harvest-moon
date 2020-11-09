package co.rngd.harvest.moon;

import java.io.*;
import com.badlogic.gdx.files.*;

public interface DataStore<T> {
  T readFrom(DataInput source) throws IOException;
  void writeTo(T value, DataOutput target) throws IOException;

  default void fail(String message) throws IOException {
    throw new IOException(message);
  }

  default T read(FileHandle source) {
    try(InputStream is = source.read();
        DataInputStream input = new DataInputStream(is)) {
      return readFrom(input);
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to read from " + source, e);
    }
  }

  default void write(T value, FileHandle target) {
    try(OutputStream os = target.write(false);
        DataOutputStream output = new DataOutputStream(os)) {
      writeTo(value, output);
    }
    catch (IOException e) {
      throw new IllegalStateException("Failed to write to " + target, e);
    }
  }
}
