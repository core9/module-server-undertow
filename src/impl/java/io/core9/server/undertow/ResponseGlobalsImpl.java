package io.core9.server.undertow;

import io.undertow.server.HttpServerExchange;

import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

public class ResponseGlobalsImpl implements ResponseGlobals {

	private HttpServerExchange exchange;

	public ResponseGlobalsImpl(HttpServerExchange exchange) {
		this.exchange = exchange;
	}

	@Override
	public Map<String, Object> getServerEnvironment() {
		String query = exchange.getQueryString();
		String url = exchange.getRequestURL();
		if(query != ""){
			url = url + "?" + query;
		}
		URI uri = null;
		try {
			uri = new URI(url);
		} catch (URISyntaxException e) {
			e.printStackTrace();
		}
		
		String[] uriParts = null;
		
		try{
			uriParts = uri.getPath().substring(1).split("/");
		}catch(Exception e){
			
		}
		
		String lasturipartnoext = "";
		try{
			lasturipartnoext = uriParts[uriParts.length - 1].split("\\.")[0];
		}catch(Exception e){
			
		}
		
		Map<String, Object> uriMap = new HashMap<String, Object>();
		uriMap.put("host", uri.getHost());
		uriMap.put("path", uri.getPath());
		uriMap.put("lasturipartnoext", lasturipartnoext);
		uriMap.put("uriparts", Arrays.asList(uriParts));
		uriMap.put("port", uri.getPort());
		uriMap.put("scheme", uri.getScheme());
		
		URL urli = null;
		try {
			urli = new URL(url);
		} catch (MalformedURLException e) {
			e.printStackTrace();
		}
		
		try {
			if(urli != null){
				uriMap.put("query", splitQuery(urli));				
			}else{
				uriMap.put("query", new HashMap<String, Object>());
			}

		} catch (UnsupportedEncodingException e) {
			e.printStackTrace();
		}
		

		return uriMap;
	}

	private Map<String, List<String>> splitQuery(URL url) throws UnsupportedEncodingException {
		  final Map<String, List<String>> query_pairs = new LinkedHashMap<String, List<String>>();
		  final String[] pairs = url.getQuery().split("&");
		  for (String pair : pairs) {
		    final int idx = pair.indexOf("=");
		    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
		    if (!query_pairs.containsKey(key)) {
		      query_pairs.put(key, new LinkedList<String>());
		    }
		    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
		    query_pairs.get(key).add(value);
		  }
		  return query_pairs;
		}
}
