/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import com.omniti.labs.logstream.Engine;
import com.omniti.labs.logstream.EngineEPLDesc;
import com.espertech.esper.client.EPStatement;
import com.espertech.esper.client.UpdateListener;

public class EngineQuery extends EngineStatement {
  UpdateListener listener;
  public EngineQuery(Engine e, EngineEPLDesc d) {
    super(e,d);
    listener = null;
  }
  public void setListener(UpdateListener l) {
    if(listener != null) statement.removeListener(listener);
    listener = l;
    statement.addListener(listener);
  }
  public void destroy() {
    statement.removeListener(listener);
    super.destroy();
  }
}
