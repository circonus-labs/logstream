/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import org.json.JSONArray;
import org.json.JSONObject;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class EngineEPLDesc {
  public boolean seen;
  public String type;
  public UUID uuid;
  public String expression;
  public String group;
  public UUID depends[];

  public EngineEPLDesc(String t,UUID u,String g,String e,UUID d[]) {
    type = t; uuid = u; group = g; expression = e; depends = d;
  }
  public EngineEPLDesc(UUID u, JSONObject o) {
    uuid = u;
    type = o.optString("type");
    group = o.optString("group");
    if(group == null || group.equals("")) group = "unset";
    expression = o.optString("expression");
    if(type.equals("statement")) {
      JSONArray a = o.optJSONArray("depends");
      if(a != null) {
        depends = new UUID[a.length()];
        for(int i=0; i<a.length(); i++)
          depends[i] = UUID.fromString(a.optString(i));
      }
    }
  }
  public EngineEPLDesc(String u, JSONObject o) {
    this(UUID.fromString(u), o);
  }

  static public void dfs_add(ArrayList<EngineEPLDesc> a,
                             EngineEPLDesc o,
                             HashMap<UUID,EngineEPLDesc> m) {
    if(o.seen) return;
    if(o.depends != null)
      for (UUID s : o.depends) {
        EngineEPLDesc dep = m.get(s);
        if(dep == null)
          throw new RuntimeException("no such statement: " + s);
        dfs_add(a, m.get(s), m);
      }
    if(o.seen) return;
    a.add(o);
  }
  static public List<EngineEPLDesc> order(EngineEPLDesc in[]) {
    ArrayList<EngineEPLDesc> a = new ArrayList<EngineEPLDesc>();
    HashMap<UUID,EngineEPLDesc> m = new HashMap<UUID,EngineEPLDesc>();
    for(int i = 0; i<in.length; i++) {
      if(in[i] == null) continue;
      in[i].seen = false;
      m.put(in[i].uuid, in[i]);
    }
    for(int i = 0; i<in.length; i++) if(in[i] != null) dfs_add(a, in[i], m);
    return a;
  }
}
