/*
 *  Copyright 2001 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 Feb 2012
 */
package com.htmlspeed.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpExchange;
import org.eclipse.jetty.io.Buffer;
import org.eclipse.jetty.io.EofException;

/**
 * HtmlSpeedServlet.
 *
 * Handles async http services.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.54 $$Date: 2013/08/21 09:55:51 $
 */
public class HtmlSpeedHttpExchange extends HttpExchange
{

    public long creationTime = System.currentTimeMillis();

    private LoadLock _lock;

    private OutputStream _out;

    private static final byte[] EMPTY = new byte[0];
    byte[] _gzipedBytes = EMPTY;
    byte[] _bytes = EMPTY;

    /**
     * Information regarding calling http-service (null when not used by http-service).
     */
    private ServiceContext _context;

    /**
     * Value of passed Host http header
     */
    private String _host;

    private final static HashSet<String> _dropedHeaders = new HashSet<String>();

    static
    {
        _dropedHeaders.add("proxy-connection");
        _dropedHeaders.add("connection");
        _dropedHeaders.add("keep-alive");
        _dropedHeaders.add("transfer-encoding");
        _dropedHeaders.add("te");
        _dropedHeaders.add("trailer");
        _dropedHeaders.add("proxy-authorization");
        _dropedHeaders.add("proxy-authenticate");
        _dropedHeaders.add("upgrade");
    }

    private ArrayList<String> _responseHeaders;
    private int _responseStatus = 0;
    private String _responseReason = null;

    private boolean _isCommFailure = false; // True when communication with the content-server has failed.

    private int _cacheControlIndex = (-1);
    private int _expiresIndex = (-1);
    private int _pragmaIndex = (-1);
    private int _etagIndex = (-1);
    private int _lastModifiedIndex = (-1);

    private int _contentTypeIndex = (-1); // Index in _responseHeaders of "Content-Type".

    private int _contentLengthIndex = (-1); // Index in _responseHeaders of "Content-Length".

    private int[] _setCookieIndexes = null; // Indexes in _responseHeaders of "Set-Cookie" headers (-1 entry means end).
    private int _setCookieCount = 0; // Number of Set-Cookie headers

    private int _contentEncodingIndex = (-1); // Less than 0 until header "Content-Encoding" is processed.
    private boolean _isGziped = false; // True when value of "Content-Encoding" in response is "gzip"
    private boolean _isDeflated = false; // True hwne value of "Content-Encoding" in response is "defalte".

    private String _contentServerAddress; // The address of accessed content-server.

    boolean _isHtml = false; // True when configData.removeFromHtml is not null and http header Content-Type contains "text/html".

    boolean _isCss = false; // True when configData.removeFromCss is not null and http header Content-Type contains "text/css".

    private static final byte[] IFRAME_DOMAIN_TOO = "document.domain=document.getElementById('htmlSpeedIfrm').contentWindow.".getBytes();

    /**
     * Index of suffix of context.url in _fileSuffixes
     */
    private int _indexOfFileSuffix = (-1);

    /**
     * True when matched patterns are replaced in html pages.
     */
    private boolean _isReplacing = false;

    /**
     * The string to be replaced in html pages when followed
     * by a near dest string (property: replace.src).
     *
     * Replaced string in html pages.
     */
    private byte[] _replaceSrc = null;

    /**
     * Number of characters from start of dest string that are skipped
     * when replacing the src string (property: replace.dst.skip).
     */
    private int _replaceDstSkipLen = 0;

    /**
     * The start (prefix) of the replacing string (property: replace.dst.prefix).
     */
    private byte[] _replaceDstPrefix = null;

    /**
     * The end (suffix) of the replacing string (property: replace.dst.suffix).
     */
    private byte[] _replaceDstSuffix = null;

    /**
     * String to insert before the replaced string (property: replace.before).
     */
    private byte[] _replaceBefore = null;

    /**
     * String to insert after the replaced string (property: replace.after).
     */
    private byte[] _replaceAfter = null;

    public void setHtmlReplaceParams()
    {
        ConfigData configData = _context.configData;
        _isReplacing = configData.defIsReplacing;
        _replaceSrc = configData.defReplaceSrc;
        _replaceDstSkipLen = configData.defReplaceDstSkipLen;
        _replaceDstPrefix = configData.defReplaceDstPrefix;
        _replaceDstSuffix = configData.defReplaceDstSuffix;
        _replaceBefore = configData.defReplaceBefore;
        _replaceAfter = configData.defReplaceAfter;
    }

