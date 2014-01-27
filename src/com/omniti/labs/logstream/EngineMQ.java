package com.omniti.labs.logstream;

import java.util.Map;
import java.io.IOException;
import com.omniti.labs.logstream.EngineOutput;
import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.QueueingConsumer;
import com.rabbitmq.client.AMQP.BasicProperties;
import org.apache.log4j.Logger;

public class EngineMQ {
  private int cidx;
  private String exchangeName;
  private String hosts[];
  private Integer port;
  private String user;
  private String password;
  private String virtualHost;
  private ConnectionFactory factory[];
  private Connection conn;
  private Channel channel;
  static Logger logger = Logger.getLogger(EngineMQ.class.getName());

  public EngineMQ(Map<String,String> config) {
    String host = config.get("host");
    port = new Integer(config.get("port"));
    exchangeName = config.get("exchange");
    if(exchangeName == null) exchangeName = "amq.topic";
    virtualHost = config.get("virtualhost");
    if(virtualHost == null) virtualHost = "/";
    user = config.get("user");
    password = config.get("password");
    hosts = host.split(",");
    factory = new ConnectionFactory[hosts.length];
    for(int i = 0; i < hosts.length; i++) {
      hosts[i] = hosts[i].replace("\"", "");
      if(hosts[i].startsWith("[")) hosts[i] = hosts[i].substring(1);
      if(hosts[i].endsWith("]")) hosts[i] = hosts[i].substring(0, hosts[i].length()-1);
      factory[i] = new ConnectionFactory();
      factory[i].setHost(hosts[i]);
      factory[i].setPort(port);
      factory[i].setUsername(user);
      factory[i].setPassword(password);
      factory[i].setVirtualHost(virtualHost);
      factory[i].setRequestedHeartbeat(0);
    }
    connect();
    logger.info("connection info: " + host + ":" + port + " " + exchangeName);
  }
  public void connect() {
    logger.info("connecting...");
    try {
      cidx = (cidx + 1) % factory.length;
      conn = factory[cidx].newConnection();
      channel = conn.createChannel();
    }
    catch(IOException e) {
      e.printStackTrace();
      if(conn != null) conn.abort();
      conn = null;
      channel = null;
    }
    logger.info("connected");
  }
  public void publish(EngineOutput o) {
    if(conn == null) connect();
    try {
      if(channel != null) {
        String rk = "logstream." + o.uuid;
        BasicProperties props = new BasicProperties();
        props.setContentType("applications/json");
        StringBuffer output = new StringBuffer();
        output.append("{\""+o.uuid+"\":[");
        if(o.object != null) {
          for(int i=0;i<o.object.length;i++) {
            if(i>0) output.append(",");
            output.append(o.object[i]);
          }
        }
        output.append("]}");
        channel.basicPublish(exchangeName, rk, props, output.toString().getBytes());
      }
    }
    catch(Exception e) {
      if(conn != null) conn.abort();
      conn = null;
      channel = null;
    }
  }
}
