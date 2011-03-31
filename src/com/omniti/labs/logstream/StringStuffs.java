package com.omniti.labs.logstream;

import java.util.HashMap;

public class StringStuffs {
  static public class StringOnce {
    public long refcnt;
    public String it;
    StringOnce(String s) { refcnt = 1; it = s; }
  }
  static final HashMap<String,StringOnce> string_map =
      new HashMap<String,StringOnce>();

  static public String getOnce(String a) {
    if(a == null) return null;
    synchronized(string_map) {
      StringOnce so = string_map.get(a);
      if(so != null) {
        so.refcnt++;
        return so.it;
      }
      StringOnce sonew = new StringOnce(a);
      string_map.put(sonew.it, sonew);
      return sonew.it;
    }
  }
  static public void putOnce(String a) {
    if(a == null) return;
    synchronized(string_map) {
      StringOnce so = string_map.get(a);
      if(so != null) {
        so.refcnt--;
        if(so.refcnt == 0) {
          string_map.remove(so.it);
        }
      }
    }
  }
}