    @Override
    public void setRequestURI(String uri)
    {
        super.setRequestURI(uri);

        _indexOfFileSuffix = (-1);
        _isReplacing = false;

        ConfigData configData = _context.configData;

        if (configData.fileSuffixes != null)
        {
            for (int i = 0 ; i < configData.fileSuffixes.length ; i++)
            {
                if (uri.endsWith(configData.fileSuffixes[i]))
                {
                    _indexOfFileSuffix = i;
                    _isReplacing = true;
                    _replaceSrc = configData.fileReplaceSrcs[i];
                    _replaceDstSkipLen = configData.fileReplaceDstSkipLens[i];
                    _replaceDstPrefix = configData.fileReplaceDstPrefixes[i];
                    _replaceDstSuffix = configData.fileReplaceDstSuffixes[i];
                    _replaceBefore = configData.fileReplaceBefores[i];
                    _replaceAfter = configData.fileReplaceAfters[i];
                    break;
                }
            }
        }

        if (_indexOfFileSuffix >= 0 && !(_out instanceof ByteArrayOutputStream))
        {
            _out = new ByteArrayOutputStream(1000);
        }
    }

    /**
     * Constructor
     *
     * When service is completed lock.decCount() is called.
     *
     * @param lock for notifying the servicing thread when http-service is finished
     * @param context info ragarding calling http-service (null when not called by a http-service)
     */
    public HtmlSpeedHttpExchange(LoadLock lock, ServiceContext context) throws IOException
    {
        _lock = lock;
        _context = context;

        if (_context != null && _context.isRouter)
        {
            _out = _context.response.getOutputStream();
        }
        else
        {
            _out = new ByteArrayOutputStream(1000);
        }
        _responseHeaders = new ArrayList<String>(32);
    }

    /**
     * @return ServiceContext instance that was passed to constructor
     */
    public ServiceContext getContext()
    {
        return _context;
    }

    /**
     * @return value of passed Host http-header
     */
    public String getHost()
    {
        return _host;
    }

    public void setContentServerAddress(String address)
    {
        _contentServerAddress = address;
    }

    public String getContentServerAddress()
    {
        return _contentServerAddress;
    }
    
    public int getResponseStatus()
    {
        return _responseStatus;
    }

    public String getResponseReason()
    {
        return _responseReason;
    }

    /**
     * @return true when communication with content-server has failed
     */
    public boolean isCommFailure()
    {
        return _isCommFailure;
    }

    public String[] getResponseHeaders()
    {
        return _responseHeaders.toArray(new String[_responseHeaders.size()]);
    }

    /**
     * @return the index in getResponseHeaders() of "Cache-Control" header (when not negative ).
     */
    public int getCacheControlIndex()
    {
        return _cacheControlIndex;
    }

    /**
     * @return the index in getResponseHeaders() of "Expires" header (when not negative ).
     */
    public int getExpiresIndex()
    {
        return _expiresIndex;
    }

    /**
     * @return the index in getResponseHeaders() of "Pragma" header (when not negative ).
     */
    public int getPragmaIndex()
    {
        return _pragmaIndex;
    }

    /**
     * @return the index in getResponseHeaders() of "ETag" header (when not negative ).
     */
    public int getEtagIndex()
    {
        return _etagIndex;
    }

    /**
     * @return the index in getResponseHeaders() of "Last-Modified" header (when not negative ).
     */
    public int getLastModifiedIndex()
    {
        return _lastModifiedIndex;
    }

    /**
     * When this method returns i, then the value of "Content-Type" header is stored at
     * the i+1'th element of response-headers-array (returned by getResponseHeaders)
     *
     * If the value of "Content-Type" header starts with
     * "text/html" then the resource is an html web-page.
     *
     * @return the index in response-headers array of "Content-Type" header
     */
    public int getContentTypeIndex()
    {
        return _contentTypeIndex;
    }

    /**
     * @return the indexes in getResponseHeaders() of "Set-Cookie" headers (when not null).
     */
    public int[] getSetCookieIndexes()
    {
        return _setCookieIndexes;
    }

    /**
     * @return true when value of "Content-Encoding" header (in the response) is "gzip".
     */
    public boolean isGziped()
    {
        return _isGziped;
    }

