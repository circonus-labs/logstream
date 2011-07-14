package com.omniti.labs.logstream;

import com.espertech.esper.client.*;
import java.lang.Math;
import java.util.UUID;
import java.util.Map;
import java.util.Set;
import java.util.HashMap;
import com.omniti.labs.logstream.EngineException;
import com.omniti.labs.logstream.EngineStatement;
import com.omniti.labs.logstream.EngineQuery;
import com.omniti.labs.logstream.EngineSet;
import com.omniti.labs.logstream.EngineListener;
import com.omniti.labs.logstream.EngineEPLDesc;
import com.omniti.labs.logstream.EngineType;
import com.omniti.labs.logstream.Histogram;
import java.util.concurrent.LinkedBlockingQueue;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONArray;
import org.apache.log4j.Logger;

public class Engine {
  private Configuration esperConfig;
  private EPServiceProvider epService;

  private EngineSet<EngineStatement> statements;
  private EngineSet<EngineQuery> queries;
  private HashMap<String,Map<String,EngineType>> types;
  private HashMap<String,Class> typeclasses;
  private LinkedBlockingQueue<EngineOutput> queue;
  private EngineMQ enginemq;
  static Logger logger = Logger.getLogger(Engine.class.getName());

  public class Catcher implements UnmatchedListener {
    public void update(EventBean e) {
      logger.error(e.getEventType().getName() + " fell through the cracks");
    }
  }

  public Engine() {
    ConfigurationEngineDefaults.Threading threadConfig;
    queue = new LinkedBlockingQueue<EngineOutput>();
    statements = new EngineSet<EngineStatement>();
    queries = new EngineSet<EngineQuery>();
    types = new HashMap<String,Map<String,EngineType>>();
    typeclasses = new HashMap<String,Class>();
    esperConfig = new Configuration();
    threadConfig = esperConfig.getEngineDefaults()
                              .getThreading();
    threadConfig.setInsertIntoDispatchPreserveOrder(false);
    threadConfig.setThreadPoolInbound(true);
    threadConfig.setThreadPoolInboundNumThreads(8);
    esperConfig.getEngineDefaults().getViewResources().setShareViews(true);
    esperConfig.addImport("java.lang.*");
    esperConfig.addImport("java.math.*");
    esperConfig.addImport("java.text.*");
    esperConfig.addImport("java.util.*");
    esperConfig.addImport("com.omniti.labs.logstream.Histogram");
    epService = EPServiceProviderManager.getProvider("logstream", esperConfig);
    epService.initialize();
    for(String s : esperConfig.getImports()) {
      logger.info("Esper importing: "+s);
    }
    //Use this for debugging
    //epService.getEPRuntime().setUnmatchedListener(new Catcher());
  }
  public EPServiceProvider getService() { return epService; }

  public void sendEvent(Object o) {
    epService.getEPRuntime().sendEvent(o);
  }
  public void sendEvent(String typename, Map<String, Object> data) {
    epService.getEPRuntime().sendEvent(data, typename);
  }
  public void setEngineMQ(EngineMQ mq) {
    enginemq = mq;
    (new Thread() {
      public void run() {
        while(true) {
          EngineOutput o = getOutput();
          enginemq.publish(o);
        }
      }
    }).start();
  }
  public EngineOutput getOutput() {
    EngineOutput o = null;
    boolean done = false;
    while(!done) {
      try {
        o = queue.take();
        if(o != null) done = true;
      }
      catch(InterruptedException e) {
      }
    }
    return o;
  }

  public void registerType(String typename, Map<String, EngineType> desc) throws javassist.CannotCompileException {
    logger.info("registering type " + typename);
    Class clazz = EngineType.makeEsperType(typename, desc);
    types.put(typename, desc);
    typeclasses.put(typename, clazz);
    epService.getEPAdministrator()
             .getConfiguration()
             .addEventType(typename, clazz);
  }
  public boolean deregisterType(String typename) {
    if(epService.getEPAdministrator()
                .getConfiguration()
                .removeEventType(typename, false)) {
      types.remove(typename);
      return true;
    }
    return false;
  }
  public Map<String, EngineType> retrieveType(String typename) {
    return types.get(typename);
  }
  public Class retrieveTypeClass(String typename) {
    return typeclasses.get(typename);
  }

