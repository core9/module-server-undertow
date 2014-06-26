package io.core9.server.undertow;

import io.core9.plugin.server.Cookie;
import io.core9.plugin.server.VirtualHost;
import io.core9.plugin.server.request.Method;
import io.core9.plugin.server.request.Request;
import io.core9.plugin.server.request.Response;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;

import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import net.minidev.json.JSONArray;
import net.minidev.json.JSONObject;
import net.minidev.json.JSONValue;
import rx.Observable;
import rx.Observable.OnSubscribe;
import rx.schedulers.Schedulers;

import com.google.common.io.CharStreams;

public class RequestImpl implements Request {
	
	private final HttpServerExchange exchange;
	private final VirtualHost vhost;
	private Map<String,Object> params;
	private Map<String,Object> context;
	private ResponseImpl response;
	
	private String strBody;
	
	private Observable<String> body = Observable.create( (OnSubscribe<String>) subscriber -> {
		if(strBody != null) {
			subscriber.onNext(strBody);
		} else {
			if(exchange.isInIoThread()) {
				subscriber.onError(new IllegalStateException("Blocking operation is in IO thread, not allowed!"));
			} else {
				exchange.startBlocking();
			}
			try {
				this.strBody = CharStreams.toString(new InputStreamReader(exchange.getInputStream()));
				subscriber.onNext(strBody);
			} catch (Exception e) {
				subscriber.onError(e);
			}
		}
		subscriber.onCompleted();
	}).subscribeOn(Schedulers.io());
	
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
		if(params != null) {
			return params;
		}
		return params = new HashMap<>();
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
	public Observable<String> getBody() {
		return this.body;
	}

	@Override
	public Observable<Map<String, Object>> getBodyAsMap() {
		return Observable.create((OnSubscribe<Map<String,Object>>) subscriber -> {
			this.getBody().subscribe((value) -> {
				try {
					HeaderValues contentTypes = exchange.getRequestHeaders().get("Content-Type");
					if(contentTypes == null) {
						subscriber.onError(new UnsupportedEncodingException("No content-type header set"));
					}
					boolean json = false;
					for(String contentType : contentTypes) {
						if(contentType.contains("application/json")) {
							json = true;
						}
					}
					if(json) {
						subscriber.onNext(((JSONObject) JSONValue.parse(value)));
					} else {
						subscriber.onNext(splitQuery(value));
					}
					subscriber.onCompleted();
				} catch (Exception e) {
					subscriber.onError(e);
				}
			});
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Observable<List<Object>> getBodyAsList() {
		return Observable.create((OnSubscribe<List<Object>>) subscriber -> {
			this.getBody().subscribe((value) -> {
				try {
					subscriber.onNext((JSONArray) JSONValue.parse(value));
				} catch (Exception e) {
					subscriber.onError(e);
				}
			});
		}).subscribeOn(Schedulers.io());
	}

	@Override
	public Map<String, Object> getContext() {
		if(context != null) {
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
		if(!exchange.getResponseCookies().containsKey(name)) {
			return null;
		}
		return new CookieImpl(exchange.getResponseCookies().get(name));
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
		for(Cookie cookie : cookies) {
			exchange.setResponseCookie(((CookieImpl) cookie).getServerCookie());
		}
	}

	@Override
	public String getHostname() {
		return exchange.getHostAndPort();
	}
	
	@SuppressWarnings("unchecked")
    private static Map<String, Object> splitQuery(String query) {
		if(query == null || query.length() == 0) return new HashMap<String, Object>();
		String[] pairs = query.split("&");
		Map<String, Object> query_pairs = new HashMap<String, Object>();
		for (String pair : pairs) {
			int idx = pair.indexOf("=");
			try {
				String key = URLDecoder.decode(pair.substring(0, idx), "UTF-8");
				String value = URLDecoder.decode(pair.substring(idx + 1), "UTF-8");
				Set<String> queryKeys = query_pairs.keySet();
				
				if(!queryKeys.isEmpty() && queryKeys.contains(key)){
					ArrayList<String> paramList = new ArrayList<>();
					Object list = query_pairs.get(key);
					if(list instanceof String){
						paramList.add(value);
					}else if(list instanceof ArrayList){
						paramList.addAll((Collection<? extends String>) query_pairs.get(key));
						paramList.add(value);
					} 
					query_pairs.put(key, paramList);
				}else{
					query_pairs.put(key,value);
				}
				
				
			} catch (UnsupportedEncodingException e) {
				return new HashMap<>();
			}
		}
		return query_pairs;
	}

}
