package com.jsb.server;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.jsb.bot.core.JSBBot;
import com.jsb.server.api.JSBBotResource;
import com.jsb.server.serializer.JSONObjectDeserializer;
import com.jsb.server.serializer.JSONObjectSerializer;

public class Webserver {
	
	private static Server server;
	
	public synchronized static void start() throws Exception {
		if(Webserver.server != null) {
			return;
		}
		
		JSONObject webserver = JSBBot.getConfig("webserver");
		
		Server server = new Server(webserver.getInt("port"));
		
		ServletContextHandler contextHandler = new ServletContextHandler();
		
		SimpleModule module = new SimpleModule();
		module.addDeserializer(JSONObject.class, new JSONObjectDeserializer());
		module.addSerializer(JSONObject.class, new JSONObjectSerializer());
		
		ObjectMapper mapper = new ObjectMapper();
		mapper.registerModule(module);
		
		ResourceConfig config = new ResourceConfig();
		config.register(JSBBotResource.class);
		
		contextHandler.addServlet(new ServletHolder(new ServletContainer(config)), "/api/*");
		
		server.setHandler(contextHandler);
		
		Webserver.server = server;
		
		server.start();
	}
	
	public static Server getServer() {
		return Webserver.server;
	}
}