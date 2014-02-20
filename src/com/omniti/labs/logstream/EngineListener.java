/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.UUID;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.EventBean;
import com.espertech.esper.client.UpdateListener;
import com.espertech.esper.client.util.JSONEventRenderer;
import com.omniti.labs.logstream.EngineOutput;

public class EngineListener implements UpdateListener {
  private String name;
  private UUID uuid;
  private EPStatement statement;
  private JSONEventRenderer jsonRenderer;
  private LinkedBlockingQueue<EngineOutput> queue;

  public EngineListener(Engine e, UUID u, String n, EPStatement s,
                        LinkedBlockingQueue<EngineOutput> q) {
    statement = s;
    name = n;
    uuid = u;
    queue = q;
    jsonRenderer = e.getService()
                    .getEPRuntime()
                    .getEventRenderer()
                    .getJSONRenderer(s.getEventType());
  }
  public void processEvent(EventBean event[]) {
    String output[] = null;
    if(event != null) {
      output = new String[event.length];
      for(int i=0; i<output.length; i++)
        output[i] = jsonRenderer.render(name, event[i]);
    }
    boolean done = false;
    while(!done) {
      try {
        queue.put(new EngineOutput(uuid, output));
        done = true;
      }
      catch(InterruptedException e) {
      }
    }
  }
  public void update(EventBean[] newEvents, EventBean[] oldEvents) {
    processEvent(newEvents);
  }
}
