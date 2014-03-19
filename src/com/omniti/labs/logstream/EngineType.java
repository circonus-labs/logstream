/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.util.Map;
import java.lang.reflect.Constructor;
import javassist.*;

public class EngineType {
  private Class type;
  private String name;
  private String validatefunc;
  private Constructor<Object> con;
  EngineType(Class t, String n, String f) {
    type = t; validatefunc = f; name = n;
    con = null;
    try {
      con = type.getDeclaredConstructor( new Class[] { String.class } );
    }
    catch(java.lang.NoSuchMethodException nsme) { }
  }
  public Class getType() { return type; }
  public String getName() { return name; }
  public String getValidate() { return validatefunc; }
  public Object makeValue(String in) {
    Object r = null;
    try {
      if(type == String.class) r = in.intern();
      else r = con.newInstance((Object)in);
    }
    catch(java.lang.InstantiationException ie) { }
    catch(java.lang.IllegalAccessException iae) { }
    catch(java.lang.reflect.InvocationTargetException ite) { }
    return r;
  }

  static public Class makeEsperType(String name, Map<String,EngineType> desc)
                      throws javassist.CannotCompileException {
    Class newclazz = null;
    ClassPool pool = ClassPool.getDefault();
    CtClass nc = pool.makeClass(name);
    for(Map.Entry<String,EngineType> e : desc.entrySet()) {
      CtField nf = CtField.make("public " + e.getValue().getType().getName() + " " + e.getKey() + ";", nc);
      nc.addField(nf);
      nc.addMethod(CtNewMethod.getter("get" + e.getKey(), nf)); 
    }
    newclazz = nc.toClass();
    return newclazz;
  }
}
