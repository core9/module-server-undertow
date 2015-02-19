package io.core9.server.undertow;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Response;
import io.core9.plugin.template.TemplateEngine;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.Headers;
import io.undertow.util.HttpString;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minidev.json.JSONValue;

public class ResponseImpl implements Response {
	private final VirtualHost vhost;
	private static TemplateEngine<String> templateEngine;
	private HttpServerExchange exchange;
	private String template;
	private final Map<String, Object> values = new HashMap<String, Object>();
	private final Map<String, Object> globals = new HashMap<String, Object>();
	private boolean ended = false;

	public static void setTemplateEngine(TemplateEngine<String> engine) {
		templateEngine = engine;
	}

	@Override
	public String getTemplate() {
		return template;
	}

	@Override
	public Response setTemplate(String template) {
		this.template = template;
		return this;
	}

	@Override
	public Map<String, Object> getValues() {
		return values;
	}

	@Override
	public Response addValue(String key, Object value) {
		this.values.put(key, value);
		return this;
	}

	@Override
	public Response addValues(Map<String, Object> values) {
		this.values.putAll(values);
		return this;
	}

	@Override
	public Response addGlobal(String key, Object value) {
		this.globals.put(key, value);
		return this;
	}

	@Override
	public Map<String, Object> getGlobals() {
		return globals;
	}

	@Override
	public Response setStatusCode(int code) {
		exchange.setResponseCode(code);
		return this;
	}

	@Override
	public Response setStatusMessage(String message) {
		exchange.getResponseSender().send(message);
		this.ended = true;
		return this;
	}

	@Override
	public int getStatusCode() {
		return exchange.getResponseCode();
	}

	@Override
	public void end(String chunk) {
		if (!this.ended) {
			exchange.getResponseSender().send(chunk);
			this.ended = true;
		}
	}

	@Override
	public void end() {
		if (!this.ended && this.template != null) {
			String result = "";
			try {
				result = processTemplate();
			} catch (Exception e) {
				result = e.getMessage();
			}
			this.end(result);
		}
	}

	@Override
	public Response sendFile(String filename) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Response sendBinary(byte[] data) {
		this.exchange.getResponseSender().send(ByteBuffer.wrap(data));
		this.ended = true;
		return this;
	}

	@Override
	public void sendJsonMap(Map<String, Object> map) {
		end(JSONValue.toJSONString(map));
	}

	@Override
	public void sendJsonArray(List<? extends Object> list) {
		end(JSONValue.toJSONString(list));
	}

	@Override
	public void sendJsonArray(Set<? extends Object> list) {
		end(JSONValue.toJSONString(list));
	}

	@Override
	public void sendRedirect(int status, String url) {
		this.exchange.setResponseCode(status);
		this.exchange.getResponseHeaders().put(Headers.LOCATION, url);
		this.exchange.endExchange();
		this.ended = true;
	}

	@Override
	public Response putHeader(String name, String value) {
		this.exchange.getResponseHeaders().put(new HttpString(name), value);
		return this;
	}

	@Override
	public Response addCookie(Cookie cookie) {
		this.exchange.getResponseCookies().put(cookie.getName(), ((CookieImpl) cookie).getServerCookie());
		return this;
	}

	@Override
	public boolean isEnded() {
		return ended;
	}

	@Override
	public void setEnded(boolean ended) {
		this.ended = ended;
	}

	public ResponseImpl(VirtualHost vhost, HttpServerExchange exchange) {
		this.vhost = vhost;
		this.exchange = exchange;
		this.addGlobal("hostname", exchange.getHostAndPort());
		this.addGlobal("path", exchange.getRequestPath());
		this.addGlobal("query", exchange.getQueryString());
	}

	private String processTemplate() throws Exception {
		String contentType = exchange.getResponseHeaders().getFirst("Content-Type");
		if (contentType == null) {
			// Default to text/html content type
			exchange.getResponseHeaders().add(new HttpString("Content-Type"), "text/html");
		}
		if (System.getProperty("GLOBAL_SERVER_VARS") != null) {
			if (System.getProperty("GLOBAL_SERVER_VARS").equals("true"))
				this.addGlobal("SERVER", new ResponseGlobalsImpl(exchange).getServerEnvironment());
		}
		return templateEngine.render(vhost, template, values, globals);
	}
}
