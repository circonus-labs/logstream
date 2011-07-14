package com.omniti.labs.logstream;

import java.io.File;
import org.json.JSONObject;
import java.util.Map;
import java.util.ArrayList;
import com.omniti.labs.logstream.Engine;
import com.omniti.labs.logstream.EngineMQ;
import com.omniti.labs.logstream.EngineJSONUtil;
import com.omniti.labs.logstream.JSONDirectory;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Logger;

public class logstream {
  static Logger logger = Logger.getLogger(logstream.class.getName());
  Engine engine;

  public logstream() {
    engine = new Engine();
  }
  public void start(JSONObject config) {
    EngineServer es = new EngineServer(engine, config);
    try { es.start(); }
    catch(Exception e) { e.printStackTrace(); }
  }

  public void setupMQ(Map<String,String> config) {
    engine.setEngineMQ(new EngineMQ(config));
  }
  public void loadTypes(String f) {
    JSONDirectory jd = new JSONDirectory(f);
    for ( Map.Entry<String,JSONObject> o : jd.getSet() ) {
      try {
        engine.registerType(o.getKey(),
                            EngineJSONUtil.convertJSONtoType(o.getValue()));
      }
      catch(Exception e) { e.printStackTrace(); }
    }
  }
  public void loadStatements(String f) {
    JSONDirectory jd = new JSONDirectory(f);
    ArrayList<EngineEPLDesc> m = new ArrayList<EngineEPLDesc>();
    int cnt;
    for ( Map.Entry<String,JSONObject> o : jd.getSet() ) {
      EngineEPLDesc desc = new EngineEPLDesc(o.getKey(), o.getValue());
      if(desc.type.equals("statement")) m.add(desc);
    }
    for(EngineEPLDesc d : EngineEPLDesc.order(m.toArray(new EngineEPLDesc[] { null }))) {
      try { engine.registerStatement(d); }
      catch (EngineException e) {
        logger.error(e);
      }
    }
  }
  public void loadQueries(String f) {
    JSONDirectory jd = new JSONDirectory(f);
    ArrayList<EngineEPLDesc> m = new ArrayList<EngineEPLDesc>();
    int cnt;
    for ( Map.Entry<String,JSONObject> o : jd.getSet() ) {
      EngineEPLDesc desc = new EngineEPLDesc(o.getKey(), o.getValue());
      if(desc.type.equals("query")) {
        try { engine.registerQuery(desc); }
        catch (EngineException e) {
          logger.error(e);
        }
      }
    }
  }

  public static void main(String args[]) {
    BasicConfigurator.configure();
    logstream ls = new logstream();
    JSONObject config;

    try {
      config = JSONDirectory.readFile(new File(args[0]));
      ls.loadTypes(config.getString("typedir"));
      ls.loadStatements(config.getString("statementdir"));
      ls.loadQueries(config.getString("querydir"));
      if(config.has("mq"))
        ls.setupMQ(EngineJSONUtil.convertJSONtoDict(config.getJSONObject("mq")));
      ls.start(config);
    }
    catch (Exception e) { e.printStackTrace(); System.exit(1); }
  }
}
