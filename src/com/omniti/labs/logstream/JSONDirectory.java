package com.omniti.labs.logstream;

import org.json.JSONObject;
import org.json.JSONException;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileInputStream;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import org.apache.log4j.Logger;

public class JSONDirectory {
  static Logger logger = Logger.getLogger(JSONDirectory.class.getName());
  private HashMap<String,JSONObject> objects;

  public static FilenameFilter filter(final String init_end) {
    return new FilenameFilter() {
      private String end = init_end;
      public boolean accept(File file, String name) {
        return name.endsWith(end);
      }
    };
  }

  private static String readFileAsString(File file) throws IOException {
    byte[] buffer = new byte[(int) file.length()];
    BufferedInputStream f = null;
    try {
      f = new BufferedInputStream(new FileInputStream(file));
      f.read(buffer);
    } finally {
      if (f != null) try { f.close(); } catch (IOException ignored) { }
    }
    return new String(buffer);
  }
  static public JSONObject readFile(File file) throws JSONException, IOException {
    String json_str = readFileAsString(file);
    return new JSONObject(json_str);
  }
  private void readAll(File d) {
    File files[] = d.listFiles(filter(".json"));
    objects = new HashMap<String,JSONObject>();
    int nobj = 0;
    for (File file : files) {
      try {
        JSONObject json = readFile(file);
        String typename = file.getName().replace(".json", "");
        objects.put(typename, json);
      } catch(Exception e) {
        logger.error(file.getName());
        e.printStackTrace();
      }
    }
  }
  public JSONDirectory(String path) {
    readAll(new File(path));
  }
  public JSONDirectory(File d) {
    readAll(d);
  }
  Set<Map.Entry<String,JSONObject>> getSet() {
    return objects.entrySet();
  }
}
