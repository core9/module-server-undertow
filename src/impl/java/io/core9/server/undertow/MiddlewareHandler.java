package io.core9.server.undertow;

import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Binding;
import io.core9.plugin.server.handler.Middleware;
import io.core9.plugin.server.request.Response;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

public class MiddlewareHandler implements HttpHandler {
	
	private static final List<Binding> BINDINGS = new ArrayList<Binding>();
	private static final Map<VirtualHost,List<Binding>> VHOST_BINDINGS = new HashMap<>();
	private static final Logger LOGGER = Logger.getLogger(MiddlewareHandler.class);
	private HostManager hostManager;
	private Map<String,VirtualHost> hosts = new HashMap<>();
	
	public void setHostManager(HostManager hostManager) {
		this.hostManager = hostManager;
	}
		
	public void addMiddleware(String pattern, Middleware middleware) {
		BINDINGS.add(createBinding(pattern, middleware));
	}
	
	public void addMiddleware(VirtualHost vhost, String pattern, Middleware middleware) {
		if(!VHOST_BINDINGS.containsKey(vhost)) {
			VHOST_BINDINGS.put(vhost, new CopyOnWriteArrayList<Binding>());
		}
		VHOST_BINDINGS.get(vhost).add(createBinding(pattern, middleware));
	}
	
	public void deregister(String pattern) {
		for(Binding binding : BINDINGS) {
			if(binding.getPath().equals(pattern)) {
				BINDINGS.remove(binding);
			}
		}
	}
	
	public void deregister(VirtualHost vhost, String pattern) {
		List<Binding> bindings = VHOST_BINDINGS.get(vhost);
		if(bindings != null) {
			for(Binding binding : bindings) {
				if(binding.getPath().equals(pattern)) {
					bindings.remove(binding);
				}
			}
		}
	}
	
	public void remove(VirtualHost vhost) {
		VHOST_BINDINGS.remove(vhost);
	}

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if(exchange.isInIoThread()) {
			exchange.dispatch(this);
			return;
		}
		LOGGER.info("Handling request on "
				+ "host: " + exchange.getHostAndPort() + ", "
				+ "path: " + exchange.getRequestPath() + ", "
				+ "client: " + exchange.getSourceAddress().getHostString());
		VirtualHost vhost = hosts.get(exchange.getHostAndPort());
		if(vhost == null) {
			RequestImpl req = new RequestImpl(null, exchange);
			for(Binding binding : BINDINGS) {
				binding.handle(req);
			}
			endRequest(req, null);
		} else {
			RequestImpl req = new RequestImpl(vhost, exchange);
			for(Binding binding : BINDINGS) {
				binding.handle(req);
			}
			for(Binding binding : VHOST_BINDINGS.get(req.getVirtualHost())) {
				binding.handle(req);
			}
			endRequest(req, vhost);
		}
	}
	
	private void endRequest(RequestImpl req, VirtualHost vhost) {
		Response response = req.getResponse();
		if(!response.isEnded()) {
			if(response.getTemplate() != null || response.getValues().size() > 0 || vhost == null) {
				response.end();
			} else {
				String alias = hostManager.getURLAlias(req.getVirtualHost(), req.getPath());
				if(alias != null) {
					response.sendRedirect(307, alias);
				} else {
					boolean hasNotFoundHandler = false;
					for(Binding binding : VHOST_BINDINGS.get(req.getVirtualHost())) {
						if(binding.getPath().equals("404")) {
							binding.getMiddleware().handle(req);
							hasNotFoundHandler = true;
							break;
						}
					}
					if(!hasNotFoundHandler) {
						response.end("Not found");
					}
				}
			}
		}
	}
	
	public static Binding createBinding(String pattern, Middleware middleware) {
		Matcher m = Pattern.compile(":([A-Za-z][A-Za-z0-9_-]*)").matcher(pattern);
		StringBuffer sb = new StringBuffer();
		Set<String> groups = new HashSet<>();
		while (m.find()) {
			String group = m.group().substring(1);
			if (groups.contains(group)) {
				throw new IllegalArgumentException("Cannot use identifier "
						+ group + " more than once in pattern string");
			}
			m.appendReplacement(sb, "(?<$1>[^\\/]+)");
			groups.add(group);
		}
		m.appendTail(sb);
		String regex = sb.toString();
		return new Binding(pattern, Pattern.compile(regex), groups, middleware);
	}

	public void addHost(VirtualHost vhost) {
		hosts.put(vhost.getHostname(), vhost);
	}
	
	public void removeHost(VirtualHost vhost) {
		VHOST_BINDINGS.remove(vhost);
		hosts.remove(vhost.getHostname());
	}
}
