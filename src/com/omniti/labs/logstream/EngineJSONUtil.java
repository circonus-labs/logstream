/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import org.json.JSONObject;
import org.json.JSONException;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.lang.reflect.Field;
import com.omniti.labs.logstream.EngineType;
import org.apache.log4j.Logger;

public class EngineJSONUtil {

  static public class MalformedEngineJSON extends Exception {
    public MalformedEngineJSON(String e) { super(e); }
  }

  static Logger logger = Logger.getLogger(EngineJSONUtil.class.getName());
  static Map<String,EngineType> convertJSONtoType(JSONObject o) 
                            throws JSONException {
    HashMap<String,EngineType> typemap = new HashMap<String,EngineType>();
    for (String key : JSONObject.getNames(o)) {
      JSONObject d = o.getJSONObject(key);
      String type = d.getString("type");
      String func = d.optString("validate", null);
      String name = d.optString("name", null);
      if(type == null) {
        throw new JSONException("Unsupported type for key " + key);
      }
      if(type.equals("String")) {
        typemap.put(key, new EngineType(String.class, name, func));
      } else if(type.equals("Double")) {
        typemap.put(key, new EngineType(Double.class, name, func));
      } else if(type.equals("Long")) {
        typemap.put(key, new EngineType(Long.class, name, func));
      } else if(type.equals("Integer")) {
        typemap.put(key, new EngineType(Long.class, name, func)); /* we just up-convert */
      } else {
        throw new JSONException("Unsupported type " + type + " for key " + key);
      }
    }
    return typemap;
  }
  static Object convertJsonParsertoObject(Class c, Map<String,EngineType> typeinfo,
                                          JsonParser p) throws MalformedEngineJSON, IOException {
    Object obj = null;
    try {
      JsonToken tok;
      if(p.getCurrentToken() != JsonToken.START_OBJECT)
        throw new MalformedEngineJSON("Bad token: " + p.getCurrentToken());
      obj = c.newInstance();
      while(p.nextToken() != JsonToken.END_OBJECT) {
        String fieldname = p.getCurrentName();
        p.nextToken();
        try {
          EngineType et = typeinfo.get(fieldname);
          Field f = c.getField(fieldname);
          if(f != null && et != null) f.set(obj, et.makeValue(p.getText()));
        }
        catch(java.lang.NoSuchFieldException nsfe) {
          logger.info(fieldname + ": " + nsfe);
        }
      }
      p.nextToken();
    }
    catch(java.lang.InstantiationException ie) {
    }
    catch(java.lang.IllegalAccessException iae) {
    }
    return obj;
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
          logger.error(key + ": " + nsfe);
        }
      }
    }
    catch(java.lang.InstantiationException ie) {
    }
    catch(java.lang.IllegalAccessException iae) {
    }
    return obj;
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
