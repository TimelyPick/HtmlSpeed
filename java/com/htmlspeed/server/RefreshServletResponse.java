/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 28 December 2012
 */
package com.htmlspeed.server;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Locale;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;

/**
 * RefreshServletResponse
 *
 * Passed to HtmlSpeedServlet.service when refreshing a state-less page.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.3 $$Date: 2012/12/28 11:43:20 $
 */
public class RefreshServletResponse implements HttpServletResponse
{

    private NullOutputStream _nullOutputStream = new NullOutputStream();

    @Override
    public String getCharacterEncoding() {return null;}

    @Override
    public String getContentType()
    {
        return "text/html";
    }

    @Override
    public ServletOutputStream getOutputStream() throws IOException
    {
        return _nullOutputStream;
    }

    @Override
    public PrintWriter getWriter() throws IOException {return null;}

    @Override
    public void setCharacterEncoding(String string) {}

    @Override
    public void setContentLength(int i) {}

    @Override
    public void setContentType(String string) {}

    @Override
    public void setBufferSize(int i) {}

    @Override
    public int getBufferSize() {return 0;}

    @Override
    public void flushBuffer() throws IOException {}

    @Override
    public void resetBuffer() {}

    @Override
    public boolean isCommitted() {return true;}

    @Override
    public void reset() {}

    @Override
    public void setLocale(Locale locale) {}

    @Override
    public Locale getLocale() {return null;}

    @Override
    public void addCookie(Cookie cookie) {}

    @Override
    public boolean containsHeader(String string) {return false;}

    @Override
    public String encodeURL(String string) {return null;}

    @Override
    public String encodeRedirectURL(String string) {return null;}

    @Override
    public String encodeUrl(String string) {return null;}

    @Override
    public String encodeRedirectUrl(String string) {return null;}

    @Override
    public void sendError(int i, String string) throws IOException {}

    @Override
    public void sendError(int i) throws IOException {}

    @Override
    public void sendRedirect(String string) throws IOException {}

    @Override
    public void setDateHeader(String string, long l) {}

    @Override
    public void addDateHeader(String string, long l) {}

    @Override
    public void setHeader(String string, String string1) {}

    @Override
    public void addHeader(String string, String string1) {}

    @Override
    public void setIntHeader(String string, int i) {}

    @Override
    public void addIntHeader(String string, int i) {}

    @Override
    public void setStatus(int i) {}

    @Override
    public void setStatus(int i, String string) {}

    @Override
    public int getStatus() {return 0;}

    @Override
    public String getHeader(String string) {return null;}

    @Override
    public Collection<String> getHeaders(String string) {return null;}

    @Override
    public Collection<String> getHeaderNames() {return null;}

    /**
     * Writes to null device.
     */
    private static class NullOutputStream extends ServletOutputStream
    {
        @Override
        public void write(int b) throws IOException {}

        @Override
        public void write(byte b[]) throws IOException {}

        @Override
        public void write(byte b[], int off, int len) throws IOException {}

        @Override
        public void flush() throws IOException {}

        @Override
        public void close() throws IOException {}

        @Override
        public void print(String s) throws IOException {}

        @Override
        public void print(boolean b) throws IOException {}

        @Override
        public void print(char c) throws IOException {}

        @Override
        public void print(int i) throws IOException {}

        @Override
        public void print(long l) throws IOException {}

        @Override
        public void print(float f) throws IOException {}

        @Override
        public void print(double d) throws IOException {}

        @Override
        public void println() throws IOException {}

        @Override
        public void println(String s) throws IOException {}

        @Override
        public void println(boolean b) throws IOException {}

        @Override
        public void println(char c) throws IOException {}

        @Override
        public void println(int i) throws IOException {}

        @Override
        public void println(long l) throws IOException {}

        @Override
        public void println(float f) throws IOException {}

        @Override
        public void println(double d) throws IOException {}
    }

}
