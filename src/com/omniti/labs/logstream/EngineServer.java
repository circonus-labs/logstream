/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.server.handler.ContextHandlerCollection;
import org.eclipse.jetty.server.handler.HandlerList;
import org.eclipse.jetty.servlet.ServletHolder;
import org.json.JSONObject;
import com.omniti.labs.logstream.EngineIngestorHandler;
import com.omniti.labs.logstream.ManipulationHandler;

public class EngineServer {
  Server server;
  public EngineServer(Engine e, JSONObject config) {
    Integer port = new Integer((int)config.optLong("port"));
    if(port == null) port = 22839;
    HandlerList contexts = new HandlerList();

    contexts.addHandler(new EngineIngestorHandler(e));
    contexts.addHandler(new ManipulationHandler(e, config.optString("typedir"),
                                                config.optString("querydir"),
                                                config.optString("statementdir")));

    server = new Server(port);
    server.setHandler(contexts);
  }
  public void start() throws Exception { server.start(); }
}
