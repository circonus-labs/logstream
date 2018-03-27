/* Copyright 2012-2014 Circonus, Inc. All rights reserved. */
/* This program released under the terms of the GPLv2 */

package com.omniti.labs.logstream;

import java.util.Map;
import java.io.IOException;
import com.omniti.labs.logstream.EngineOutput;
import com.circonus.FqClient;
import com.circonus.FqClientImplNoop;
import com.circonus.FqCommand;
import com.circonus.FqMessage;
import org.apache.log4j.Logger;

public class EngineFQ {
  private int cidx;
  private String exchangeName;
  private String hosts[];
  private Integer port;
  private String user;
  private String password;
  private FqClient conn;
  static Logger logger = Logger.getLogger(EngineFQ.class.getName());

  public EngineFQ(Map<String,String> config) {
    String host = config.get("host");
    if(host == null) host = "localhost";
    String port_str = config.get("port");
    if(port_str == null) port_str = "8765";
    port = new Integer(port_str);
    exchangeName = config.get("exchange");
    if(exchangeName == null) exchangeName = "logstream.cep";
    user = config.get("user");
    if(user == null) user = "guest";
    password = config.get("password");
    if(password == null) password = "guest";
    hosts = host.split(",");

    FqClientImplNoop noop = new FqClientImplNoop();
    try {
      conn = new FqClient(noop);
      conn.creds(hosts[0], port, user, password);
      conn.connect();
    } catch(Exception e) {
      logger.error("failed to establish Fq connection");
      e.printStackTrace();
    }
    logger.info("connection info: " + hosts[0] + ":" + port + " " + exchangeName);
  }
  public void publish(EngineOutput o) {
    try {
      String rk = "logstream." + o.uuid;
      StringBuffer output = new StringBuffer();
      output.append("{\""+o.uuid+"\":[");
      if(o.object != null) {
        for(int i=0;i<o.object.length;i++) {
          if(i>0) output.append(",");
          output.append(o.object[i]);
        }
      }
      output.append("]}");
      FqMessage msg = new FqMessage();
      msg.setMsgId();
      msg.setExchange(exchangeName.getBytes());
      msg.setRoute(rk.getBytes());
      msg.setPayload(output.toString().getBytes());
      conn.send(msg);
    }
    catch(Exception e) {
    }
  }
}