    /**
     * @return true when value of "Content-Encoding" header (in the response) is "defelate.
     */
    public boolean isDeflated()
    {
        return _isDeflated;
    }

    public byte[] getResponseContent()
    {
        if (_isGziped || _isDeflated)
            return _gzipedBytes;
        else
            return _bytes;
    }

    /**
     * @return the ungziped content of the response
     */
    public byte[] getUngzipedResponseContent()
    {
        if ((_isGziped || _isDeflated) && _bytes == EMPTY)
            ungzipResponse();

        return _bytes;
    }

    /**
     * Replaces in response:
     *      document.domain=  by
     *      document.domain=document.getElementById('htmlSpeedIfrm').contentWindow.document.domain=
     */
    public void handleLocationUpdates()
    {
        getUngzipedResponseContent(); // Scanning ungipzed response.
        if (_bytes == null || _bytes.length < 18)
            return;

        ByteArrayOutputStream baos = new ByteArrayOutputStream(_bytes.length + 20);
        int bytesOffset = 0;

        // Search for "document.domain=":
        for (int i = 0 ; i+17 < _bytes.length ; i++)
        {
            if (_bytes[i] == 'd' && _bytes[i+1] == 'o' && _bytes[i+2] == 'c' && _bytes[i+3] == 'u' &&
                        _bytes[i+4] == 'm' && _bytes[i+5] == 'e' && _bytes[i+6] == 'n' && _bytes[i+7] == 't' &&
                        _bytes[i+8] == '.' && _bytes[i+9] == 'd' && _bytes[i+10] == 'o' && _bytes[i+11] == 'm' &&
                        _bytes[i+12] == 'a' && _bytes[i+13] == 'i' && _bytes[i+14] == 'n' &&
                        (( _bytes[i+15] == '=' && _bytes[i+16] != '=') ||
                            (_bytes[i+15] == ' ' && _bytes[i+16] == '=' && _bytes[i+17] != '=')))
            {
                // Assigning also to iframe's domain:
                int len = i - bytesOffset;
                baos.write(_bytes, bytesOffset, len);
                bytesOffset += len;
                baos.write(IFRAME_DOMAIN_TOO, 0, IFRAME_DOMAIN_TOO.length);
                i = i + (_bytes[i+15] == ' ' ? 17 : 16);
                continue;
            }
        }

        if (baos.size() > 0)
        {
            // Found:
            int len = _bytes.length - bytesOffset;
            baos.write(_bytes, bytesOffset, len);
            bytesOffset += len;
            _bytes = baos.toByteArray();

            // Gzipping _bytes:
            try
            {
                ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(_bytes.length*3/4);
                OutputStream oGzip = new GZIPOutputStream(oByteArr);
                oGzip.write(_bytes);
                oGzip.close();
                _gzipedBytes = oByteArr.toByteArray();
                _isGziped = true;
                _isDeflated = false;
            }
            catch (IOException exc)
            {
                _gzipedBytes = null;
                _isGziped = false;
                _isDeflated = false;
            }
        }
    }

    /**
     * Removes each occurence of removeFromHtml.
     *
     * When isRouter the altered response is writen to content.response
     */
    public void removeFromHtml() throws IOException
    {
        byte[] removeFromHtml = _context.configData.removeFromHtml;

        if (removeFromHtml == null || removeFromHtml.length == 0 || (!_isHtml))
            return;

        getUngzipedResponseContent(); // Scanning ungipzed response.

        if (_responseStatus == 200 && _bytes != null && _bytes.length > removeFromHtml.length)
        {

            ByteArrayOutputStream baos = null; // _bytes after removing all occurences of removeFromHtml
            int bytesOffset = 0; // Current offset in _bytes

            // Search for removeFromHtml:
            int removeLen = removeFromHtml.length;
            for (int i = 0 ; i + removeLen < _bytes.length ; i++)
            {
                if (_bytes[i] == removeFromHtml[0])
                {
                    boolean found = true;

                    for (int j = 1 ; j < removeLen ; j++)
                    {
                        if (_bytes[i + j] != removeFromHtml[j])
                        {
                            found = false;
                            break;
                        }
                    }

                    if (found)
                    {
                        // Assigning also to iframe's domain:
                        int len = i - bytesOffset;
                        if (baos == null)
                            baos = new ByteArrayOutputStream(_bytes.length);
                        baos.write(_bytes, bytesOffset, len);
                        bytesOffset += len;
                        bytesOffset += removeLen;
                        i = bytesOffset;
                    }
                    continue;
                }
            }

            if (baos != null)
            {
                // Found removeFromHtml:
                int len = _bytes.length - bytesOffset;
                baos.write(_bytes, bytesOffset, len);
                bytesOffset += len;
                _bytes = baos.toByteArray();

                // Gzipping modified _bytes:
                if (_isGziped || _isDeflated)
                {
                    try
                    {
                        ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(_bytes.length*3/4);
                        OutputStream oGzip = new GZIPOutputStream(oByteArr);
                        oGzip.write(_bytes);
                        oGzip.close();
                        _gzipedBytes = oByteArr.toByteArray();
                        _isGziped = true;
                        _isDeflated = false;
                    }
                    catch (IOException exc)
                    {
                        _gzipedBytes = null;
                        _isGziped = false;
                        _isDeflated = false;
                        throw exc;
                    }
                }
            }
        }

        // When isRouter then writing full response:
        if (_context.isRouter && _bytes != null && _bytes.length > 0)
        {
            OutputStream out = _context.response.getOutputStream();
            if (_isGziped || _isDeflated)
                out.write(_gzipedBytes);
            else
                out.write(_bytes);
        }
    }

