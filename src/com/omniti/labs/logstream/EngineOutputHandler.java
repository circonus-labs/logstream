/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Set;
import java.util.Map;
import java.util.HashMap;
import java.util.UUID;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.AsyncContext;
import org.eclipse.jetty.io.RuntimeIOException;
import javax.naming.Context;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationListener;
import org.json.JSONObject;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONStringer;
import javax.servlet.*;
import javax.servlet.http.*;
import com.omniti.labs.logstream.Engine;

public class EngineOutputHandler extends AbstractHandler implements ContinuationListener {
  Engine engine;
  HashMap<UUID,HashMap<ServletResponse,AsyncContext>> outputs;
  HashMap<ServletResponse,UUID> backmap;

  EngineOutputHandler(Engine e) {
    engine = e;
    outputs = new HashMap<UUID,HashMap<ServletResponse,AsyncContext>>();
    backmap = new HashMap<ServletResponse,UUID>();
    (new Thread() { public void run() { processQueue(); } }).start();
  }
  public void onTimeout(Continuation continuation) {
  }
  public void onComplete(Continuation continuation) {
    ServletResponse response = continuation.getServletResponse();
    cleanupResponse(response);
  }
  public void cleanupResponse(ServletResponse response) {
    UUID interest = null;
    synchronized(backmap) { interest = backmap.remove(response); }
    if(interest != null) {
      HashMap<ServletResponse,AsyncContext> m;
      synchronized(outputs) {
        m = outputs.get(interest);
        if(m == null) {
          m.remove(response);
          if(m.size() == 0) outputs.remove(interest);
        }
      }
    }
  }
  public void handle(String target,
                     Request baseRequest,
                     HttpServletRequest request,
                     HttpServletResponse response) 
        throws IOException, ServletException
  {
    if(!target.startsWith("/output/") ||
       !request.getMethod().equals("GET")) return;
    baseRequest.setHandled(true);
    try {
      int ingested = 0;
      String all = "";
      String cb;
      cb = request.getParameter("cb");
      if(cb == null) cb = "window.ls_output";
      ServletOutputStream o = response.getOutputStream();
      String uri = request.getRequestURI();
      String uuid_str = uri.substring(uri.indexOf('/',1)+1);
      UUID uuid = UUID.fromString(uuid_str);
      if(uuid == null) throw new Exception("not a uuid");

      AsyncContext ac = baseRequest.startAsync(request,response);
      ac.setTimeout(3600 * 1000);
      ac.addContinuationListener(this);

      HashMap<ServletResponse,AsyncContext> s;
      synchronized(outputs) {
        s = outputs.get(uuid);
        if(s == null) {
          s = new HashMap<ServletResponse,AsyncContext>();
          outputs.put(uuid,s);
        }
      }
      synchronized(backmap) { backmap.put(response,uuid); }
      synchronized(s) {
        s.put(response,ac);
      }
      response.setHeader("Content-type", "text/html");
      response.setStatus(200);
      o.print("<script type=\"text/javascript\">var __cb = " + cb + "</script>\n");
      response.flushBuffer();
    } catch(Exception e) {
      response.setHeader("Content-type", "text/html");
      response.setStatus(500);
    }
  }
  void processQueue() {
    EngineOutput o;
    while(true) {
      o = engine.getOutput();
      if(o == null) continue;
      HashMap<ServletResponse,AsyncContext> listeners;
      synchronized(outputs) {
        listeners = outputs.get(o.uuid);
      }
      if(listeners == null) continue;
      Set<Map.Entry<ServletResponse,AsyncContext>> lo;
      synchronized(listeners) {
        lo = listeners.entrySet();
      }
      for (Map.Entry<ServletResponse,AsyncContext> me : lo) {
        AsyncContext ac = me.getValue();
        ServletResponse response = ac.getResponse();
        try {
          ServletOutputStream out = response.getOutputStream();
          out.print("<script type=\"text/javascript\">__cb(");
          out.print("[");
          for(int i=0; i<o.object.length; i++) {
            if(i>0) out.print(",");
            out.print(o.object[i]);
          }
          out.print("]);</script>\n");
          response.flushBuffer();
        }
        catch(IOException ioe) { ac.complete(); }
        catch(RuntimeIOException eof) { cleanupResponse(response); }
      }
    }
  }
}
