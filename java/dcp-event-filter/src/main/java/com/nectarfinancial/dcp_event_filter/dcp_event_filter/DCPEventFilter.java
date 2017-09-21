package com.nectarfinancial.dcp_event_filter.dcp_event_filter;

import com.couchbase.client.dcp.Client;
import com.couchbase.client.dcp.ControlEventHandler;
import com.couchbase.client.dcp.DataEventHandler;
import com.couchbase.client.dcp.StreamFrom;
import com.couchbase.client.dcp.StreamTo;
import com.couchbase.client.dcp.message.DcpDeletionMessage;
import com.couchbase.client.dcp.message.DcpMutationMessage;
import com.couchbase.client.dcp.transport.netty.ChannelFlowController;
import com.couchbase.client.deps.io.netty.buffer.ByteBuf;
import com.couchbase.client.deps.io.netty.util.CharsetUtil;
import com.google.gson.Gson;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.MessageProperties;

import com.rabbitmq.client.Connection;
import com.rabbitmq.client.Channel;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeoutException;

public class DCPEventFilter {
	
	private static Channel channel = null;
	private static Connection connection = null;
	private static String rabbitMQHost = null;
	private static String rabbitMQUsername = null;
	private static String rabbitMQPassword = null;
	private static String couchbaseHost = null;
	private static String couchbasePassword = null;
	private static String couchbaseBucket = null;
	private static boolean useSSL = false;
	private static EventConfig[] eventConfigs = null;
	
	private static int instCount = 0;
	
	public static void main(String[] argv) throws IOException, TimeoutException {
		Gson gson = new Gson();
		Map<String, String> envVars = System.getenv();
        rabbitMQHost = envVars.getOrDefault("RABBITMQ_PORT_4369_TCP_ADDR", "localhost");
        rabbitMQUsername = envVars.get("RABBITMQ_USERNAME");
        rabbitMQPassword = envVars.get("RABBITMQ_PASSWORD");
        couchbaseHost = envVars.getOrDefault("COUCHBASE_PORT_8091_TCP_ADDR", "localhost");
        couchbasePassword = envVars.get("COUCHBASE_PASSWORD");
        couchbaseBucket = envVars.getOrDefault("COUCHBASE_BUCKET", "models");
        useSSL = Boolean.parseBoolean(envVars.getOrDefault("USE_SSL", "false"));
        String configs = envVars.getOrDefault("EVENT_CONFIG", "[{\"model\": \"CustodyAccount\", \"queueName\": \"modified-custodyAccount\" }, {\"model\": \"Instrument\", \"queueName\": \"modified-Instrument\" }]");
        eventConfigs = gson.fromJson(configs, EventConfig[].class);
        
		initRabbitMQ();
		subscribe("");
	}
	
	public static void initRabbitMQ() {
	    try {
	      ConnectionFactory factory = new ConnectionFactory();
	      factory.setHost(rabbitMQHost);
	      factory.setUsername(rabbitMQUsername);
	      factory.setPassword(rabbitMQPassword);
	      
	      if (useSSL) {
	    	  factory.useSslProtocol();
	      }

	      connection = factory.newConnection();
	      channel = connection.createChannel();

	      channel.queueDeclare("modified-custodyAccount", true, false, false, null);
	    }
	    catch  (Exception e) {
	      e.printStackTrace();
	    }
	}
	
	public static void sendMessage(String msg, String queueName) {
		try {
			channel.basicPublish("", queueName,
			        MessageProperties.PERSISTENT_TEXT_PLAIN,
			        msg.getBytes("UTF-8"));
			    System.out.println(" [x] Sent '" + msg + "'");
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public static void subscribe(String filter) throws IOException, TimeoutException {
		final Client client = Client.configure()
				.hostnames(couchbaseHost)
				.password(couchbasePassword)
				.bucket(couchbaseBucket)
				.build();
		
		// Don't do anything with control events in this example
		client.controlEventHandler(new ControlEventHandler() {
			@Override
			public void onEvent(ChannelFlowController cfc, ByteBuf arg1) {
				// TODO Auto-generated method stub
				
				
			}
		});

        // Print out Mutations and Deletions
        client.dataEventHandler(new DataEventHandler() {
			@Override
			public void onEvent(ChannelFlowController arg0, ByteBuf event) {
				if (DcpMutationMessage.is(event)) {
                    String key = DcpMutationMessage.key(event).toString(CharsetUtil.UTF_8);
                    // check for namespace
                    if (key.contains("::")) {
                    	key = key.split("::")[1];
                    } 
                    String model = key.split("\\|")[0];
                    String oid = key.split("\\|")[1];
                    
                    for(EventConfig config: eventConfigs) {
                    	if (model.equals(config.model)) {
                    		String msg = "{ \"oid\": \"" + oid + "\", \"host\": \"" + couchbaseHost + "\" }";
                        	sendMessage(msg, config.queueName);
                    	}
                    }
					
                }
                event.release();
			}
        });
        
        Runtime.getRuntime().addShutdownHook(new Thread()
        {
            @Override
            public void run()
            {
                System.out.println("Closing all the connections!");
                client.disconnect().await();
                try {
					channel.close();
					connection.close();
	                if (connection != null) {
	        	        try {
	        	          connection.close();
	        	        }
	        	        catch (Exception ignore) {
	        	        	
	        	        }
	        	    }
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (TimeoutException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }
        });

        // Connect the sockets
        client.connect().await();
		// Initialize the state (start now, never stop)
        client.initializeState(StreamFrom.BEGINNING, StreamTo.NOW).await();
        // Start streaming on all partitions
        client.startStreaming().await();
	}
}