    /**
     * Removes each occurence of configData.removeFromCss.
     *
     * When isRouter the altered response is writen to content.response
     */
    public void removeFromCss() throws IOException
    {
        byte[] removeFromCss = _context.configData.removeFromCss;

        if (removeFromCss == null || removeFromCss.length == 0 || (!_isCss))
            return;

        getUngzipedResponseContent(); // Scanning ungipzed response.

        if (_responseStatus == 200 && _bytes != null && _bytes.length > removeFromCss.length)
        {

            ByteArrayOutputStream baos = null; // _bytes after removing all occurences of removeFromCss
            int bytesOffset = 0; // Current offset in _bytes

            // Search for removeFromCss:
            int removeLen = removeFromCss.length;
            for (int i = 0 ; i + removeLen < _bytes.length ; i++)
            {
                if (_bytes[i] == removeFromCss[0])
                {
                    boolean found = true;

                    for (int j = 1 ; j < removeLen ; j++)
                    {
                        if (_bytes[i + j] != removeFromCss[j])
                        {
                            found = false;
                            break;
                        }
                    }

                    if (found)
                    {
                        // Assigning also to iframe's domain:
                        int len = i - bytesOffset;
                        if (baos == null)
                            baos = new ByteArrayOutputStream(_bytes.length);
                        baos.write(_bytes, bytesOffset, len);
                        bytesOffset += len;
                        bytesOffset += removeLen;
                        i = bytesOffset;
                    }
                    continue;
                }
            }

            if (baos != null)
            {
                // Found removeFromCss:
                int len = _bytes.length - bytesOffset;
                baos.write(_bytes, bytesOffset, len);
                bytesOffset += len;
                _bytes = baos.toByteArray();

                // Gzipping modified _bytes:
                if (_isGziped || _isDeflated)
                {
                    try
                    {
                        ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(_bytes.length*3/4);
                        OutputStream oGzip = new GZIPOutputStream(oByteArr);
                        oGzip.write(_bytes);
                        oGzip.close();
                        _gzipedBytes = oByteArr.toByteArray();
                        _isGziped = true;
                        _isDeflated = false;
                    }
                    catch (IOException exc)
                    {
                        _gzipedBytes = null;
                        _isGziped = false;
                        _isDeflated = false;
                        throw exc;
                    }
                }
            }
        }

        // When isRouter then writing full response:
        if (_context.isRouter && _bytes != null && _bytes.length > 0)
        {
            OutputStream out = _context.response.getOutputStream();
            if (_isGziped || _isDeflated)
                out.write(_gzipedBytes);
            else
                out.write(_bytes);
        }
    }

    @Override
    public void addRequestHeader(String name, String value)
    {
        if (name.equals("Host"))
        {
            _host = value;
        }
        else if (_context.configData.removeFromHtml != null && (!_context.isOthers) &&
                    name.equals("Accept") && (value.contains("text/html") || value.contains("xml")))
        {
            _isHtml = true;
            if (!(_out instanceof ByteArrayOutputStream))
                _out = new ByteArrayOutputStream(4*1024); // Response will be written after removing
        }
        else if (_context.configData.removeFromCss != null && (!_context.isOthers) &&
                    name.equals("Accept") && value.contains("text/css"))
        {
            _isCss = true;
            if (!(_out instanceof ByteArrayOutputStream))
                _out = new ByteArrayOutputStream(4*1024); // Response will be written after removing
        }

        super.addRequestHeader(name, value);
    }

