package io.core9.server.undertow;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Method;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.server.request.Response;
import io.undertow.server.HttpServerExchange;

import java.util.List;
import java.util.Map;

public class RequestImpl implements Request {
	
	private final HttpServerExchange exchange;
	private final VirtualHost vhost;
	private ResponseImpl response;
	
	public RequestImpl(VirtualHost vhost, HttpServerExchange exchange) {
		this.exchange = exchange;
		this.vhost = vhost;
	}

	@Override
	public String getPath() {
		return exchange.getRequestPath();
	}

	@Override
	public Map<String, Object> getParams() {
		return null;
	}

	@Override
	public Response getResponse() {
		if(response != null) {
			return response;
		}
		return response = new ResponseImpl(this.vhost, this.exchange);
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(exchange.getRequestMethod().toString());
	}

	@Override
	public String getBody() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getBodyAsMap() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Object> getBodyAsList() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Map<String, Object> getContext() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R getContext(String name, R defaultVal) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R getContext(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public <R> R putContext(String name, R value) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public VirtualHost getVirtualHost() {
		return vhost;
	}

	@Override
	public Cookie getCookie(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public List<Cookie> getAllCookies(String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void setCookies(List<Cookie> cookies) {
		// TODO Auto-generated method stub

	}

	@Override
	public String getHostname() {
		return exchange.getHostAndPort();
	}

}
