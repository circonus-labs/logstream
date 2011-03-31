package com.omniti.labs.logstream;

import java.io.BufferedReader;
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
import javax.servlet.*;
import javax.servlet.http.*;
import com.omniti.labs.logstream.Engine;
import com.omniti.labs.logstream.EngineType;

public class EngineIngestorHandler extends AbstractHandler {
  Engine engine;
  EngineIngestorHandler(Engine e) {
    engine = e;
  }
  public int ingestObject(String type, Class c, Map<String,EngineType> typeinfo,
                          JSONObject o) throws JSONException {
    Object out;
    out = EngineJSONUtil.convertJSONtoObject(c, typeinfo, o);
    engine.sendEvent(out);
    return 1;
  }
  public int ingestArray(String type, Class c, Map<String,EngineType> typeinfo,
                         JSONArray a) {
    int i, count = 0, len = a.length();
    for(i = 0; i < len; i++) {
      JSONObject o = a.optJSONObject(i);
      if(o != null) try { count += ingestObject(type, c, typeinfo, o); }
                    catch (Exception e) { }
                    // don't do anything in array context
    }
    return count;
  }
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) {
    try { request.setCharacterEncoding("UTF8"); }
    catch(Exception e) { System.err.println(e); }

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
      while(true) {
        String line = br.readLine();
        if(line == null) break;
        all = all + line;
      }
      try {
        JSONObject obj = new JSONObject(all);
        ingested = ingestObject(type, c, typeinfo, obj);
      }
      catch(Exception e) {
        if(e.getMessage().equals("A JSONObject text must begin with '{' at character 1")) {
          e = null;
          try {
            JSONArray arr = new JSONArray(all);
            ingested = ingestArray(type, c, typeinfo, arr);
          }
          catch(Exception ie) { e = ie; };
        }
        if(e != null) {
          response.setStatus(500);
          o.print(new JSONStringer().object()
                                    .key("error")
                                    .value(e.getMessage())
                                    .endObject().toString());
          return;
        }
      }
      o.print("{ 'ingested': " + ingested + " }");
    } catch(Exception e) {}
  }
}