    @Override
    protected void onResponseComplete() throws IOException
    {
        if (_out instanceof ByteArrayOutputStream)
        {
            ByteArrayOutputStream baos = (ByteArrayOutputStream)_out;
            if (_isGziped || _isDeflated)
                _gzipedBytes = baos.toByteArray();
            else
                _bytes = baos.toByteArray();
            baos.close();
        }

        if (_indexOfFileSuffix >= 0)
            replace(_bytes);

        if (_context.configData.removeFromHtml != null && _isHtml)
            removeFromHtml();
        else if (_context.configData.removeFromCss != null && _isCss)
            removeFromCss();
        _lock.decCount();
    }

    @Override
    protected void onResponseContent(Buffer content) throws IOException
    {
        content.writeTo(_out);
        if (_context.isRouter)
            _out.flush();
    }

    @Override
    protected void onResponseStatus(Buffer version, int status, Buffer reason) throws IOException
    {
        _responseStatus = status;
        _responseReason = reason != null ? reason.toString() : null;

        if (_context != null && _context.isRouter)
            _context.response.setStatus(_responseStatus, _responseReason);
    }

    @Override
    protected void onResponseHeader(Buffer name, Buffer value) throws IOException
    {
        String n = name.toString().toLowerCase();
        String val = value.toString();
        String valLower = val.toLowerCase();

        if (_context.configData.removeFromHtml != null && (!_context.isOthers) &&
                n.equals("content-type") && valLower.contains("text/html"))
        {
            _isHtml = true;
            if (!(_out instanceof ByteArrayOutputStream))
                _out = new ByteArrayOutputStream(4*1024); // Response will be written after removing
        }

        if (n.equals("content-encoding"))
        {
            if (valLower.equals("gzip"))
            {
                _isGziped = true;
            }
            else if (valLower.equals("deflate"))
            {
                _isDeflated = true;
            }
        }

        if (_context != null && _context.isRouter && !n.equals("connection"))
        {
            _context.response.addHeader(name.toString(), val);
            return;
        }

        if (!_dropedHeaders.contains(n))
        {
            if (_contentTypeIndex < 0 && n.equals("content-type"))
            {
                _contentTypeIndex = _responseHeaders.size();
            }
            else if (_contentLengthIndex < 0 && n.equals("content-length"))
            {
                _contentLengthIndex = _responseHeaders.size();
            }
            else if (n.equals("set-cookie"))
            {
                if (_setCookieCount == 0)
                {
                    _setCookieIndexes = new int[64];
                    Arrays.fill(_setCookieIndexes, -1);
                }
                _setCookieIndexes[_setCookieCount++] = _responseHeaders.size();
            }
            else if (_contentEncodingIndex < 0 && n.equals("content-encoding"))
            {
                _contentEncodingIndex = _responseHeaders.size();
            }
            else if (_cacheControlIndex < 0 && n.equals("cache-control"))
            {
                _cacheControlIndex = _responseHeaders.size();
            }
            else if (_expiresIndex < 0 && n.equals("expires"))
            {
                _expiresIndex = _responseHeaders.size();
            }
            else if (_pragmaIndex < 0 && n.equals("pragma"))
            {
                _pragmaIndex = _responseHeaders.size();
            }
            else if (_etagIndex < 0 && n.equals("etag"))
            {
                _etagIndex = _responseHeaders.size();
            }
            else if (_lastModifiedIndex < 0 && n.equals("last-modified"))
            {
                _lastModifiedIndex = _responseHeaders.size();
            }

            _responseHeaders.add(name.toString());

            String queryEncoding = _context.configData.queryEncoding;
            if (queryEncoding != null)
                _responseHeaders.add(value.toString(queryEncoding));
            else
                _responseHeaders.add(val);
        }
    }

    /**
     * Adds/replaces Content-Type header in response-headers.
     *
     * @param value new value of Content-Type header in response
     */
    public void setResponseContentType(String value)
    {
        if (_contentTypeIndex >= 0)
        {
            _responseHeaders.set(_contentTypeIndex + 1, value);
        }
        else
        {
            _contentTypeIndex = _responseHeaders.size();
            _responseHeaders.add("Content-Type");
            _responseHeaders.add(value);
        }
    }

