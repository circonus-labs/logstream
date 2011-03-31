package com.omniti.labs.logstream;

import org.json.JSONObject;
import org.json.JSONException;
import java.util.Map;
import java.util.HashMap;
import java.lang.reflect.Field;
import com.omniti.labs.logstream.EngineType;
import com.omniti.labs.logstream.StringStuffs;

public class EngineJSONUtil {
  static Map<String,EngineType> convertJSONtoType(JSONObject o) 
                            throws JSONException {
    HashMap<String,EngineType> typemap = new HashMap<String,EngineType>();
    for (String key : JSONObject.getNames(o)) {
      JSONObject d = o.getJSONObject(key);
      String type = d.getString("type");
      String func = d.optString("validate", null);
      if(type == null) {
        throw new JSONException("Unsupported type for key " + key);
      }
      if(type.equals("String")) {
        typemap.put(key, new EngineType(String.class, func));
      } else if(type.equals("Double")) {
        typemap.put(key, new EngineType(Double.class, func));
      } else if(type.equals("Long")) {
        typemap.put(key, new EngineType(Long.class, func));
      } else if(type.equals("Integer")) {
        typemap.put(key, new EngineType(Long.class, func)); /* we just up-convert */
      } else {
        throw new JSONException("Unsupported type " + type + " for key " + key);
      }
    }
    return typemap;
  }
  static public class RefMap<K,V> extends HashMap<K,V> {
    RefMap() { super(); }
    protected void finalize() {
      int cnt = 0;
      try{
        for (V v : values()) {
          if(v != null && v.getClass() == String.class) {
            cnt++;
            StringStuffs.putOnce((String)v);
          }
        }
      } catch(Exception e) {System.err.println(e);}
    }
  }
  static Object convertJSONtoObject(Class c, Map<String,EngineType> typeinfo,
                                                JSONObject o) {
    Object obj = null;
    try {
      obj = c.newInstance();
      for(String key : typeinfo.keySet()) {
        EngineType et = typeinfo.get(key);
        Class expect = (et != null) ? et.getType() : null;
        try {
          Field f = c.getField(key);
          if(o.has(key)) {
            Object v = et.makeValue(o.optString(key));
            f.set(obj, v);
          } else
            f.set(obj, null);
        }
        catch(java.lang.NoSuchFieldException nsfe) {
          System.err.println(key + ": " + nsfe);
        }
      }
    }
    catch(java.lang.InstantiationException ie) {
    }
    catch(java.lang.IllegalAccessException iae) {
    }
    return obj;
  }
  static Map<String,Object> convertJSONtoMap(Map<String,EngineType> typeinfo,
                                             JSONObject o)
                            throws JSONException {
    Map<String,Object> out = new RefMap<String,Object>();
    for(String key : typeinfo.keySet()) {
      EngineType et = typeinfo.get(key);
      Class expect = (et != null) ? et.getType() : null;
      if(expect == String.class) {
        if(o.has(key))
          out.put(key, StringStuffs.getOnce(o.optString(key)));
        else
          out.put(key, null);
      }
      else if(expect == Double.class)  out.put(key, o.has(key)?o.optDouble(key):null);
      else if(expect == Long.class)  out.put(key, o.has(key)?o.optLong(key):null);
      else throw new JSONException("Uknown type at runtime!");
    }
    return out;
  }
  static Map<String,String> convertJSONtoDict(JSONObject o)
                            throws JSONException {
    Map<String,String> out = new HashMap<String,String>();
    for(String key : JSONObject.getNames(o)) {
      out.put(key, o.optString(key));
    }
    return out;
  }
}
