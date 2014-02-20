/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Map;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import javax.naming.Context;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONStringer;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonToken;

import javax.servlet.*;
import javax.servlet.http.*;
import com.omniti.labs.logstream.Engine;
import com.omniti.labs.logstream.EngineType;
import org.apache.log4j.Logger;

public class EngineIngestorHandler extends AbstractHandler {
  static Logger logger = Logger.getLogger(EngineIngestorHandler.class.getName());
  Engine engine;
  JsonFactory factory;

  EngineIngestorHandler(Engine e) {
    engine = e;
    factory = new JsonFactory();
  }
  public int ingestObject(String type, Class c, Map<String,EngineType> typeinfo,
                          JsonParser p) throws IOException,
                                               EngineJSONUtil.MalformedEngineJSON {
    Object out;
    out = EngineJSONUtil.convertJsonParsertoObject(c, typeinfo, p);
    engine.sendEvent(out);
    return 1;
  }
  public int ingestArray(String type, Class c, Map<String,EngineType> typeinfo,
                         JsonParser p) {
    int count = 0;
    try {
      if(p.getCurrentToken() != JsonToken.START_ARRAY) return 0;
      p.nextToken();
      while(p.getCurrentToken() != JsonToken.END_ARRAY) {
        count += ingestObject(type, c, typeinfo, p);
      }
      p.nextToken();
    }
    catch(IOException e) { logger.error(e); }
    catch(EngineJSONUtil.MalformedEngineJSON je) { logger.error(je); }
    return count;
  }
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) {
    try { request.setCharacterEncoding("UTF8"); }
    catch(Exception e) { logger.error(e); }

    if(!target.startsWith("/ingest/") ||
       !request.getMethod().equals("PUT")) return;
    baseRequest.setHandled(true);

    try {
      int ingested = 0;
      String all = "";
      ServletOutputStream o = response.getOutputStream();
      String type = request.getRequestURI().substring(8);
      Map<String,EngineType> typeinfo = engine.retrieveType(type);
      Class c = engine.retrieveTypeClass(type);

      response.setHeader("Content-type", "application/json");
      if(typeinfo == null || c == null) {
        response.setStatus(404);
        return;
      }
      BufferedReader br = request.getReader();
      JsonParser parser = factory.createJsonParser(br);
      try {
        JsonToken start = parser.nextToken();
        if(start == JsonToken.START_OBJECT) {
          try {
            ingested = ingestObject(type, c, typeinfo, parser);
          }
          catch(IOException e) { logger.error(e); }
          catch(EngineJSONUtil.MalformedEngineJSON je) { logger.error(je); }
        }
        else if(start == JsonToken.START_ARRAY) {
          ingested = ingestArray(type, c, typeinfo, parser);
        }
        else {
          throw new java.lang.RuntimeException("invalid JSON start sequence");
        }
      }
      catch(Exception e) {
        logger.error(e);
      }
      parser.close();
      o.print("{ 'ingested': " + ingested + " }");
    } catch(Exception e) {}
  }
}