    @Override
    protected void onConnectionFailed(Throwable ex)
    {
        _responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (_context != null && _context.isRouter)
            _context.response.setStatus(_responseStatus);
        _isCommFailure = true;
        _responseReason = null;
        _lock.setError(true);
        _lock.decCount();
    }

    @Override
    protected void onException(Throwable ex)
    {
        if (ex instanceof EofException && _responseStatus != 0)
        {
            try
            {
                onResponseComplete();
            }
            catch (IOException e)
            {
            }
        }
        else
        {
            _responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
            if (_context != null && _context.isRouter)
                _context.response.setStatus(_responseStatus);
            _isCommFailure = true;
            _responseReason = null;
            _lock.setError(true);
            _lock.decCount();
        }
    }

    @Override
    protected void onExpire()
    {
        _responseStatus = HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
        if (_context != null && _context.isRouter)
            _context.response.setStatus(_responseStatus);
        _isCommFailure = true;
        _responseReason = null;
        _lock.setError(true);
        _lock.decCount();
    }

    /**
     * Ungzip the response-content (only when its content-encoding is gzip).
     */
    private void ungzipResponse()
    {
        if (_isGziped)
        {
            if (_gzipedBytes.length == 0)
            {
                _bytes = new byte[0];
                return;
            }

            try
            {
                MyByteArrayInputStream bais = new MyByteArrayInputStream(_gzipedBytes);
                GZIPInputStream is = new GZIPInputStream(bais);
                byte[] buff = new byte[16*10240];
                ByteArrayOutputStream os = new ByteArrayOutputStream(_gzipedBytes.length * 2);
                int n;
                while (bais.getPos() < _gzipedBytes.length && (n = is.read(buff)) >= 0)
                    os.write(buff, 0, n);
                _bytes = os.toByteArray();

                is.close();
                os.close();
            }
            catch (IOException exc)
            {
                exc.printStackTrace();
            }
        }
        else if (_isDeflated)
        {
            if (_gzipedBytes.length == 0)
            {
                _bytes = new byte[0];
                return;
            }

            try
            {
                InflaterInputStream is = new InflaterInputStream(
                                                                    new ByteArrayInputStream(_gzipedBytes), new Inflater(true));
                byte[] buff = new byte[16*10240];
                ByteArrayOutputStream os = new ByteArrayOutputStream(_gzipedBytes.length * 2);
                int n;
                while ((n = is.read(buff)) >= 0)
                    os.write(buff, 0, n);
                _bytes = os.toByteArray();

                is.close();
                os.close();
            }
            catch (IOException exc)
            {
                exc.printStackTrace();
            }
        }
    }

