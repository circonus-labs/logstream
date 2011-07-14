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
  private String exchangeName;
  private String host;
  private Integer port;
  private String user;
  private String password;
  private String virtualHost;
  private ConnectionFactory factory;
  private Connection conn;
  private Channel channel;
  static Logger logger = Logger.getLogger(EngineMQ.class.getName());

  public EngineMQ(Map<String,String> config) {
    host = config.get("host");
    port = new Integer(config.get("port"));
    exchangeName = config.get("exchange");
    if(exchangeName == null) exchangeName = "amq.topic";
    virtualHost = config.get("virtualhost");
    if(virtualHost == null) virtualHost = "/";
    user = config.get("user");
    password = config.get("password");
    factory = new ConnectionFactory();
    factory.setHost(host);
    factory.setPort(port);
    factory.setUsername(user);
    factory.setPassword(password);
    factory.setVirtualHost(virtualHost);
    factory.setRequestedHeartbeat(0);
    logger.info("connection info: " + host + ":" + port + " " + exchangeName);
  }
  public void connect() {
    logger.info("connecting...");
    try {
      conn = factory.newConnection();
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
