package com.omniti.labs.logstream;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.lang.Exception;
import java.util.ArrayList;
import java.util.Collection;
import com.omniti.labs.logstream.EngineStatement;
import com.omniti.labs.logstream.EngineException;

public class EngineSet<T extends EngineStatement> {
  ConcurrentHashMap<UUID,String> set;
  ConcurrentHashMap<UUID,T> blank_group;
  ConcurrentHashMap<String,ConcurrentHashMap<UUID,T>> hierset;

  public EngineSet() {
    set = new ConcurrentHashMap<UUID,String>();
    blank_group = new ConcurrentHashMap<UUID,T>();
    hierset = new ConcurrentHashMap<String,ConcurrentHashMap<UUID,T>>();
  }
  public boolean deregister(UUID u) {
    boolean found = false;
    synchronized(set) {
      String group = set.get(u);
      ConcurrentHashMap<UUID,T> s;
      s = (group == null) ? blank_group : hierset.get(group);
      if(s != null) {
        T q = s.remove(u);
        if(q != null) {
          found = true;
          ((EngineStatement)q).destroy();
        }
        if(s.size() == 0 && group != null) hierset.remove(group);
      }
    }
    return found;
  }
  public boolean deregister(String group) {
    boolean found = false;
    ConcurrentHashMap<UUID,T> s = hierset.remove(group);
    if(s != null) {
      found = true;
      for(UUID u : s.keySet()) {
        T q = s.remove(u);
        if(q != null) ((EngineStatement)q).destroy();
      }
    }
    return found;
  }
  public void register(T q) throws EngineException {
    deregister(q.uuid());
    try {
      set.put(q.uuid(),q.group());
      ConcurrentHashMap<UUID,T> s = (q.group() == null) ? blank_group : hierset.get(q.group());
      if(s == null) {
        s = new ConcurrentHashMap<UUID,T>();
        hierset.put(q.group(),s);
      }
      s.put(q.uuid(),q);
    }
    catch(Exception e) {
      throw new EngineException(e);
    }
  }

  public T get(UUID u) {
    String group = set.get(u);
    if(group != null) {
      ConcurrentHashMap<UUID,T> s = hierset.get(group);
      if(s != null) return s.get(u);
    }
    return null;
  }
  public Collection<T> getGroup(String group) {
    ArrayList<T> o = new ArrayList<T>();
    ConcurrentHashMap<UUID,T> s = hierset.get(group);
    if(s != null) o.addAll(s.values());
    return o;
  }
  public Collection<T> getAll() {
    ArrayList<T> o = new ArrayList<T>();
    for(ConcurrentHashMap<UUID,T> s : hierset.values())
      if(s != null) o.addAll(s.values());
    return o;
  }
}
