package com.omniti.labs.logstream;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.FileOutputStream;
import java.io.File;
import java.util.Map;
import java.util.UUID;
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

public class ManipulationHandler extends AbstractHandler {
  Engine engine;
  String tdir;
  String qdir;
  String sdir;
  ManipulationHandler(Engine e, String t, String q, String s) {
    engine = e; tdir = t; qdir = q; sdir = s;
  }
  private JSONObject readJSONPUT(HttpServletRequest request) {
    JSONObject obj = null;
    try {
      BufferedReader br = request.getReader();
      String all = "";
      while(true) {
        String line = br.readLine();
        if(line == null) break;
        all = all + line;
      }
      obj = new JSONObject(all);
    }
    catch(Exception e) { }
    return obj;
  }
  private void typeGET(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    ServletOutputStream o = response.getOutputStream();
    response.setHeader("Content-type", "application/json");
    response.setStatus(200);
    if(request.getRequestURI().equals("/type/")) {
      o.print(engine.getTypes().toString());
    } else {
      String type = request.getRequestURI().substring(6);
      o.print(engine.getType(type).toString());
    }
  }
  private void typePUT(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    String type = request.getRequestURI().substring(6);
    ServletOutputStream o = response.getOutputStream();
    Map<String,EngineType> typeinfo = engine.retrieveType(type);
    if(typeinfo != null) {
      if(!engine.deregisterType(type)) {
        response.setStatus(500);
        o.print("{\"error\":\"type in use\"}\n");
        return;
      }
    }
    File jsonfile = new File(tdir + "/" + type + ".json");
    FileOutputStream out;
    try {
      out = new FileOutputStream(jsonfile);
      JSONObject in = readJSONPUT(request);
      engine.registerType(type,
                          EngineJSONUtil.convertJSONtoType(in));
      out.write(in.toString().getBytes());
      out.close();
      response.setStatus(200);
      o.print("{}\n");
    }
    catch(Exception e) {
      jsonfile.delete();
      response.setStatus(500);
      try {
        o.print(new JSONStringer().object()
                                  .key("error").value(e.toString())
                                  .endObject()
                                  .toString());
      } catch(JSONException jsone) {}
    }
  }
  private void typeDELETE(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    String type = request.getRequestURI().substring(6);
    ServletOutputStream o = response.getOutputStream();
    Map<String,EngineType> typeinfo = engine.retrieveType(type);
    if(typeinfo != null) {
      if(!engine.deregisterType(type)) {
        response.setStatus(500);
        o.print("{\"error\":\"type in use\"}\n");
        return;
      }
      (new File(tdir + "/" + type + ".json")).delete();
    }
    response.setStatus(200);
    o.print("{}\n");
  }
  private void exprGET(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    ServletOutputStream o = response.getOutputStream();
    response.setHeader("Content-type", "application/json");
    response.setStatus(200);
    if(request.getRequestURI().equals("/expression/")) {
      o.print(engine.getExpressions().toString());
    } else {
      String arg = request.getRequestURI().substring(12);
      try {
        UUID uuid = UUID.fromString(arg);
        o.print(engine.getExpression(uuid).toString());
      }
      catch(IllegalArgumentException iae) {
        o.print(engine.getExpressionsInGroup(arg).toString());
      }
    }
  }
  private void exprPUT(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    String type = request.getRequestURI().substring(12);
    ServletOutputStream o = response.getOutputStream();
    File jsonfile = null;
    try {
      JSONObject in = readJSONPUT(request);
      EngineEPLDesc desc;
      if(type.equals("new")) {
        desc = new EngineEPLDesc(UUID.randomUUID(), in);
      }
      else {
        UUID uuid;
        try { uuid = UUID.fromString(type); }
        catch (IllegalArgumentException iae) {
          response.setStatus(500);
          return;
        }
        desc = new EngineEPLDesc(uuid, in);
      }
      if(desc.type.equals("query")) {
        jsonfile = new File(qdir + "/" + desc.uuid.toString() + ".json");
        jsonfile.delete();
        FileOutputStream out = new FileOutputStream(jsonfile);
        engine.deregisterQuery(desc.uuid);
        engine.registerQuery(desc);
        out.write(in.toString().getBytes());
        out.close();
        jsonfile = null; // so it doesn't get deleted if we throw
        response.setStatus(200);
        o.print(new JSONStringer().object().key("id")
                                  .value(desc.uuid.toString()).endObject()
                                  .toString());
      }
      else if(desc.type.equals("statement")) {
        jsonfile = new File(sdir + "/" + desc.uuid.toString() + ".json");
        jsonfile.delete();
        FileOutputStream out = new FileOutputStream(jsonfile);
        engine.deregisterStatement(desc.uuid);
        engine.registerStatement(desc);
        out.write(in.toString().getBytes());
        out.close();
        jsonfile = null; // so it doesn't get deleted if we throw
        response.setStatus(200);
        o.print(new JSONStringer().object().key("id")
                                  .value(desc.uuid.toString()).endObject()
                                  .toString());
      }
      else {
        response.setStatus(500);
        o.print("{\"error\":\"type not supported\"}\n");
      }
    }
    catch(Throwable e) {
      if(jsonfile != null) jsonfile.delete();
      response.setStatus(500);
      try {
        String trace = new String();
        trace += e.toString() + "\n";
        StackTraceElement[] elements = e.getStackTrace();
        if(elements != null)
          for ( StackTraceElement se : elements )
            trace += se.toString() + "\n";
        Throwable cause = e.getCause();
        if(cause != null) {
          trace += "CAUSED BY:\n";
          trace += cause.toString() + "\n";
          elements = cause.getStackTrace();
          if(elements != null)
            for ( StackTraceElement se : elements )
              trace += se.toString() + "\n";
        }
        o.print(new JSONStringer().object()
                                  .key("error").value(trace)
                                  .endObject()
                                  .toString());
      } catch(JSONException jsone) {
        System.err.println(e);
        System.err.println(jsone);
      }
    }
  }
  private void exprDELETE(HttpServletRequest request, HttpServletResponse response)
               throws IOException {
    File jsonfile = null;
    ServletOutputStream o = response.getOutputStream();
    response.setHeader("Content-type", "application/json");
    String arg = request.getRequestURI().substring(12);
    try {
      UUID uuid = UUID.fromString(arg);
      if(engine.deregisterQuery(uuid))
        (new File(qdir + "/" + uuid.toString() + ".json")).delete();
      if(engine.deregisterStatement(uuid))
        (new File(sdir + "/" + uuid.toString() + ".json")).delete();
      response.setStatus(200);
      o.print("{}\n");
    }
    catch(Exception e) {
      response.setStatus(500);
      try {
        String trace = new String();
        trace += e.toString() + "\n";
        StackTraceElement[] elements = e.getStackTrace();
        for ( StackTraceElement se : elements ) {
            trace += se.toString() + "\n";
        }
        o.print(new JSONStringer().object()
                                  .key("error").value(trace)
                                  .endObject()
                                  .toString());
      } catch(JSONException jsone) {}
    }
  }

  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) {
    try { request.setCharacterEncoding("UTF8"); }
    catch(Exception e) { System.err.println(e); }

    try {
      if(target.startsWith("/type/")) {
        baseRequest.setHandled(true);
        if(request.getMethod().equals("GET"))
          typeGET(request, response);
        else if(request.getMethod().equals("PUT"))
          typePUT(request, response);
        else if(request.getMethod().equals("DELETE"))
          typeDELETE(request, response);
        else
          response.setStatus(405);
      }
      if(target.startsWith("/expression/")) {
        baseRequest.setHandled(true);
        if(request.getMethod().equals("GET"))
          exprGET(request, response);
        else if(request.getMethod().equals("PUT"))
          exprPUT(request, response);
        else if(request.getMethod().equals("DELETE"))
          exprDELETE(request, response);
        else
          response.setStatus(405);
      }
    } catch(IOException ioe) {}
  }
}