  public boolean deregisterQuery(UUID u) { return queries.deregister(u); }
  public boolean deregisterQueryGroup(String u) { return queries.deregister(u); }
  public UUID registerQuery(EngineEPLDesc desc) throws EngineException {
    if(desc.uuid == null) desc.uuid = UUID.randomUUID();
    queries.deregister(desc.uuid);
    logger.info("registering query " + desc.uuid);
    try {
      EngineQuery q = new EngineQuery(this,desc);
      queries.register(q);
      q.setListener(new EngineListener(this,desc.uuid,"data",q.getStatement(),queue));
      logger.info("registered.");
    }
    catch(Exception e) { throw new EngineException(e); }
    return desc.uuid;
  }

  public boolean deregisterStatement(UUID u) { return statements.deregister(u); }
  public boolean deregisterStatementGroup(String u) { return statements.deregister(u); }
  public UUID registerStatement(EngineEPLDesc desc) throws EngineException {
    if(desc.uuid == null) desc.uuid = UUID.randomUUID();
    statements.deregister(desc.uuid);
    logger.info("registering statement " + desc.uuid);
    try {
      statements.register(new EngineStatement(this,desc));
      logger.info("registered.");
    }
    catch(Exception e) { throw new EngineException(e); }
    return desc.uuid;
  }

  private JSONObject internalGetType(String name) {
    Map<String,EngineType> type = types.get(name);
    if(type == null) return null;
    JSONObject o = new JSONObject();
    for(Map.Entry<String,EngineType> e : type.entrySet()) {
      String cname = e.getValue().getType().getName();
      cname = cname.substring(cname.lastIndexOf(".")+1);
      String validatefunc = e.getValue().getValidate();
      try { 
        JSONObject et = new JSONObject();
        et.put("type", cname);
        if(validatefunc != null) et.put("validate", validatefunc);
        o.put(e.getKey(), et);
      }
      catch (JSONException jsone) { }
    }
    return o;
  }
  public JSONObject getType(String name) {
    JSONObject o = new JSONObject();
    try { o.put(name, internalGetType(name)); }
    catch (JSONException jsone) { }
    return o;
  }
  public JSONObject getTypes() {
    JSONObject o = new JSONObject();
    for(String name : types.keySet()) {
      try { o.put(name, internalGetType(name)); }
      catch (JSONException jsone) { }
    }
    return o;
  }
  private JSONObject internalGetStatement(EngineStatement es) {
    JSONObject o = new JSONObject();
    try {
      o.put("type", es.type());
      o.put("expression", es.expression());
      o.put("group", es.group());
      UUID depends[] = es.depends();
      if(depends != null) {
        JSONArray jdep = new JSONArray();
        for(UUID dep : depends)
          jdep.put(dep.toString());
        o.put("depends", jdep);
      }
    }
    catch (JSONException jsone) { }
    return o;
  }
  public JSONObject getExpression(UUID u) {
    JSONObject o = new JSONObject();
    EngineStatement s;
    s = queries.get(u);
    if(s == null) s = statements.get(u);
    if(s != null) try { o.put(s.uuid().toString(), internalGetStatement(s)); }
                  catch (JSONException e) { }
    return o;
  }
  public JSONObject getExpressionsInGroup(String g) {
    JSONObject o = new JSONObject();
    for(EngineStatement s : statements.getGroup(g)) {
      try { o.put(s.uuid().toString(), internalGetStatement(s)); }
      catch (JSONException e) { }
    }
    for(EngineQuery s : queries.getGroup(g)) {
      try { o.put(s.uuid().toString(), internalGetStatement(s)); }
      catch (JSONException e) { }
    }
    return o;
  }
  public JSONObject getExpressions() {
    JSONObject o = new JSONObject();
    for(EngineStatement s : statements.getAll()) {
      try { o.put(s.uuid().toString(), internalGetStatement(s)); }
      catch (JSONException e) { }
    }
    for(EngineQuery s : queries.getAll()) {
      try { o.put(s.uuid().toString(), internalGetStatement(s)); }
      catch (JSONException e) { }
    }
    return o;
  }
}
