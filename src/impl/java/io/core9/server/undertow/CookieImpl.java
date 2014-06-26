package io.core9.server.undertow;

import java.util.Set;

public class CookieImpl implements io.core9.plugin.server.Cookie {
	
	private io.undertow.server.handlers.Cookie cookie;
	
	public io.undertow.server.handlers.Cookie getServerCookie() {
		return cookie;
	}

	@Override
	public String getName() {
		return cookie.getName();
	}

	@Override
	public String getValue() {
		return cookie.getValue();
	}

	@Override
	public void setValue(String value) {
		cookie.setValue(value);
	}

	@Override
	public String getDomain() {
		return cookie.getDomain();
	}

	@Override
	public void setDomain(String domain) {
		cookie.setDomain(domain);
	}

	@Override
	public String getPath() {
		return cookie.getPath();
	}

	@Override
	public void setPath(String path) {
		cookie.setPath(path);
	}

	@Override
	public String getComment() {
		return cookie.getComment();
	}

	@Override
	public void setComment(String comment) {
		cookie.setComment(comment);
	}

	@Override
	public long getMaxAge() {
		return cookie.getMaxAge();
	}

	@Override
	public void setMaxAge(long maxAge) {
		if(maxAge > Integer.MAX_VALUE || maxAge < Integer.MIN_VALUE) {
			throw new IllegalArgumentException
            (maxAge + " cannot be cast to int without changing its value.");
		}
		cookie.setMaxAge((int) maxAge);
	}

	@Override
	public int getVersion() {
		return cookie.getVersion();
	}

	@Override
	public void setVersion(int version) {
		cookie.setVersion(version);
	}

	@Override
	public boolean isSecure() {
		return cookie.isSecure();
	}

	@Override
	public void setSecure(boolean secure) {
		cookie.setSecure(secure);
	}

	@Override
	public boolean isHttpOnly() {
		return cookie.isHttpOnly();
	}

	@Override
	public void setHttpOnly(boolean httpOnly) {
		cookie.setHttpOnly(httpOnly);
	}

	@Override
	public String getCommentUrl() {
		throw new UnsupportedOperationException("Comment URL is not implemented in Undertow Cookies");
	}

	@Override
	public void setCommentUrl(String commentUrl) {
		throw new UnsupportedOperationException("Comment URL is not implemented in Undertow Cookies");
	}

	@Override
	public boolean isDiscard() {
		return cookie.isDiscard();
	}

	@Override
	public void setDiscard(boolean discard) {
		cookie.setDiscard(discard);
	}

	@Override
	public Set<Integer> getPorts() {
		throw new UnsupportedOperationException("Ports is not implemented in Undertow Cookies");
	}

	@Override
	public void setPorts(int... ports) {
		throw new UnsupportedOperationException("Comment URL is not implemented in Undertow Cookies");
	}

	@Override
	public void setPorts(Iterable<Integer> ports) {
		throw new UnsupportedOperationException("Comment URL is not implemented in Undertow Cookies");
	}
	
	public CookieImpl(io.undertow.server.handlers.Cookie cookie) {
		this.cookie = cookie;
	}

}
