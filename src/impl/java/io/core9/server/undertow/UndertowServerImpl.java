package io.core9.server.undertow;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.Server;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.template.TemplateEngine;
import io.undertow.Undertow;
import net.xeoh.plugins.base.annotations.PluginImplementation;
import net.xeoh.plugins.base.annotations.events.PluginLoaded;
import net.xeoh.plugins.base.annotations.injections.InjectPlugin;

import org.apache.log4j.Logger;

@PluginImplementation
public class UndertowServerImpl implements Server {
	
	private HostManager hostManager;
	
	@PluginLoaded
	public void setHostManager(HostManager hostManager) {
		this.hostManager = hostManager;
	}
	
	@InjectPlugin
	private TemplateEngine<String> engine;
	
	private static final Logger LOG = Logger.getLogger(UndertowServerImpl.class);
	
	private MiddlewareHandler handler = new MiddlewareHandler();

	private int port = 8080;

	@Override
	public void use(String pattern, Middleware middleware) {
		LOG.info("Registring middleware : " + middleware.toString() + " with pattern : " + pattern);
		handler.addMiddleware(pattern, middleware);
	}

	@Override
	public void use(VirtualHost vhost, String pattern, Middleware middleware) {
		LOG.info("Registring: " + pattern + " on vhost: " + vhost.getHostname());
		handler.addMiddleware(vhost, pattern, middleware);
	}
	
	@Override
	public void remove(VirtualHost vhost) {
		handler.remove(vhost);
	}

	@Override
	public void deregister(String pattern) {
		handler.deregister(pattern);
	}

	@Override
	public void deregister(VirtualHost vhost, String pattern) {
		handler.deregister(vhost, pattern);
	}
	
	@Override
	public Cookie newCookie(String name, String value) {
		return new CookieImpl(new io.undertow.server.handlers.CookieImpl(name, value));
	}

	@Override
	public Cookie newCookie(String name) {
		return new CookieImpl(new io.undertow.server.handlers.CookieImpl(name));
	}

	@Override
	public void execute() {
		if(System.getProperty("PORT") != null) {
			port = Integer.parseInt(System.getProperty("PORT")); 
		}
		if(handler != null) {
			handler.setHostManager(hostManager);
		}
		ResponseImpl.setTemplateEngine(engine);
		Undertow server = Undertow.builder()
				.setHandler(handler)
				.addHttpListener(port, "0.0.0.0")
				.build();
		server.start();
	}

	@Override
	public void addVirtualHost(VirtualHost vhost) {
		handler.addHost(vhost);
	}

	@Override
	public void removeVirtualHost(VirtualHost vhost) {
		handler.removeHost(vhost);
	}

}
