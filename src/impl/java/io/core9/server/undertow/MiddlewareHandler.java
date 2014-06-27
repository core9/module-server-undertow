package io.core9.server.undertow;

import io.core9.plugin.server.HostManager;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.handler.Binding;
import io.core9.plugin.server.handler.Middleware;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.SameThreadExecutor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MiddlewareHandler implements HttpHandler {
	
	private static final List<Binding> BINDINGS = new ArrayList<Binding>();
	private static final Map<VirtualHost,List<Binding>> VHOST_BINDINGS = new HashMap<>();
	private Map<String,VirtualHost> hosts;
	
	public void setHostManager(HostManager hostManager) {
		this.hosts = hostManager.getVirtualHostsByHostname();
	}
		
	public void addMiddleware(String pattern, Middleware middleware) {
		BINDINGS.add(createBinding(pattern, middleware));
	}
	
	public void addMiddleware(VirtualHost vhost, String pattern, Middleware middleware) {
		if(!VHOST_BINDINGS.containsKey(vhost)) {
			VHOST_BINDINGS.put(vhost, new ArrayList<Binding>());
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

	@Override
	public void handleRequest(HttpServerExchange exchange) throws Exception {
		if(exchange.isInIoThread()) {
			exchange.dispatch(this);
			return;
		}
		VirtualHost vhost = hosts.get(exchange.getHostAndPort());
		RequestImpl req = new RequestImpl(vhost, exchange);
		for(Binding binding : VHOST_BINDINGS.get(req.getVirtualHost())) {
			binding.handle(req);
		}
		for(Binding binding : BINDINGS) {
			binding.handle(req);
		}
		req.getResponse().end();
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
}
