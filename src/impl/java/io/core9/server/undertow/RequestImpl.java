package io.core9.server.undertow;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.FileUpload;
import io.core9.plugin.server.request.Method;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.server.request.Response;
import io.undertow.server.HttpServerExchange;
import io.undertow.server.handlers.form.FormData;
import io.undertow.server.handlers.form.FormData.FormValue;
import io.undertow.server.handlers.form.FormDataParser;
import io.undertow.server.handlers.form.FormParserFactory;
import io.undertow.util.HeaderValues;
import io.undertow.util.Headers;

import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.schedulers.Schedulers;

import com.google.common.io.CharStreams;

public class RequestImpl implements Request {

	private static final String APPLICATION_JSON = "application/json";

	private HttpServerExchange exchange;
	private final VirtualHost vhost;
	private Map<String, Deque<String>> queryParams;
	private Map<String, String> pathParams;
	private Map<String, Object> context;
	private ResponseImpl response;

	private String strBody;
	private JSONObject mapBody;
	private JSONArray listBody;

	/**
	 * TODO: Tidy up (refactor)
	 */
	private Observable<String> body = Observable.create((OnSubscribe<String>) subscriber -> {
		if (strBody != null) {
			subscriber.onNext(strBody);
		} else {
			if (exchange.isInIoThread()) {
				subscriber.onError(new IllegalStateException("Blocking operation is in IO thread, not allowed!"));
			} else {
				exchange.startBlocking();
			}
			try {
				String mimeType = exchange.getRequestHeaders().getFirst(Headers.CONTENT_TYPE);
				if (mimeType != null && mimeType.startsWith(APPLICATION_JSON)) {
					Object tmpBody = JSONValue.parse(exchange.getInputStream());
					if (tmpBody instanceof JSONObject) {
						mapBody = (JSONObject) tmpBody;
						strBody = mapBody.toJSONString();
					} else if (tmpBody instanceof JSONArray) {
						listBody = (JSONArray) tmpBody;
						strBody = listBody.toJSONString();
					} else {
						throw new IllegalStateException("A non JSON value was emitted to a JSON parser");
					}
				} else {
					FormDataParser parser = FormParserFactory.builder().build().createParser(exchange);
					if (parser != null) {
						try {
							FormData data = parser.parseBlocking();
							mapBody = new JSONObject();
							data.forEach((key) -> {
								Deque<FormValue> values = data.get(key);
								List<FileUpload> files = new ArrayList<FileUpload>();
								for (FormValue value : values) {
									if (value.isFile()) {
										HeaderValues content = value.getHeaders().get(Headers.CONTENT_DISPOSITION);
										String folder = "/";
										for(String item : content) {
											int idx = item.indexOf("name=\"");
											if(idx != -1) {
												folder = item.substring(idx + 6);
												folder = folder.substring(0, folder.indexOf(";") -1);
											}
										};
										files.add(new FileUploadImpl(folder + value.getFileName(),
												value.getHeaders().getFirst(Headers.CONTENT_TYPE_STRING),
												value.getFile().getPath()));
									}
								}
								if (files.isEmpty()) {
									// FIXME getFirst(), what about the rest?
									mapBody.put(key, data.get(key).getFirst().getValue());
								} else {
									context.put("files", files);
								}
							});
							strBody = mapBody.toJSONString();
						} catch (Exception e) {
							e.printStackTrace();
						}
					} else {
						strBody = CharStreams.toString(new InputStreamReader(exchange.getInputStream()));
					}
				}
				subscriber.onNext(strBody);
			} catch (Exception e) {
				subscriber.onError(e);
			}
		subscriber.onCompleted();
		}
	}).subscribeOn(Schedulers.io());

	public RequestImpl(VirtualHost vhost, HttpServerExchange exchange) {
		this.exchange = exchange;
		this.vhost = vhost;
		this.queryParams = exchange.getQueryParameters();
	}

	public HttpServerExchange getExchange() {
		return exchange;
	}

	@Override
	public String getPath() {
		return exchange.getRequestPath();
	}

	@Override
	public Map<String, String> getPathParams() {
		if (pathParams != null) {
			return pathParams;
		}
		return pathParams = new HashMap<String, String>();
	}

	@Override
	public Response getResponse() {
		if (response != null) {
			return response;
		}
		return response = new ResponseImpl(this.vhost, this.exchange);
	}

	@Override
	public Method getMethod() {
		return Method.valueOf(exchange.getRequestMethod().toString());
	}

	@Override
	public Observable<String> getBody() {
		return this.body;
	}

	@Override
	public Observable<Map<String, Object>> getBodyAsMap() {
		return Observable.create((OnSubscribe<Map<String, Object>>) subscriber -> {
			if(mapBody != null) {
				subscriber.onNext(mapBody);
				subscriber.onCompleted();
			} else {
				this.body.toBlocking().last();
				subscriber.onNext(mapBody);
				subscriber.onCompleted();
			}
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Observable<List<Object>> getBodyAsList() {
		return Observable.create((OnSubscribe<List<Object>>) subscriber -> {
			this.getBody().subscribe((value) -> {
				subscriber.onNext(listBody);
				subscriber.onCompleted();
			});
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Map<String, Object> getContext() {
		if (context != null) {
			return context;
		}
		return context = new HashMap<>();
	}

	@Override
	public <R> R getContext(String name, R defaultVal) {
		R result = getContext(name);
		return result == null ? defaultVal : result;
	}

	@SuppressWarnings("unchecked")
	@Override
	public <R> R getContext(String name) {
		return (R) getContext().get(name);
	}

	@Override
	public <R> R putContext(String name, R value) {
		getContext().put(name, value);
		return value;
	}

	@Override
	public VirtualHost getVirtualHost() {
		return vhost;
	}

	@Override
	public Cookie getCookie(String name) {
		if (!exchange.getRequestCookies().containsKey(name)) {
			return null;
		}
		return new CookieImpl(exchange.getRequestCookies().get(name));
	}

	@Override
	/**
	 * FIXME: returns only one cookie
	 */
	public List<Cookie> getAllCookies(String name) {
		List<Cookie> cookies = new ArrayList<Cookie>();
		cookies.add(new CookieImpl(exchange.getResponseCookies().get(name)));
		return cookies;
	}

	@Override
	public void setCookies(List<Cookie> cookies) {
		for (Cookie cookie : cookies) {
			exchange.setResponseCookie(((CookieImpl) cookie).getServerCookie());
		}
	}

	@Override
	public String getScheme() {
		return exchange.getRequestScheme();
	}

	@Override
	public String getHostname() {
		return exchange.getHostAndPort();
	}

	@Override
	public String getSourceHost() {
		return exchange.getSourceAddress().getHostName();
	}

	@Override
	public Map<String, Deque<String>> getQueryParams() {
		return queryParams;
	}

	@Override
	public Map<String, String> getParams() {
		Map<String, String> res = new HashMap<String, String>();
		for(String param : queryParams.keySet()){
			res.put(param, queryParams.get(param).getFirst());
		}
		return res;
	}

}
