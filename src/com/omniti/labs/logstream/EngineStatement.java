package com.omniti.labs.logstream;

import java.util.UUID;
import com.omniti.labs.logstream.Engine;
import com.espertech.esper.client.EPStatement;

public class EngineStatement {
  protected EngineEPLDesc d;
  protected EPStatement statement;

  public EngineStatement(Engine e, EngineEPLDesc desc) {
    d = desc;
    statement = e.getService().getEPAdministrator().createEPL(expression());
  }
  public UUID uuid() { return d.uuid; }
  public String type() { return d.type; }
  public String expression() { return d.expression; }
  public String group() { return d.group; }
  public UUID[] depends() { return d.depends; }
  public EPStatement getStatement() { return statement; }
  public void destroy() { statement.destroy(); }
}