    /**
     * When _replaceSrc is found in orig and when it is followed by _replaceDstPrefix
     * and later by _replaceDestSuffix a modified byte[] in which the source string is
     * replaced is returned (otherwise orig is returned).
     *
     * @param orig the source buffer in which _replaceSrc is to be replaced
     * @return the modified buffer if a replace has occured (otherwise orig is returned)
     */
    public byte[] replace(byte[] orig) throws IOException
    {
        if (_indexOfFileSuffix >= 0)
        {
            getUngzipedResponseContent(); // Scanning ungipzed response.
            orig = _bytes;
        }

        if (!_isReplacing || _responseStatus != 200 || (_context != null && _context.isRouter))
            return orig;

        int srcLen = _replaceSrc.length;
        int dstPrefixLen = (_replaceDstPrefix != null ? _replaceDstPrefix.length : 0);
        int i = 0;
        int srcFirst = (-1);

        // Looping until a src string that is followed by a near dest string is found:
        while (i < orig.length)
        {
                // Finding next occurence of _replaceSrc in orig:
                while (++i < orig.length)
                {
                    if (orig[i] != _replaceSrc[0])
                        continue;
                    if (i + srcLen >= orig.length)
                        return orig;
                    boolean foundSrc = true;
                    for (int j = 1 ; j < srcLen ; j++)
                    {
                        if (orig[i + j] !=  _replaceSrc[j])
                        {
                            foundSrc = false;
                            break;
                        }
                    }
                    if (foundSrc)
                        break;
                }

                if (i >= orig.length)
                    return orig;

                srcFirst = i;
                i += srcLen;

                // Finding first near occurence of _replacePrefix in orig:
                if (_replaceDstPrefix == null)
                    break; // No dst to replace src (only before and after will be applied)

                boolean foundDstPrefix = false;
                int maxPrefixFirst = (i + 1024 > orig.length ? orig.length : i + 1024);
                while (++i < maxPrefixFirst)
                {
                    if (orig[i] != _replaceDstPrefix[0])
                        continue;
                    if (i + dstPrefixLen >= orig.length)
                        return orig;
                    foundDstPrefix = true;
                    for (int j = 1 ; j < dstPrefixLen ; j++)
                    {
                        if (orig[i + j] !=  _replaceDstPrefix[j])
                        {
                            foundDstPrefix = false;
                            break;
                        }
                    }
                    if (foundDstPrefix)
                        break;
                }
                if (foundDstPrefix)
                    break;

                i = srcFirst + 1; // Didn't find an appropriate dest-prefix for current src.
        }

        if (i >= orig.length)
            return orig;

        int dstPrefixFirst = (_replaceDstPrefix != null ? i + _replaceDstSkipLen : -1);
        i += dstPrefixLen;

        // Finding first near occurence of _replaceDstSuffix in orig:
        int dstSuffixLen = (_replaceDstPrefix != null ? _replaceDstSuffix.length : 0);
        if (_replaceDstPrefix != null)
        {
            boolean foundDstSuffix = false;
            int maxSuffixFirst = (i + 1024 > orig.length ? orig.length : i + 1024);
            while (++i < maxSuffixFirst)
            {
                if (orig[i] != _replaceDstSuffix[0])
                    continue;
                if (i + dstSuffixLen >= orig.length)
                    return orig;
                foundDstSuffix = true;
                for (int j = 1 ; j < dstSuffixLen ; j++)
                {
                    if (orig[i + j] !=  _replaceDstSuffix[j])
                    {
                        foundDstSuffix = false;
                        break;
                    }
                }
                if (foundDstSuffix)
                    break;
            }

            if (!foundDstSuffix)
                return orig;
        }
        int dstSuffixFirst = i;

        // Returned modified byte-array:
        int dstLen = (_replaceDstPrefix != null ? (dstSuffixFirst + dstSuffixLen) - dstPrefixFirst : 0);
        int newLen = orig.length - srcLen + dstLen +
                            (_replaceBefore != null ? _replaceBefore.length : 0) +
                            (_replaceAfter != null ? _replaceAfter.length : 0);
        byte[] newOrig = new byte[newLen];
        int iNewOrig = 0;
        System.arraycopy(orig, 0, newOrig, iNewOrig, srcFirst);
        iNewOrig += srcFirst;
        if (_replaceBefore != null)
        {
            System.arraycopy(_replaceBefore, 0, newOrig, iNewOrig, _replaceBefore.length);
            iNewOrig += _replaceBefore.length;
        }
        if (_replaceDstPrefix != null)
        {
            System.arraycopy(orig, dstPrefixFirst, newOrig, iNewOrig, dstLen);
            iNewOrig += dstLen;
        }
        if (_replaceAfter != null)
        {
            System.arraycopy(_replaceAfter, 0, newOrig, iNewOrig, _replaceAfter.length);
            iNewOrig += _replaceAfter.length;
        }
        System.arraycopy(orig, srcFirst + srcLen, newOrig, iNewOrig, newLen - iNewOrig);

        _bytes = newOrig;

        // Gzipping modified _bytes:
        if (_indexOfFileSuffix >= 0 && (_isGziped || _isDeflated))
        {
            try
            {
                ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(newOrig.length*3/4);
                OutputStream oGzip = new GZIPOutputStream(oByteArr);
                oGzip.write(newOrig);
                oGzip.close();
                _gzipedBytes = oByteArr.toByteArray();
                _isGziped = true;
                _isDeflated = false;
            }
            catch (IOException exc)
            {
                _gzipedBytes = null;
                _isGziped = false;
                _isDeflated = false;
                throw exc;
            }
        }
        return newOrig;
    }

    /**
     * Adds getPos to ByteArrayInputStream.
     * Needed for bypassing EOFException bug of GZIPInputStream.
     */
    private static class MyByteArrayInputStream extends ByteArrayInputStream
    {
        public MyByteArrayInputStream(byte[] bytes)
        {
            super(bytes);
        }

        public int getPos()
        {
            return pos;
        }
    }

}
