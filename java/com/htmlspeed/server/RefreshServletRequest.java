/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 28 December 2012
 */
package com.htmlspeed.server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

/**
 * RefreshServletRequest
 *
 * Passed to HtmlSpeedServlet.service when refreshing a state-less page.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.4 $$Date: 2013/07/25 06:55:57 $
 */
public class RefreshServletRequest implements HttpServletRequest
{
    private HashMap<String, Enumeration<String>> _headers = new HashMap<String, Enumeration<String>>();

    /**
     * To be returned by getRequestURL.
     */
    private StringBuffer _url;

    /**
     * To be returned by getRequestURI.
     */
    private String _uri;

    /**
     * To be returned as value of "Host" header.
     */
    private String _host;

    /**
     * To be returned by getQueryString.
     */
    private String _queryString;

    /**
     * CONSTUCTOR
     *
     * @param url full url of refreshed page
     */
    public RefreshServletRequest(String url)
    {
        _url = new StringBuffer(url);

        int dSlash = _url.indexOf("//");
        int sSlash = _url.indexOf("/", dSlash+2);
        String uri = _url.substring(sSlash);

        _host = _url.substring(dSlash+2, sSlash);

        int index = uri.indexOf('?');

        if (index < 0)
        {
            _uri = uri;
            _queryString = null;
        }
        else
        {
            _uri = uri.substring(0, index);
            _queryString = uri.substring(index + 1);
        }

        // Initializing request-header:
        _headers.put("User-Agent", Collections.enumeration(Arrays.asList(new String[] {
                            "Mozilla/5.0 (X11; Linux i686 on x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1"})));
        _headers.put("Accept", Collections.enumeration(Arrays.asList(new String[] {
                            "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8"})));
        _headers.put("Accept-Language", Collections.enumeration(Arrays.asList(new String[] {
                            "he-IL,he;q=0.8,en-US;q=0.6,en;q=0.4"})));
        _headers.put("Accept-Encoding", Collections.enumeration(Arrays.asList(new String[] {
                            "gzip,deflate"})));
        _headers.put("Accept-Charset", Collections.enumeration(Arrays.asList(new String[] {
                            "ISO-8859-1,utf-8;q=0.7,*;q=0.7"})));
        _headers.put("Connection", Collections.enumeration(Arrays.asList(new String[] {
                            "keep-alive"})));
        _headers.put("Host", Collections.enumeration(Arrays.asList(new String[] {
                            ""})));

    }

    @Override
    public Object getAttribute(String string) {return null;}

    @Override
    public Enumeration<String> getAttributeNames() {return null;}

    @Override
    public String getCharacterEncoding() {return null;}

    @Override
    public void setCharacterEncoding(String string) throws UnsupportedEncodingException {}

    @Override
    public int getContentLength() {return 0;}

    @Override
    public String getContentType() {return null;}

    @Override
    public ServletInputStream getInputStream() throws IOException {return null;}

    @Override
    public String getParameter(String string) {return null;}

    @Override
    public Enumeration<String> getParameterNames() {return null;}

    @Override
    public String[] getParameterValues(String string) {return null;}

    @Override
    public Map<String, String[]> getParameterMap() {return null;}

    @Override
    public String getProtocol()
    {
        return "HTTP/1.1";
    }

    @Override
    public String getScheme()
    {
        return "http";
    }

    @Override
    public String getServerName() {return null;}

    @Override
    public int getServerPort() {return 0;}

    @Override
    public BufferedReader getReader() throws IOException {return null;}

    @Override
    public String getRemoteAddr()
    {
        return "127.0.0.1-Refresh";
    }

    @Override
    public String getRemoteHost() {return null;}

    @Override
    public void setAttribute(String string, Object o) {}

    @Override
    public void removeAttribute(String string){}

    @Override
    public Locale getLocale() {return null;}

    @Override
    public Enumeration<Locale> getLocales() {return null;}

    @Override
    public boolean isSecure() {return false;}

    @Override
    public RequestDispatcher getRequestDispatcher(String string) {return null;}

    @Override
    public String getRealPath(String string) {return null;}

    @Override
    public int getRemotePort() {return 0;}

    @Override
    public String getLocalName() {return null;}

    @Override
    public String getLocalAddr() {return null;}

    @Override
    public int getLocalPort() {return 0;}

    @Override
    public ServletContext getServletContext() {return null;}

    @Override
    public AsyncContext startAsync() throws IllegalStateException {return null;}

    @Override
    public AsyncContext startAsync(ServletRequest sr, ServletResponse sr1) throws IllegalStateException {return null;}

    @Override
    public boolean isAsyncStarted() {return false;}

    @Override
    public boolean isAsyncSupported() {return false;}

    @Override
    public AsyncContext getAsyncContext() {return null;}

    @Override
    public DispatcherType getDispatcherType() {return null;}

    @Override
    public String getAuthType() {return null;}

    @Override
    public Cookie[] getCookies() {return null;}

    @Override
    public long getDateHeader(String string) {return 0;}

    @Override
    public String getHeader(String name)
    {
        if (name.equalsIgnoreCase("host"))
            return _host;

        Enumeration<String> values = _headers.get(name);
        if (values != null && values.hasMoreElements())
            return values.nextElement();
        return null;
    }

    @Override
    public Enumeration<String> getHeaders(String name)
    {
        return _headers.get(name);
    }

    @Override
    public Enumeration<String> getHeaderNames()
    {
        return Collections.enumeration(_headers.keySet());
    }

    @Override
    public int getIntHeader(String string) {return 0;}

    @Override
    public String getMethod()
    {
        return "GET";
    }

    @Override
    public String getPathInfo() {return null;}

    @Override
    public String getPathTranslated() {return null;}

    @Override
    public String getContextPath() {return null;}

    @Override
    public String getQueryString()
    {
        return _queryString;
    }

    @Override
    public String getRemoteUser() {return null;}

    @Override
    public boolean isUserInRole(String string) {return false;}

    @Override
    public Principal getUserPrincipal() {return null;}

    @Override
    public String getRequestedSessionId() {return null;}

    @Override
    public String getRequestURI()
    {
        return _uri;
    }

    @Override
    public StringBuffer getRequestURL()
    {
        return _url;
    }

    @Override
    public String getServletPath() {return null;}

    @Override
    public HttpSession getSession(boolean bln) {return null;}

    @Override
    public HttpSession getSession() {return null;}

    @Override
    public boolean isRequestedSessionIdValid() {return false;}

    @Override
    public boolean isRequestedSessionIdFromCookie() {return false;}

    @Override
    public boolean isRequestedSessionIdFromURL() {return false;}

    @Override
    public boolean isRequestedSessionIdFromUrl() {return false;}

    @Override
    public boolean authenticate(HttpServletResponse hsr) throws IOException, ServletException {return false;}

    @Override
    public void login(String string, String string1) throws ServletException {}

    @Override
    public void logout() throws ServletException {}

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {return null;}

    @Override
    public Part getPart(String string) throws IOException, ServletException {return null;}

}
