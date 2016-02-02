/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;
import javax.crypto.Cipher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.io.ByteArrayBuffer;

/**
 * ServiceUtils
 *
 * Methods called during http service execution.
 * A service-context is passed to each method.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.232 $$Date: 2013/08/26 10:24:43 $
 */
public class ServiceUtils
{
    /**
     * When an ungziped rsrc with a larger size is added to the cache it is gziped.
     */
    public static final int MIN_GZIP_LEN = 4000;

    /**
     * Used for routing un-licensed domains
     */
    public static final String OTHERS_DOMAIN = "others";

    private static final String EMPTY_STRING = "";

    private static final byte[] ABOUT_BLANK = new byte[]{'a','b','o','u','t',':','b','l','a','n','k'};

    private static final String BASE_TARGET = "<base  target=\"_parent\">";
    private static final byte[] BASE_TARGET_BYTES = BASE_TARGET.getBytes();

    private static final String HTML_SPEED_FUNCS =
            "<script>" +
            "var iHtmlSpeed = 3;\nvar ieHtmlSpeed = /msie/i.test(navigator.userAgent) && !/opera/i.test(navigator.userAgent);\n" +
            "function htmlSpeed1(){// Copyright Galiel 3.14\n" +
            "var frm = null; if (top.document)frm=top.document.getElementById('htmlSpeedIfrm');\niHtmlSpeed++;\n" +
            "if (frm == null || typeof top.iHtmlSpeed === 'undefined' || !top.document || " +
                    "(iHtmlSpeed < 2 && top.document.readyState != 'complete') || " +
                    "(top.document.readyState == 'uninitialized')){setTimeout('htmlSpeed1()', 100);if(frm != null &&frm.height<document.height)frm.height=document.height;return;};\n" +
            "var scrlx = -1;\n" +
            "var scrly = -1;\n" +
            " if(window.pageYOffset){\n" +
                "scrly=window.pageYOffset; scrlx=window.pageXOffset;}\n" +
            "if (scrlx >= 0 && top.scrollTo) " +
                "top.scrollTo(scrlx, scrly);\n" +
            "top.document.body.removeChild(frm);\n"+
            "};\n" +
            "</script>";

    private static final String IFRAME = "<script>if (top.document==document){" +
                                            "document.write(\"<iframe id='htmlSpeedIfrm' src='//\" + location.hostname + \"/htmlspeed.html' width='100%' " +
                                            "height='100%' scrolling='no' frameBorder='0' style='position:absolute;left:0;top:0;z-index:2147483647'></iframe>\");" +
                                            "}else{setTimeout('htmlSpeed1()', 200);}</script>";
    private static final byte[] IFRAME_BYTES= IFRAME.getBytes();

    private static final String IFRAME_IE = IFRAME.replace("opacity:0.0;", "filter:alpha(opacity=0);");
    private static final byte[] IFRAME_IE_BYTES = IFRAME_IE.getBytes();

    private static final byte[] HTML_SPEED_FUNCS_BYTES = HTML_SPEED_FUNCS.getBytes();

    private static final String HTML_SPEED_FUNCS_DELAY_IFRAME =
            HTML_SPEED_FUNCS.replace(
                        "top.document.readyState == 'uninitialized'",
                        "top.document.readyState != 'complete' && top.document.readyState != 'interactive'")
                            .replace(
                        "iHtmlSpeed++;",
                        "if (ieHtmlSpeed){"+
                            "if (iHtmlSpeed == 3){"+
                                "iHtmlSpeed = 1; " +
                                "if(top.addEventListener){" +
                                    "top.addEventListener("+
                                        "'DOMContentLoaded',function(e){iHtmlSpeed = 2;}, false);"+
                                "}" +
                            "};"+
                        "}");

    private static final byte[] HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES = HTML_SPEED_FUNCS_DELAY_IFRAME.getBytes();

    private static final String IF_IN_IFRAME = "if(top.document==document){";
    private static final byte[] IF_IN_IFRAME_BYTES = IF_IN_IFRAME.getBytes();

    private static final String SCRIPT_IN_IFRAME_START = "<script>if (top.document==document){document.write('<sc'+'ript";
    private static final byte[] SCRIPT_IN_IFRAME_START_BYTES = SCRIPT_IN_IFRAME_START.getBytes();

    private static final String SCRIPT_IN_IFRAME_START_DEFER = "<script>if (top.document==document){document.write('<sc'+'ript defer=\"defer\" ";
    private static final byte[] SCRIPT_IN_IFRAME_START_DEFER_BYTES = SCRIPT_IN_IFRAME_START_DEFER.getBytes();

    private static final String SCRIPT_IN_IFRAME_END = "</sc'+'ript>');}</script>";
    private static final byte[] SCRIPT_IN_IFRAME_END_BYTES = SCRIPT_IN_IFRAME_END.getBytes();

    private static final String ONLOAD_SUFFIX = "};";
    private static final byte[] ONLOAD_SUFFIX_BYTES = ONLOAD_SUFFIX.getBytes();

    private static final String ONLOAD_FULL = " onload=\"\"";
    private static final byte[] ONLOAD_FULL_BYTES = ONLOAD_FULL.getBytes();

    private static final String PARENT = "_parent";
    private static final byte[] PARENT_BYTES = PARENT.getBytes();

    private static final String TARGET_PARENT = " target='_parent'";
    private static final byte[] TARGET_PARENT_BYTES = TARGET_PARENT.getBytes();

    private static final String TARGET_SELF = " target='_self'";
    private static final byte[] TARGET_SELF_BYTES = TARGET_SELF.getBytes();

    private static final String SCRIPT_START = "<script>";
    private static final byte[] SCRIPT_START_BYTES = SCRIPT_START.getBytes();

    private static final String SCRIPT_END = "</script>";
    private static final byte[] SCRIPT_END_BYTES = SCRIPT_END.getBytes();

    private static final String IF_START = "if (top.document==document){";
    private static final byte[] IF_START_BYTES = IF_START.getBytes();

    private static final String ELSE = "} else {";
    private static final byte[] ELSE_BYTES = ELSE.getBytes();

    private static final String IF_END = "};";
    private static final byte[] IF_END_BYTES = IF_END.getBytes();

    private static final String DOCUMENT_WRITE_START = "document.write('";
    private static final byte[] DOCUMENT_WRITE_START_BYTES = DOCUMENT_WRITE_START.getBytes();

    private static final String DOCUMENT_WRITE_END = "');";
    private static final byte[] DOCUMENT_WRITE_END_BYTES = DOCUMENT_WRITE_END.getBytes();

    private static final String STYLE_START = "<style>";
    private static final byte[] STYLE_START_BYTES = STYLE_START.getBytes();

    private static final String STYLE_END = "</style>";
    private static final byte[] STYLE_END_BYTES = STYLE_END.getBytes();

    /**
     * True when content-first optimization is activated.
     */
    private static boolean _isWithIframe = false;

    /**
     * Used to be a property (should always be true).
     */
    private final static boolean _isDelayIframe = true;

    /**
     * True when FIRST_PLUS_VISIT is enabled.
     */
    private static boolean _isWithFirstPlus = false;

    /**
     * List of hosts and their routing information as definied in configuration.
     */
    private static HostInfo[] _hostInfos = null;

    /**
     * Maps boosted domain/sub-domain to its routing information.
     */
    private static HashMap<String, HostInfo> _hostToHostInfo = null;

    /**
     * Uri's from this host are not prefixed in cache.rsrcs by "(host-name)".
     */
    private static String _primeHost = null;

    /**
     * Information regarding the content-servers one of the domains/sub-domains
     * that are boosted by current htmlspeed server.
     */
    private static class HostInfo
    {
        /**
         * Name of domain (example: www.kuku.com)
         */
        public String[] hosts;

        /**
         * List all content-servers node-names/ip-addresses.
         */
        public String[] addresses = null;

        /**
         * _weights[i] is the weight of content-server _addresses[i]
         * Example: when _weights[i] equals 2 than 2 requests are
         * routed to _addresses[i] and the third request will be
         * routed to _addresses[i+1].
         */
        public byte[] weights = null;

        /**
         * _commFailureTimes[i] is the last time when communication
         * with _addresses[i] has failed, or 0 when the content-server
         * is currently healthy.
         */
        public long[] commFailureTimes = null;

        /**
         * Next request will be routed to _addresses[_iAddresses]
         * iff _rWeights > 0
         */
        public int iAddresses = 0;

        /**
         * Remaining number of requests to route to _addresses[_iAddresses].
         */
        public int rWeights;
    }

    /**
     * Number of milli-seconds before Rsrc.maxFreshTime when refresh is required.
     */
    private static final int REFRESH_GAP_IN_MILLIS = 5000;

    /**
     * Queue of collections of temp-rsrcs to be loaded by the background-thread.
     */
    private static LinkedBlockingQueue<BackThreadLoadInfo> _blockingQueue =
                            new LinkedBlockingQueue<BackThreadLoadInfo>();

    /**
     * The background-thread is responsible for loading rsrcs of statefull pages/style-sheets.
     */
    private static BackThread _backThread = null;

    /**
     * Last time exit-now msg has been checked.
     */
    private static volatile long _prevExitNowTime = 0;

    /**
     * When true X-Real-IP and X-Forwarded-For headers are added to requests.
     */
    private static boolean _addProxyHeaders = false;

    /**
     * Sets value or property withFirstPlus (true when FIRST_PLUS_VISIT is enabled).
     *
     * @param isWithFirstPlus  new value of property
     */
    public static void setWithFirstPlus(boolean isWithFirstPlus)
    {
        _isWithFirstPlus = isWithFirstPlus;
    }

    /**
     * Sets value of property addProxyHeaders (default is false).
     *
     * @param addProxyHeaders new value of property
     */
    public static void setAddProxyHeaders(boolean addProxyHeaders)
    {
        _addProxyHeaders = addProxyHeaders;
    }

    /**
     * Assigns new value to property withIframe
     * @param isWithIframe new value of property
     */
    public static void setWithIframe(boolean isWithIframe)
    {
        _isWithIframe = isWithIframe;
    }

    /**
     * Tries to return a cached resource to the calling browser.
     *
     * Cached resources have variants. The identifier of each variant
     * is a character (example: CacheUtils.FIRST_VISIT_VARIANT).
     * Different variants can be used for different levels of inlining of
     * resources in a page.
     *
     * Each resource-variant is separately loaded into cache.
     * The key in cache.rsrcs is:  variant+url.
     *
     * For stateless pages, if the requested page-variant is cached and its
     * etag hasn't changed then the status not-modified is returned, else if
     * the next-variant is cached it is returned, else false is returned, after
     * storing the next-variant in context.variant.
     *
     * When tryToUseCachedRsrc returns false, the service loads the
     * page, generates the required page-variant (context.variant) and
     * adds it to the cache.
     *
     * @param context service-context
     * @return true when a cached resource has been returned to calling browser
     * @throws IOException can occur when accessing the response
     */
    public static boolean tryToUseCachedRsrc(ServiceContext context) throws IOException
    {
        String url = context.url;
        HttpServletRequest request = context.request;
        HttpServletResponse response = context.response;
        boolean isIE8 = context.isIE8;
        boolean isMobile = context.isMobile;
        boolean isRobot = context.isRobot;
        ConfigData configData = context.configData;

        boolean isVersioned = url.endsWith(CacheUtils.VERSION_SUFFIX);

        String etag = request.getHeader("If-None-Match"); // ETag to return to browser.

        if (isVersioned && "1".equals(etag))
        {
            response.setHeader("ETag", "1");
            response.setHeader("Cache-Control", "max-age=31536000"); // Cache for another year.
            response.setStatus(304); // Not modified.
            return true;
        }

        char variant = CacheUtils.variantOf(context.url, etag, isIE8, isMobile, isRobot);

        // Skipping first-visit for pages refered to by other pages in this site:
        if (variant == CacheUtils.FIRST_VISIT_VARIANT ||
            variant == CacheUtils.IE8_FIRST_VISIT_VARIANT ||
            variant == CacheUtils.MOBILE_FIRST_VISIT_VARIANT)
        {
            String referer = request.getHeader("Referer");
            int index = referer == null ? (-1) : referer.indexOf(context.host);
            if (index == 7 || index == 8)
            {
                // referer starts with http[s]://host
                if (_isWithFirstPlus)
                    variant += 1;
                else
                    variant += 2;
            }
        }

        context.variant = variant;
        char savedVariant = context.variant; // Used when can't find a fresh next variant.

        if (etag != null && !CacheUtils.isHtmlSpeedEtag(etag))
            return false; // Not an ETag of a state-less resource.

        boolean isGziped = false; // True when response is Gziped
        String contentEncoding = null; // When isGziped equals "gzip" or "deflate"
        byte[] cachedContent = null; // Content to return to browser (when not null)
        String[] cachedHttpHeaders = null; // Headers of found cached resource.

        long currentTime = System.currentTimeMillis();

        Rsrc rsrc = findFreshCachedRsrc(context, currentTime);
        long rsrcMaxFreshTime = context.maxFreshTime;
        long maxAge = 0; // max-age to return to browser.

        if (rsrc != null)
        {

            if (!(rsrc instanceof PageRsrc))
            {
                variant = CacheUtils.NON_PAGE_VARIANT;
                context.variant = variant;
            }

            isGziped = rsrc.optimGzip != null;
            if (isGziped)
                contentEncoding = rsrc.optimGzipEncoding;

            if (configData.isFixedMaxAge)
                maxAge = context.maxAge;
            else
                maxAge = (rsrcMaxFreshTime - currentTime) / 1000;

            if (maxAge < 0)
                maxAge = 0;

            // Resource not-modified:
            if (etag != null && rsrc.origMd5 != null && etag.contains(rsrc.origMd5))
            {
                response.setHeader("ETag", etag); // The ETag is '[' + md5 + variant + ']'

                if (rsrc instanceof PageRsrc && configData.minStateLessVariant != configData.maxStateLessVariant)
                    response.setHeader("Cache-Control", "private, max-age=" + maxAge);
                else
                    response.setHeader("Cache-Control", "max-age=" + maxAge);

                response.setStatus(304); // Not modified.
                return true;
            }

            // Resource was modified:
            if (etag != null && rsrc instanceof PageRsrc &&
                    ((CacheUtils.FIRST_VISIT_VARIANT <= variant &&
                    variant < CacheUtils.FORTH_VISIT_VARIANT) ||
                        (CacheUtils.IE8_FIRST_VISIT_VARIANT <= variant &&
                        variant < CacheUtils.IE8_FORTH_VISIT_VARIANT) ||
                        (CacheUtils.MOBILE_FIRST_VISIT_VARIANT <= variant &&
                        variant < CacheUtils.MOBILE_FORTH_VISIT_VARIANT)))
            {
                // Next-visit variant should be returned to browser:
                variant++;
                if (variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                        variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                        variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                    variant++;
                context.variant = variant;
                
                rsrc = findFreshCachedRsrc(context, currentTime);
                rsrcMaxFreshTime = context.maxFreshTime;

                if (rsrc == null)
                {
                    context.variant = savedVariant;
                    return false; // Next-visit variant is not found or not fresh.
                }

                if (configData.isFixedMaxAge)
                    maxAge = context.maxAge;
                else
                    maxAge = (rsrcMaxFreshTime - currentTime) / 1000;

                if (maxAge < 0)
                    maxAge = 0;
            }

            etag = '[' + rsrc.origMd5 + variant + ']';
            //
            // If the correct page-variant is found in cache, then
            // it is returned. Otherwise false is returned, and
            // the page with required page-variant will be loaded
            // from content-server and added to the cache-structure.
            //

            if(System.currentTimeMillis() < rsrcMaxFreshTime)
            {
                cachedHttpHeaders = rsrc.httpHeaders;

                if (rsrc.optimGzip != null)
                {
                    isGziped = true;
                    contentEncoding = rsrc.optimGzipEncoding;
                    cachedContent = rsrc.optimGzip;
                }
                else if (rsrc.origGzip != null)
                {
                    isGziped = true;
                    contentEncoding = rsrc.origGzipEncoding;
                    cachedContent = rsrc.origGzip;
                }
                else if (rsrc.optimData != null)
                {
                    isGziped = false;
                    cachedContent = rsrc.optimData;
                }
                else if (rsrc.origData != null)
                {
                    isGziped = false;
                    cachedContent = rsrc.origData;
                }
            }
        }

        if (cachedContent != null)
        {
            boolean foundVaryHeader = false;

            // Adding header-fields to response:
            for (int h = 0 ; h < cachedHttpHeaders.length ; h += 2)
            {
                String header = cachedHttpHeaders[h].toLowerCase();

                if (header.equals("etag") ||
                    header.equals("cache-control") ||
                    header.equals("expires") ||
                    header.equals("set-cookie") ||
                    (header.equals("pragma") &&
                        cachedHttpHeaders[h+1].equalsIgnoreCase("no-cache")) ||
                     header.equals("content-encoding"))
                {
                    // Skip:
                    //      Etag is added bellow,
                    //      Cache-Control is added bellow,
                    //      Expires not used (max-age is used instead),
                    //      no cookies are sent to browser for stateless rsrcs.
                }
                else if (rsrc instanceof PageRsrc && header.equals("vary") )
                {
                    foundVaryHeader = true;
                    if (cachedHttpHeaders[h+1].toLowerCase().contains("user-agent"))
                        response.addHeader(cachedHttpHeaders[h], cachedHttpHeaders[h+1]);
                    else
                        response.addHeader(cachedHttpHeaders[h], cachedHttpHeaders[h+1] + ",User-Agent");
                }
                else
                {
                    response.addHeader(cachedHttpHeaders[h], cachedHttpHeaders[h+1]);
                }
            }

            if (isVersioned)
            {
                response.setHeader("ETag", "1");
                response.setHeader("Cache-Control", "max-age=31536000"); // Cached for one-year:
            }
            else
            {
                response.setHeader("ETag", etag);

                if (rsrc instanceof PageRsrc && configData.minStateLessVariant != configData.maxStateLessVariant)
                    response.setHeader("Cache-Control", "private, max-age=" + maxAge);
                else
                    response.setHeader("Cache-Control", "max-age=" + maxAge);

                if (rsrc instanceof PageRsrc && !foundVaryHeader)
                    response.addHeader("Vary", "User-Agent");
            }

            if (isGziped)
                response.setHeader("Content-Encoding", contentEncoding);
            response.setHeader("Content-Length", cachedContent.length + "");
            response.setStatus(200);
            response.getOutputStream().write(cachedContent);
            return true;
        }

        context.variant = savedVariant;
        return false;
    }

    /**
     * Tries to find in the cache the fresh resource: context.variant + context.url.
     * If refresh of resource must be initiated then context.refreshRsrc is allocated.
     * When must waits to refresher, the method waits until refersh is finished.
     *
     * @param context of current http-service
     * @param currentTime saves a lot of calls to System.currentTimeMillis
     * @return a fresh cached resource when found (null otherwise).
     */
    private static Rsrc findFreshCachedRsrc(ServiceContext context, long currentTime)
    {
        CacheStructure cache = context.cache;
        char variant = context.variant;
        String url = context.url;
        HttpServletRequest request = context.request;
        ConfigData configData = context.configData;

        Rsrc rsrc = null; // Not null when a fresh cached resource is found.
        context.maxFreshTime = 0; // Unknown until rsrc is found.
        context.maxAge = 0;
        
        LoadLock waiter = null; // used for waiting to refresher of rsrc (when not null).

        synchronized (cache.globalLock)
        {
            RsrcIfc r = getFromCache(variant, url, configData);

            if (r instanceof TempRsrc)
            {
                TempRsrc tmpRsrc = (TempRsrc)r;

                if (tmpRsrc.isBeingRefreshed && tmpRsrc.loader != null)
                {
                    if (tmpRsrc.replacer.maxFreshTime > currentTime)
                    {
                        // Can use refreshed but still fresh rsrc:
                        rsrc = tmpRsrc.replacer;
                        context.maxFreshTime = rsrc.maxFreshTime;
                        context.maxAge = rsrc.maxAge;
                    }
                    else
                    {
                        waiter = new LoadLock();
                        waiter.setCount(1);
                        if (tmpRsrc.waiters == null)
                            tmpRsrc.waiters = new ArrayList<LoadLock>();
                        tmpRsrc.waiters.add(waiter);
                    }
                }
            }
            else if (r instanceof Rsrc)
            {
                rsrc = (Rsrc)r;
                rsrc.lastUsageTime = currentTime;
                context.maxFreshTime = rsrc.maxFreshTime;
                context.maxAge = rsrc.maxAge;

                if (r instanceof PageRsrc && ConfigUtils.isAutoRefreshedPage(((PageRsrc)r).url))
                {
                    return rsrc; // Auto-refreshed rsrc is assumed to be fresh.
                }

                // Forcing upper-limit on server-side max-age:
                long maxServerSideFreshTime = rsrc.maxFreshTime;
                final long maxServerSideMaxAge = configData.maxServerSideMaxAge;
                if (maxServerSideMaxAge > 0 && (rsrc.maxFreshTime - rsrc.lastRefreshTime) > maxServerSideMaxAge*1000)
                {
                    maxServerSideFreshTime = rsrc.lastRefreshTime + maxServerSideMaxAge*1000;
                }

                if (maxServerSideFreshTime - currentTime < REFRESH_GAP_IN_MILLIS)
                {
                    // Current service should refresh the rsrc:
                    rsrc.isBeingLoaded = true;
                    TempRsrc tmpRsrc = new TempRsrc();
                    tmpRsrc.replacer = rsrc;
                    tmpRsrc.isBeingRefreshed = true;
                    tmpRsrc.loader = new LoadLock();
                    tmpRsrc.host = rsrc.host;
                    tmpRsrc.url = rsrc.url;
                    tmpRsrc.isVersioned = rsrc.versionUrl != null;
                    context.refreshRsrc = tmpRsrc;
                    putInCache(variant, url, tmpRsrc, configData);
                    context.maxFreshTime = 0;
                    context.maxAge = 0;
                    return null;
                }
            }
        }

        if (waiter != null)
        {
            // Waiting for refresher:
            try
            {
                waiter.waitUntilCountIs0();
            }
            catch (InterruptedException e)
            {
            }

            // Trying to use the just refreshed resource:
            synchronized (cache.globalLock)
            {
                RsrcIfc r = getFromCache(variant, url, configData);
                if (r instanceof Rsrc)
                {
                    rsrc = (Rsrc) r;
                    context.maxFreshTime = rsrc.maxFreshTime;
                    context.maxAge = rsrc.maxAge;
                }
                else
                {
                    rsrc = null;
                }
            }
        }

        return rsrc;
    }

    /**
     * Initializes context.exchange before routing
     * the executed http-request to the content server.
     *
     * @param context service-context
     */
    public static void initExchange(ServiceContext context)
    {
        HtmlSpeedHttpExchange exchange = context.exchange;
        boolean isHttps = context.isHttps;
        HttpServletRequest request = context.request;
        String host = context.host;
        String url = context.url;
        HashSet<String> dropedHeaders = context.dropedHeaders;
        InputStream in = context.in;
        boolean isPost = context.isPost;
        byte[] postedContent = context.postedContent;
        ConfigData configData = context.configData;

        final int MD5_LEN = CacheUtils.MD5_LENGTH;

        exchange.setMethod(request.getMethod());

        setExchangeDestParams(
                        context,
                        isPost,
                        exchange,
                        url,
                        host);

        String protocol = request.getProtocol();
        exchange.setVersion(protocol);

        // check connection header
        String connectionHdr = request.getHeader("Connection");
        if (connectionHdr != null)
        {
            connectionHdr = connectionHdr.toLowerCase();
            if (connectionHdr.indexOf("keep-alive") < 0 && connectionHdr.indexOf("close") < 0)
                connectionHdr = null;
        }

        boolean isGetMethod = request.getMethod().equals("GET");

        // Copy headers:

        boolean xForwardedFor = false;
        boolean xRealIP = false;
        long contentLength = -1;
        Enumeration<?> enm = request.getHeaderNames();
        while (enm.hasMoreElements())
        {
            // TODO could be better than this!
            String hdr = (String)enm.nextElement();
            String lhdr = hdr.toLowerCase();

            if (dropedHeaders.contains(lhdr))
                continue;
            if (connectionHdr != null && connectionHdr.indexOf(lhdr) >= 0)
                continue;
            if ("host".equals(lhdr))
                continue;

            if ("x-forwarded-for".equals(lhdr))
            {
                xForwardedFor = true;
            }
            else if ("x-real-ip".equals(lhdr))
            {
                xRealIP = true;
            }

            Enumeration<?> vals = request.getHeaders(hdr);
            while (vals.hasMoreElements())
            {
                String val = (String)vals.nextElement();
                if (val != null)
                {
                    //
                    // Removing md5 of prev content from value of If-None-Match
                    // and storing it in context.prevMd5. That value will later be
                    // compared to md5 of response:
                    //
                    if ("if-none-match".equals(lhdr) && CacheUtils.isHtmlSpeedEtag(val))
                    {
                        int len = val.length();
                        String prevMd5 = val.substring(len - MD5_LEN - 2, len - 2); // Variant is not part of md5
                        context.prevMd5 = prevMd5;

                        if (len == MD5_LEN + 3)
                            continue; // No original ETag

                        val = val.substring(0, len - MD5_LEN - 3); // The original ETag.
                    }

                    exchange.addRequestHeader(hdr,val);
                }
            }
        }

        // When refreshing a state-less resource using cached origEtag and origLastModified:
        if (context.refreshRsrc != null)
        {
            Rsrc rsrc = (Rsrc)context.refreshRsrc.replacer;

            if (rsrc.origEtag != null)
                exchange.setRequestHeader("If-None-Match", rsrc.origEtag);

            if (rsrc.origLastModified != null)
                exchange.setRequestHeader("If-Modified-Since", rsrc.origLastModified);
        }

        // Proxy headers    !!!!!!!!!!!!!!!!!!!!!!!!!!!!! (CAUSES NOT-GZIP REPLIES)
        //exchange.setRequestHeader("Via","1.1 (jetty)");
        if (_addProxyHeaders && !xForwardedFor)
        {
            exchange.addRequestHeader("X-Forwarded-For",request.getRemoteAddr());
            //exchange.addRequestHeader("X-Forwarded-Proto",request.getScheme());
            //exchange.addRequestHeader("X-Forwarded-Host",request.getServerName());
            //exchange.addRequestHeader("X-Forwarded-Server",request.getLocalName());
        }
        if (_addProxyHeaders && !xRealIP)
        {
            exchange.addRequestHeader("X-Real-IP", request.getRemoteAddr());
        }

        if (postedContent != null)
        {
            try
            {
                if (configData.isDebug)
                    System.out.println("POST content: " + new String(postedContent, "UTF-8"));
                exchange.setRequestContent(new ByteArrayBuffer(postedContent));

                // exchange.setRequestContentSource(in);  !!!! not-working 
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
            }
        }
    }

    /**
     * @param context current service context
     * @return the posted content (null when no content is posted)
     */
    public static byte[] getPostedContent(ServiceContext context)
    {
        HttpServletRequest request = context.request;
        InputStream in = context.in;
        boolean isPost = context.isPost;

        if (!isPost)
            return null;

        int contentLen = (-1);

        if (request.getHeader("Content-Type") != null || request.getHeader("Content-Length") != null)
        {
            contentLen = request.getContentLength(); // Number of bytes to read from in.
        }

        if (contentLen >= 0)
        {
            try
            {
                byte[] bytes = new byte[contentLen]; // The request content.
                int readCount = 0; // Total number of read bytes.
                while (readCount < contentLen)
                {
                    int count = in.read(bytes, readCount, contentLen - readCount);

                    if (count < 0)
                        break;

                    readCount += count;
                }
                return bytes;
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
            }
        }

        return null;
    }

    /**
     * Replaces refreshedRsrc by refreshedRsrc.replacer in the cache-structure,
     * and notifies refreshedRsrc.waiters that the refresh has completed.
     *
     * @param exchange http-service invoked on content-server
     * @param refreshedRsrc the temp-rsrc pointing to the refreshed resource
     * @param modifiedRsrc when not null is the modified resource to replace the cached rsrc instance.
     * @param maxAge freshness time in seconds of refreshed resource.
     * @param configData used configuration
     * @return new max-age of refreshed resource.
     */
    public static long endRefreshAndNotifyWaiters(
                                                HtmlSpeedHttpExchange exchange,
                                                TempRsrc refreshedRsrc,
                                                Rsrc modifiedRsrc,
                                                long maxAge,
                                                ConfigData configData)
    {
        TempRsrc tmpRsrc = refreshedRsrc;
        Rsrc rsrc = tmpRsrc.replacer;

        CacheStructure cache = configData.cache;

        synchronized (cache.globalLock)
        {
            if (modifiedRsrc != null)
            {
                modifiedRsrc.variant = rsrc.variant;
                if (rsrc.versionUrl != null)
                    removeFromCache(rsrc.variant, rsrc.versionUrl, configData); // The modified rsrc has a different version-url
                rsrc = modifiedRsrc;
            }

            tmpRsrc.isBeingRefreshed = false;
            rsrc.isBeingLoaded = false;
            rsrc.lastRefreshTime = System.currentTimeMillis();
            rsrc.maxFreshTime = rsrc.lastRefreshTime + maxAge*1000;
            rsrc.lastUsageTime = rsrc.lastRefreshTime;
            rsrc.maxAge = maxAge;
            rsrc.host = tmpRsrc.host;
            rsrc.url = tmpRsrc.url;

            // Replacing tmpRsrc by rsrc in cache.rsrcs:
            putInCache(rsrc.variant, rsrc.url, rsrc, configData);

            if (tmpRsrc.waiters != null)
                for (LoadLock w : tmpRsrc.waiters)
                    w.decCount();
            tmpRsrc.waiters = null;
        }

        return maxAge;
    }

    /**
     * Process http-headers of the response from content-server.
     *
     * @param context service-context
     */
    public static void processResponseHeaders(ServiceContext context)
    {
        HtmlSpeedHttpExchange exchange = context.exchange;
        HttpServletResponse response = context.response;
        int status = context.status;
        String[] responseHeaders = context.responseHeaders;
        ArrayList<String> cachedHeaders = context.cachedHeaders;
        boolean isStateFull = context.isStateFull;
        boolean isVersionedRsrc = context.isVersionedRsrc;
        boolean isServiceWithIframe = context.isServiceWithIframe;
        boolean isRouter = context.isRouter;
        boolean isHtml = context.isHtml;
        ConfigData configData = context.configData;

        boolean foundVaryHeader = false;

        long minMaxAge = configData.minStateLessMaxAge;
        final long minMaxAgeForContentFirst = (minMaxAge < 32 ? minMaxAge : 32);

        long maxAge = CacheUtils.maxAgeOf(exchange, !isStateFull, configData);

        for (int h = 0 ; h+1 < responseHeaders.length ; h += 2)
        {
            String header = responseHeaders[h].toLowerCase();

            if (!isRouter)
            {
                cachedHeaders.add(responseHeaders[h]);
                cachedHeaders.add(responseHeaders[h+1]);

                if (status == 200)
                {
                    if (header.equals("content-length") || header.equals("content-encoding"))
                        continue;

                    if (header.equals("etag"))
                        continue;

                    if ((!isStateFull) &&
                           (header.equals("etag") ||
                            header.equals("cache-control") ||
                            header.equals("expires") ||
                            header.equals("set-cookie") ||
                           (header.equals("pragma") && responseHeaders[h+1].equalsIgnoreCase("no-cache"))))
                        continue;

                    if (isStateFull && !isServiceWithIframe && header.equals("cache-control") &&
                            responseHeaders[h+1].contains("no-store"))
                    {
                        response.addHeader("Cache-Control", "max-age=0");
                        continue;
                    }

                    if (isStateFull && isServiceWithIframe &&
                           (header.equals("cache-control") ||
                            header.equals("expires") ||
                           (header.equalsIgnoreCase("pragma") && responseHeaders[h+1].equalsIgnoreCase("no-cache"))))
                        continue;
                }
            }

            if (isHtml && header.equals("vary") )
            {
                foundVaryHeader = true;
                if (responseHeaders[h+1].toLowerCase().contains("user-agent"))
                    response.addHeader(responseHeaders[h], responseHeaders[h+1]);
                else
                    response.addHeader(responseHeaders[h], responseHeaders[h+1] + ",User-Agent");
            }
            else if (!header.equals("date"))
            {
                response.addHeader(responseHeaders[h], responseHeaders[h+1]);
            }
        }

        if (isHtml && !foundVaryHeader)
            response.addHeader("Vary", "User-Agent");

        if (isRouter)
        {
            // Not changing Cache-Control when in router-mode
        }
        else if (status != 200)
        {
            // Not changing Cache-Control when rsrc is not returned
        }
        else if (!isStateFull)
        {
            //
            // State-less Resource for whom no max-age is specified
            // is cached for 5 minutes or 1 year for versioned rsrc:
            //
            if (isVersionedRsrc)
                maxAge = 31536000;

            response.setHeader("Cache-Control", "max-age=" + maxAge);
        }
        else if (isServiceWithIframe)
        {
            // Preventing refetching content-first page from server by iframe:
            if (maxAge < minMaxAgeForContentFirst)
                maxAge = minMaxAgeForContentFirst;

            response.setHeader("Cache-Control", "max-age=" + maxAge);
        }

        String reason = exchange.getResponseReason();
        if (reason != null)
            response.setStatus(status, reason);
        else
            response.setStatus(status);
    }

    /**
     * Finds needed cached resources and load missing resources.
     *
     * @param context service-context
     * @return true when context.url shouldn't be inlined
     * @throws IOException
     */
    public static boolean loadMissingRsrcs(ServiceContext context) throws IOException
    {
        CacheStructure cache = context.cache;
        byte[] orig = context.orig;
        HttpServletRequest request = context.request;
        boolean isCss = context.isCss;
        String url = context.url;
        int[] info = context.info;
        char variant = context.variant;
        boolean isIE8 = context.isIE8;
        boolean isMobile = context.isMobile;
        ConfigData configData = context.configData;

        boolean noInline = false;

        long currentTime = System.currentTimeMillis();

        // Finding cached resources:
        int i = 0;
        int rsrcCount = 0; // Number of resources in the html-page.
        while (i < info.length)
        {
            if (info[i] == HtmlAnalyzer.EOF_KIND)
                break;
            if (info[i] == HtmlAnalyzer.IMG_KIND ||
                    info[i] == HtmlAnalyzer.CSS_IMG_KIND ||
                    info[i] == HtmlAnalyzer.INPUT_KIND ||
                    info[i] == HtmlAnalyzer.SCRIPT_KIND ||
                    info[i] == HtmlAnalyzer.LINK_KIND)
                rsrcCount++;
            i += HtmlAnalyzer.INFO_LENS[info[i]];
        }

        i = 0;
        RsrcIfc[] allRsrcs = new RsrcIfc[rsrcCount];  // All found img, js, and css resources.
        context.allRsrcs = allRsrcs;

        int iAll = 0;
        TempRsrc[] rsrcsToLoad = new TempRsrc[rsrcCount];
        BackThreadLoadInfo backThreadLoadInfo =
                                                new BackThreadLoadInfo(context.client, url, context.requestHost);
        LoadLock rsrcsLoadLock = new LoadLock();
        LoadLock rsrcsWaitLock = new LoadLock();
        synchronized(cache.globalLock)
        {
            while (i < info.length)
            {
                if (info[i] == HtmlAnalyzer.EOF_KIND)
                    break;

                int dupInfo = 0;
                boolean replace = false;

                if (info[i] == HtmlAnalyzer.IMG_KIND)
                {
                    dupInfo = info[i + HtmlAnalyzer.DUPLICATES_INFO];
                    replace = true;
                    int srcFirst = info[i + HtmlAnalyzer.IMG_SRC_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.IMG_SRC_LAST];
                    String path =  (srcFirst < 0 || srcLast < srcFirst ? "" : new String(orig, srcFirst, srcLast - srcFirst, "UTF-8"));
                    String validUrl = toValidUrl(path, context);
                    if (path.length() == 0 || path.equals("about:blank") || path.startsWith("data:") || path.contains("#") ||
                            cache.isStateFull(validUrl, configData) || variant == CacheUtils.FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT)
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }
                    allRsrcs[iAll] = checkRsrc(
                                                        context, currentTime, path, validUrl, ImageRsrc.class,
                                                        rsrcsToLoad, backThreadLoadInfo,
                                                        rsrcsLoadLock, rsrcsWaitLock, false);
                }

                else if(info[i] == HtmlAnalyzer.CSS_IMG_KIND)
                {
                    dupInfo = info[i + HtmlAnalyzer.DUPLICATES_INFO];
                    replace = true;
                    int srcFirst = info[i + HtmlAnalyzer.CSS_IMG_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.CSS_IMG_LAST];
                    String path =  (srcFirst < 0 || srcLast < srcFirst ? "" : new String(orig, srcFirst, srcLast - srcFirst, "UTF-8"));
                    String validUrl = toValidUrl(path, context);
                    if (path.length() == 0 || path.equals("about:blank") || path.startsWith("data:") ||
                            cache.isStateFull(validUrl, configData) || variant == CacheUtils.FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT)
                    {
                        if (!path.startsWith("http:") && !path.startsWith("https:"))
                            noInline = true;

                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }
                    allRsrcs[iAll] = checkRsrc(
                                                    context, currentTime, path, validUrl, ImageRsrc.class,
                                                    rsrcsToLoad, backThreadLoadInfo,
                                                    rsrcsLoadLock, rsrcsWaitLock, false);
                }

                else if(info[i] == HtmlAnalyzer.INPUT_KIND)
                {
                    dupInfo = info[i + HtmlAnalyzer.DUPLICATES_INFO];
                    replace = true;
                    int srcFirst = info[i + HtmlAnalyzer.INPUT_SRC_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.INPUT_SRC_LAST];
                    String path =  (srcFirst < 0 || srcLast < srcFirst ? "" : new String(orig, srcFirst, srcLast - srcFirst, "UTF-8"));
                    String validUrl = toValidUrl(path, context);
                    if (path.length() == 0 || path.equals("about:blank") || path.startsWith("data:") || path.contains("#") ||
                            cache.isStateFull(validUrl, configData) || variant == CacheUtils.FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT)
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }
                    allRsrcs[iAll] = checkRsrc(
                                                    context, currentTime, path, validUrl, ImageRsrc.class,
                                                    rsrcsToLoad, backThreadLoadInfo,
                                                    rsrcsLoadLock, rsrcsWaitLock, false);
                }

                else if(info[i] == HtmlAnalyzer.SCRIPT_KIND)
                {
                    dupInfo = info[i + HtmlAnalyzer.DUPLICATES_INFO];
                    replace = true;
                    int srcFirst = info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.SCRIPT_SRC_LAST];
                    String path =  (srcFirst < 0 || srcLast < srcFirst ? "" : new String(orig, srcFirst, srcLast - srcFirst));
                    String validUrl = toValidUrl(path, context);
                    if (srcFirst < 0 || dupInfo != 0 || path.contains("#") ||
                            cache.isStateFull(validUrl, configData) || variant == CacheUtils.FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT)
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }
                    allRsrcs[iAll] = checkRsrc(
                                                    context, currentTime, path, validUrl, JsRsrc.class, rsrcsToLoad,
                                                    backThreadLoadInfo, rsrcsLoadLock, rsrcsWaitLock,
                                                    info[i + HtmlAnalyzer.SCRIPT_IN_IE_COMMENT] == 1);
                }

                else if(info[i] == HtmlAnalyzer.LINK_KIND)
                {
                    dupInfo = info[i + HtmlAnalyzer.DUPLICATES_INFO];
                    replace = true;
                    int hrefFirst = info[i + HtmlAnalyzer.LINK_HREF_FIRST];
                    int hrefLast = info[i + HtmlAnalyzer.LINK_HREF_LAST];
                    if (hrefFirst < 0 || hrefLast <= hrefFirst || dupInfo != 0)
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }

                    int relFirst = info[i + HtmlAnalyzer.LINK_REL_FIRST];
                    int relLast = info[i + HtmlAnalyzer.LINK_REL_LAST];
                    if (relFirst < 0 || relLast <= relFirst)
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }
                    String rel = new String(orig, relFirst, relLast - relFirst, "UTF-8").toLowerCase();
                    if (!rel.equals("stylesheet"))
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }

                    int typeFirst = info[i + HtmlAnalyzer.LINK_TYPE_FIRST];
                    int typeLast = info[i + HtmlAnalyzer.LINK_TYPE_LAST];
                    String type = "text/css";
                    if (0 <= typeFirst && typeFirst < typeLast)
                        type =   new String(orig, typeFirst, typeLast - typeFirst, "UTF-8").toLowerCase();
                    if (!type.equals("text/css"))
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }

                    int mediaFirst = info[i + HtmlAnalyzer.LINK_MEDIA_FIRST];
                    int mediaLast = info[i + HtmlAnalyzer.LINK_MEDIA_LAST];
                    String media = "all";
                    if (0 <= mediaFirst && mediaFirst < mediaLast)
                        media = new String(orig, mediaFirst, mediaLast - mediaFirst, "UTF-8").toLowerCase();
                    if (!media.equals("all") && !media.contains("screen"))
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }

                    String href = new String(orig, hrefFirst, hrefLast - hrefFirst, "UTF-8");
                    String validUrl = toValidUrl(href, context);
                    if (cache.isStateFull(validUrl, configData) || variant == CacheUtils.FORTH_VISIT_VARIANT ||
                            variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                            variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT || validUrl.contains("#"))
                    {
                        i += HtmlAnalyzer.INFO_LENS[info[i]];
                         iAll++;
                        continue;
                    }

                    allRsrcs[iAll] = checkRsrc(
                                                    context, currentTime, href, validUrl, CssRsrc.class, rsrcsToLoad,
                                                    backThreadLoadInfo, rsrcsLoadLock, rsrcsWaitLock,
                                                    info[i + HtmlAnalyzer.LINK_IN_IE_COMMENT] == 1);
                }

                //
                // Replacing ImgRsrc, JsRsrc, CssRsrc resources by TempRsrcs.
                // The field tmpRsrc.replacer points to the replaced cached-rsrc:
                //
                if (replace)
                {
                    if (allRsrcs[iAll] instanceof TempRsrc)
                    {
                        TempRsrc tmpRsrc = (TempRsrc)allRsrcs[iAll];
                        tmpRsrc.isDuplicated = (dupInfo != 0);
                    }
                    else if (allRsrcs[iAll] != null)
                    {
                        TempRsrc tmpRsrc = new TempRsrc();
                        tmpRsrc.replacer = (Rsrc)allRsrcs[iAll];
                        tmpRsrc.host = tmpRsrc.replacer.host;
                        tmpRsrc.url = tmpRsrc.replacer.url;
                        tmpRsrc.isDuplicated = (dupInfo != 0);
                        allRsrcs[iAll] = tmpRsrc;
                    }
                    iAll++;
                }

                i += HtmlAnalyzer.INFO_LENS[info[i]];
            }
        }

        // Enqueuing rsrcs to be loaded by the background-thread:
        if (backThreadLoadInfo.tmpRsrcs != null)
        {
            while (true)
            {
                try
                {
                    _blockingQueue.put(backThreadLoadInfo);
                    break;
                }
                catch (InterruptedException e)
                {
                    try
                    {
                    Thread.sleep(1000);
                    }
                    catch (Exception exc)
                    {
                    }
                }
            }

            synchronized (ServiceUtils.class)
            {
                if (_backThread == null || !_backThread.isAlive())
                {
                    _backThread = new BackThread("HtmlSpeedServiceUtils");
                    _backThread.start();
                }
            }
        }

        loadMissingRsrcsIntoCache(
                                        context,
                                        request.getProtocol(),
                                        request.getRemoteAddr(),
                                        rsrcsToLoad,
                                        rsrcsLoadLock,
                                        rsrcsWaitLock);

        //
        // Replacing TempRsrc instances by replacers in allRsrcs. The code bellow
        // decides when to use versioned-resources instead of the non-inlined original
        // resources. When it decide to use versioned-resources, tempRsrc.isVersioned
        // is set to true and tempRsrc is not replaced by replacer:
        //

        final int COUNTS_CSS = 0; // Lower rsrc-kinds are not inlined before higher rsrc-kinds:
        final int COUNTS_LARGE_JS = 1;
        final int COUNTS_LARGE_IMG = 2;
        final int COUNTS_MEDIUM_JS = 3;
        final int COUNTS_MEDIUM_IMG = 4;
        final int COUNTS_SMALL_JS = 5;
        final int COUNTS_SMALL_IMG = 6;
        final int COUNTS_TINY_JS = 7;
        final int COUNTS_TINY_IMG = 8;
        final int COUNTS_LEN = 9;
        int countsAll = 0; // Total number of inlinable rsrcs.
        int[] counts = new int[COUNTS_LEN]; // counts[i]: number of rsrcs of kind i that are allowed to be inlined

        for (int step = 0 ;
                step < (!isCss && ((CacheUtils.FIRST_VISIT_VARIANT < variant &&
                                                variant < CacheUtils.FORTH_VISIT_VARIANT) ||
                                            (CacheUtils.IE8_FIRST_VISIT_VARIANT < variant &&
                                                variant < CacheUtils.IE8_FORTH_VISIT_VARIANT) ||
                                            (CacheUtils.MOBILE_FIRST_VISIT_VARIANT < variant &&
                                                variant < CacheUtils.MOBILE_FORTH_VISIT_VARIANT)) ? 2 : 1) ;
                step++)
        {
            //
            // When step == 0 counts the number of inlinable rsrcs.
            // When step == 1 (which is possible only when variant is not
            // [IE8_ | MOBILE_]FIRST_VISIT_VARIANT or [IE8_ | MOBILE_]FORTH_VISIT_VARIANT)
            // allows inline of correct number of rsrcs of each rsrc-kind:
            //

            if (step == 1)
            {
                //
                // Adjusting elements of counts so that inline of lower rsrc-kinds
                // is prevented before inline of higher rsrc-kinds, and the sum of
                // all elements of counts equals max' allowed inlined elements:
                //
                int noInlinedCount = 0;
                if (variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                        variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                        variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                    noInlinedCount = countsAll / 3;
                else if (variant == CacheUtils.SECOND_VISIT_VARIANT ||
                        variant == CacheUtils.IE8_SECOND_VISIT_VARIANT ||
                        variant == CacheUtils.MOBILE_SECOND_VISIT_VARIANT)
                    noInlinedCount = countsAll / 3;
                else if (variant == CacheUtils.THIRD_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_THIRD_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_THIRD_VISIT_VARIANT)
                    noInlinedCount = countsAll * 2 / 3;
                for (int ii = 0 ; ii < counts.length ; ii++)
                {
                    if (noInlinedCount >= counts[ii])
                    {
                        noInlinedCount -= counts[ii];
                        counts[ii] = 0;
                    }
                    else
                    {
                        counts[ii] -= noInlinedCount;
                        noInlinedCount = 0;
                        break;
                    }
                }
            }

            for (int a = 0 ; a < allRsrcs.length ; a++)
            {
                if (allRsrcs[a] instanceof TempRsrc)
               {
                    TempRsrc tempRsrc = (TempRsrc)allRsrcs[a];

                    Rsrc replacer = tempRsrc.replacer;

                    if(replacer.versionUrl != null)
                        tempRsrc.isVersioned = true;

                    if (variant == CacheUtils.FORTH_VISIT_VARIANT ||
                            variant == CacheUtils.IE8_FORTH_VISIT_VARIANT ||
                            variant == CacheUtils.MOBILE_FORTH_VISIT_VARIANT)
                    {
                        // Nothing is inlined from forth visit.
                    }
                    else if(replacer.origData == null)
                    {
                    }
                    else if((tempRsrc.isDuplicated && replacer.origData.length > 256) ||
                                tempRsrc.isStateFull ||
                                (replacer instanceof ImageRsrc && ((ImageRsrc)replacer).base64Data == null) ||
                                ((isIE8 || isCss) && replacer instanceof ImageRsrc &&
                                    ((ImageRsrc)replacer).base64Data.length > 32*1024) ||
                                (replacer.origData.length >= configData.minHugeBuffer) ||
                                (replacer.origData.length >= 8*1024 && isMobile) ||
                                (isCss && configData.isNoInline(tempRsrc.url)))
                    {
                        if (isCss)
                            noInline = true; // relevant only for css-imgs in stylesheets
                    }
                    else if (replacer instanceof JsRsrc && !((JsRsrc)replacer).isInlinable)
                    {
                    }
                    else if (replacer instanceof CssRsrc && !((CssRsrc)replacer).isInlinable)
                    {
                    }
                    else
                    {
                        // rsrc is inlinable:

                        if (variant == CacheUtils.FIRST_VISIT_VARIANT ||
                                variant == CacheUtils.IE8_FIRST_VISIT_VARIANT ||
                                variant == CacheUtils.MOBILE_FIRST_VISIT_VARIANT ||
                                isCss)
                        {
                            allRsrcs[a] = replacer; // During first-visit inlinable rsrcs are inlined
                        }
                        else if(step == 0)
                        {
                            //
                            // Counting inlineable resources:
                            //
                            countsAll++;

                            if (replacer instanceof CssRsrc)
                            {
                                counts[COUNTS_CSS]++;
                            }
                            else if (replacer instanceof JsRsrc)
                            {
                                if (replacer.origData.length >= configData.minLargeBuffer &&
                                        replacer.origData.length < configData.minHugeBuffer)
                                    counts[COUNTS_LARGE_JS]++;
                                else if (replacer.origData.length >= configData.minMediumBuffer)
                                    counts[COUNTS_MEDIUM_JS]++;
                                else if (replacer.origData.length >= configData.minSmallBuffer)
                                    counts[COUNTS_SMALL_JS]++;
                                else
                                    counts[COUNTS_TINY_JS]++;
                            }
                            else if (replacer instanceof ImageRsrc)
                            {
                                if (replacer.origData.length >= configData.minLargeBuffer &&
                                        replacer.origData.length < configData.minHugeBuffer)
                                    counts[COUNTS_LARGE_IMG]++;
                                else if (replacer.origData.length >= configData.minMediumBuffer)
                                    counts[COUNTS_MEDIUM_IMG]++;
                                else if (replacer.origData.length >= configData.minSmallBuffer)
                                    counts[COUNTS_SMALL_IMG]++;
                                else
                                    counts[COUNTS_TINY_IMG]++;
                            }
                        }
                        else // step == 1
                        {
                            //
                            // Allowing correct num of rsrcs of each kind to be inlined:
                            //
                            if (replacer instanceof CssRsrc)
                            {
                                if (counts[COUNTS_CSS]-- > 0)
                                    allRsrcs[a] = replacer;
                            }
                            else if (replacer instanceof JsRsrc)
                            {
                                if (replacer.origData.length >= configData.minHugeBuffer)
                                {
                                }
                                else if (replacer.origData.length >= configData.minLargeBuffer)
                                {
                                    if (counts[COUNTS_LARGE_JS]-- > 0)
                                        allRsrcs[a] = replacer;
                                }
                                else if (replacer.origData.length >= configData.minMediumBuffer)
                                {
                                    if (counts[COUNTS_MEDIUM_JS]-- > 0)
                                        allRsrcs[a] = replacer;
                                }
                                else if (replacer.origData.length >= configData.minSmallBuffer)
                                {
                                    if (counts[COUNTS_SMALL_JS]-- > 0)
                                        allRsrcs[a] = replacer;
                                }
                                else
                                {
                                    if (counts[COUNTS_TINY_JS]-- > 0)
                                        allRsrcs[a] = replacer;
                                }
                            }
                            else if (replacer instanceof ImageRsrc)
                            {
                                if (replacer.origData.length >= configData.minHugeBuffer)
                                {
                                }
                                else if (replacer.origData.length >= configData.minLargeBuffer)
                                {
                                    if (counts[COUNTS_LARGE_IMG]-- > 0 ||
                                            variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                                        allRsrcs[a] = replacer;
                                }
                                else if (replacer.origData.length >= configData.minMediumBuffer)
                                {
                                    if (counts[COUNTS_MEDIUM_IMG]-- > 0 ||
                                            variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                                        allRsrcs[a] = replacer;
                                }
                                else if (replacer.origData.length >= configData.minSmallBuffer)
                                {
                                    if (counts[COUNTS_SMALL_IMG]-- > 0 ||
                                            variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                                        allRsrcs[a] = replacer;
                                }
                                else
                                {
                                    if (counts[COUNTS_TINY_IMG]-- > 0 ||
                                            variant == CacheUtils.FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT ||
                                            variant == CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT)
                                        allRsrcs[a] = replacer;
                                }
                            }
                            else
                            {
                                allRsrcs[a] = replacer;
                            }
                        }
                    }
                }

                if (allRsrcs[a] instanceof TempRsrc)
                     Cdn.handleCdn((TempRsrc)allRsrcs[a], configData);
            }
        }

        return noInline;
    }

    /**
     * Loads rsrcsToLoad and inserts loaded rsrcs into the cache-structure.
     *
     * Called when current service loads missing rsrcs of requested state-less url,
     * or when background-thread loads missing rsrcs of requested state-full urls.
     *
     * @param context http-service context
     * @param defaultProtocol http protocol (example HTTP/1.1)
     * @param requestRemoteAddr client invoking http-request
     * @param rsrcsToLoad list of resources to load
     * @param rsrcsLoadLock used for waiting for end of loads initiated by current thread
     * @param rsrcsWaitLock used for waiting for end of loads initiated by other threads
     * @throws IOException 
     */
    private static void loadMissingRsrcsIntoCache(
                                                    ServiceContext context,
                                                    String protocol,
                                                    String requestRemoteAddr,
                                                    TempRsrc[] rsrcsToLoad,
                                                    LoadLock rsrcsLoadLock,
                                                    LoadLock rsrcsWaitLock
                                                    ) throws IOException
    {
        CacheStructure cache = context.cache;
        boolean isHttps = context.isHttps;
        String url = context.url;
        ConfigData configData = context.configData;

        // Loading missing resources:
        if (rsrcsLoadLock.getCount() > 0)
        {
            loadMissingRsrcs(context, rsrcsToLoad, isHttps, protocol, rsrcsLoadLock);

            // Waiting for load to finish:
            try
            {
                if (configData.isDebug)
                    System.out.println("Start waiting for rsrcLoadLock, " + url);

                rsrcsLoadLock.waitUntilCountIs0();

                if (configData.isDebug)
                    System.out.println("End waiting for rsrcLoadLock, " + url);
            }
            catch (InterruptedException e)
            {
            }

            // Preparing replacers of loaded resources before insertion into cache-structure:
            for (RsrcIfc r : rsrcsToLoad)
            {
                if (!(r instanceof TempRsrc))
                    continue;

                TempRsrc tr = (TempRsrc)r;

                if (tr.exchange == null)
                    continue; // Loader already handled end-of-load.

                if (tr.exchange.getResponseStatus() == 200 &&
                        CacheUtils.isStateFull(tr.url, tr.exchange, false /*isHtml*/, configData))
                {
                    tr.isStateFull = true;
                    continue;
                }

                if (_isWithIframe &&  !context.isRouter && tr.exchange.getResponseStatus() == 200 &&
                        tr.replacer instanceof JsRsrc && !context.isIE8 &&
                        configData.isContentFirst(tr.url, false /* wildcardsAllowed */))
                    tr.exchange.handleLocationUpdates();

                if (tr.exchange.isCommFailure())
                    reportUnhealthyContentServer(tr.exchange.getContentServerAddress());

                if (tr.isBeingRefreshed && tr.exchange.getResponseStatus() == 304)
                {
                    long maxAge = CacheUtils.maxAgeOf(tr.exchange, !tr.isStateFull, configData);
                    endRefreshAndNotifyWaiters(tr.exchange, tr, null, maxAge, configData);
                    tr.exchange = null;
                    if (configData.isDebug)
                        System.out.println("URL: " + tr.url + " from ip: " + requestRemoteAddr + " (refreshed)");
                }
                else if (tr.exchange.getResponseStatus() == 200 && tr.exchange.getUngzipedResponseContent() != null)
                {
                    // Load succeeded:
                    tr.replacer.host = tr.host;
                    tr.replacer.variant = CacheUtils.NON_PAGE_VARIANT;
                    tr.replacer.url = tr.url;
                    tr.replacer.origData = tr.exchange.getUngzipedResponseContent();
                    tr.replacer.httpHeaders = tr.exchange.getResponseHeaders();
                    if (tr.exchange.isGziped())
                    {
                        tr.replacer.origGzip = tr.exchange.getResponseContent();
                        tr.replacer.origGzipEncoding = "gzip";
                    }
                    else if (tr.exchange.isDeflated())
                    {
                        tr.replacer.origGzip = tr.exchange.getResponseContent();
                        tr.replacer.origGzipEncoding = "deflate";
                    }
                    else if (tr.replacer instanceof JsRsrc || tr.replacer instanceof CssRsrc)
                    {
                        if (!tr.exchange.isGziped() && !tr.exchange.isDeflated() &&
                                tr.exchange.getUngzipedResponseContent().length >= MIN_GZIP_LEN)
                        {
                            tr.replacer.origGzip = toGzip(tr.replacer.origData);
                            tr.replacer.origGzipEncoding = "gzip";
                        }                                             
                    }

                    if (tr.replacer instanceof ImageRsrc)
                    {
                        ImageRsrc ir = (ImageRsrc)tr.replacer;
                        String mime = new ImageAnalyzer(ir.origData).getMime();
                        if (mime != null)
                        {
                            tr.replacer.versionUrl =
                                    CacheUtils.versionUrlOf(
                                                        context.host, tr.url,
                                                        CacheUtils.NON_PAGE_VARIANT, ir.origData, tr.exchange, configData);
                            if (configData.jpegMin >= 0 && ir.origData.length >= configData.jpegMin && mime.endsWith("jpeg"))
                            {
                                ir.origData = ImageUtils.optimizedJpeg(ir.origData, configData);
                                ir.isOptimized = true;
                            }
                            ir.base64Data = ("data:" + mime + ";base64," +
                                                ImageUtils.encode(ir.origData)).getBytes();
                        }
                    }
                    else if (tr.replacer instanceof JsRsrc)
                    {
                        tr.replacer.versionUrl =
                                CacheUtils.versionUrlOf(
                                                    context.host, tr.url,
                                                    CacheUtils.NON_PAGE_VARIANT, tr.replacer.origData, tr.exchange, configData);
                        JsRsrc jsRsrc = (JsRsrc)tr.replacer;
                        jsRsrc.isInIEComment = tr.isInIEComment;
                        jsRsrc.checkInlinable(configData);
                    }
                    else if (tr.replacer instanceof CssRsrc)
                    {
                        CssRsrc cssRsrc = (CssRsrc)tr.replacer;
                        cssRsrc.isInIEComment = tr.isInIEComment;
                        cssRsrc.url = tr.url;
                        cssRsrc.checkInlinable(configData);
                    }

                    long maxAge = CacheUtils.maxAgeOf(tr.exchange, !tr.isStateFull, configData);
                    tr.replacer.lastRefreshTime = System.currentTimeMillis();
                    tr.replacer.maxFreshTime = tr.replacer.lastRefreshTime + maxAge*1000;
                    tr.replacer.origEtag =
                            (tr.exchange.getEtagIndex() >= 0 ?
                                tr.exchange.getResponseHeaders()[
                                                    tr.exchange.getEtagIndex() + 1] :
                                null);
                    tr.replacer.origLastModified =
                            (tr.exchange.getLastModifiedIndex() >= 0 ?
                                tr.exchange.getResponseHeaders()[
                                                    tr.exchange.getLastModifiedIndex() + 1] :
                                null);
                    tr.replacer.origMd5 = CacheUtils.md5Of(tr.replacer.origData);
                    if (tr.replacer.versionUrl != null)
                        tr.replacer.versionUrlBytes = tr.replacer.versionUrl.getBytes();

                }
            }

            // Replacing TempRsrc instances in cache-structure by replacers:
            synchronized (cache.globalLock)
            {
                for (RsrcIfc r : rsrcsToLoad)
                {
                    if (!(r instanceof TempRsrc))
                        continue;

                    TempRsrc tr = (TempRsrc)r;

                    if (tr.exchange != null &&
                            !tr.isStateFull &&
                            tr.exchange.getResponseStatus() == 200 &&
                            tr.exchange.getUngzipedResponseContent() != null)
                    {
                        // Load succeeded:
                        putInCache(tr.replacer.variant, tr.replacer.url, tr.replacer, configData);
                        if (tr.replacer.versionUrl != null)
                            putInCache(tr.replacer.variant, tr.replacer.versionUrl, tr.replacer, configData);
                        tr.replacer.isBeingLoaded = false;

                        if (tr.isBeingRefreshed && configData.isDebug)
                            System.out.println("URL: " + tr.url + " from ip: " + requestRemoteAddr+ " (refreshed - modified)");
                    }
                    else
                    {
                        if (tr.isBeingRefreshed)
                        {
                            // Rollback and postpone the refresh of rsrc by 1 minute:
                            synchronized (cache.globalLock)
                            {
                                tr.isBeingRefreshed = false;
                                Rsrc rsrc = tr.replacer;
                                rsrc.isBeingLoaded = false;
                                rsrc.lastRefreshTime = System.currentTimeMillis();
                                rsrc.maxFreshTime = rsrc.lastRefreshTime + 60*1000;
                                putInCache(CacheUtils.NON_PAGE_VARIANT, tr.url, rsrc, configData);
                            }
                        }
                        else
                        {
                            removeFromCache(CacheUtils.NON_PAGE_VARIANT, tr.url, configData);

                            if (tr.replacer.versionUrl != null)
                                removeFromCache(CacheUtils.NON_PAGE_VARIANT, tr.replacer.versionUrl, configData);

                            if (tr.isStateFull)
                                cache.addStateFull(tr.url);
                        }
                    }

                    if (tr.waiters != null)
                    {
                        for (LoadLock w : tr.waiters)
                            w.decCount(); // Notifying waiters
                        tr.waiters = null;
                    }

                    tr.exchange = null;
                }
            }

        }

        try
        {
            if (rsrcsWaitLock != null)
            {
                if (configData.isDebug)
                    System.out.println("Start waiting for rsrcWaitLock, " + url);

                rsrcsWaitLock.waitUntilCountIs0();

                if (configData.isDebug)
                    System.out.println("End waiting for rsrcWaitLock, " + url);
            }
        }
        catch (InterruptedException e)
        {
        }

    }

    /**
     * Optimizing stylesheet used by current html page by inlining images
     *
     * @param context service-context
     * @throws IOException
     */
    public static void optimizeStylesheets(ServiceContext context) throws IOException
    {
        int[] info = context.info;
        RsrcIfc[] allRsrcs = context.allRsrcs;
        ConfigData configData = context.configData;

        int iAll = 0;
        int i = 0;
        while (i < info.length)
        {
            if (info[i] == HtmlAnalyzer.EOF_KIND)
                break;

            if (info[i] == HtmlAnalyzer.LINK_KIND && allRsrcs[iAll] instanceof CssRsrc)
            {
                CssRsrc cssRsrc = (CssRsrc)allRsrcs[iAll];

                // Loading images referenced by stylesheet:
                if (!cssRsrc.isOptimized)
                {
                    cssRsrc.isOptimized = true;
                    if (configData.isDebug)
                        System.out.println("Loading images of stylesheet: " + cssRsrc.url);
                    ServiceContext cssContext = new ServiceContext();
                    cssContext.client = context.client;
                    cssContext.cache = context.cache;
                    cssContext.isCss = true;
                    cssContext.variant = CacheUtils.NON_PAGE_VARIANT;
                    cssContext.isHtml = false;
                    cssContext.isHttps = context.isHttps;
                    cssContext.isServiceWithIframe = false;
                    cssContext.base = context.base;
                    cssContext.isStateFull = false;
                    cssContext.orig = cssRsrc.origData;
                    cssContext.host = context.host;
                    cssContext.requestHost = context.requestHost;
                    cssContext.configData = context.configData;

                    HtmlAnalyzer cssAnalyzer = new HtmlAnalyzer();
                    int[] cssInfo = cssAnalyzer.analyze(cssContext);
                    cssContext.info = cssInfo;

                    cssContext.request = context.request;
                    cssContext.base = cssRsrc.url;
                    cssContext.url = cssRsrc.url;

                    boolean noInline = ServiceUtils.loadMissingRsrcs(cssContext);
                    if (noInline)
                        cssRsrc.isInlinable = false;

                    if (cssRsrc.isInlinable)
                    {
                        byte[] optimData = ServiceUtils.buildResponseToBrowser(cssContext, false /* isGziped */);
                        cssRsrc.optimData = optimData;
                        cssRsrc.optimGzip = toGzip(optimData);
                        cssRsrc.optimGzipEncoding = "gzip";
                    }
                    else
                    {
                        byte[] optimGzip = ServiceUtils.buildResponseToBrowser(cssContext, true /* isGziped */);
                        cssRsrc.optimGzip = optimGzip;
                        cssRsrc.optimGzipEncoding = "gzip";
                    }
                }

                if (!cssRsrc.isInlinable && allRsrcs[iAll] instanceof CssRsrc)
                    allRsrcs[iAll] = null;
            }

            if (info[i] == HtmlAnalyzer.IMG_KIND ||
                    info[i] == HtmlAnalyzer.CSS_IMG_KIND ||
                    info[i] == HtmlAnalyzer.INPUT_KIND ||
                    info[i] == HtmlAnalyzer.SCRIPT_KIND ||
                    info[i] == HtmlAnalyzer.LINK_KIND)
                iAll++;

            i += HtmlAnalyzer.INFO_LENS[info[i]];
        }
    }

    /**
     * @param bytes src array of byte
     * @return bytes converted to gzip
     * @throws IOException
     */
    public static byte[] toGzip(byte[] bytes) throws IOException
    {
        ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(bytes.length*3/4);
        OutputStream oGzip = new GZIPOutputStream(oByteArr);
        oGzip.write(bytes);
        oGzip.close();
        byte[] out = oByteArr.toByteArray();
        return out;
    }

    /**
     * @param bytes gziped array of byte
     * @return ungziped bytes
     * @throws IOException
     */
    public static byte[] toUngzip(byte[] bytes) throws IOException
    {
        ByteArrayInputStream bais = new ByteArrayInputStream(bytes);
        GZIPInputStream is = new GZIPInputStream(bais);
        byte[] buff = new byte[16*10240];
        ByteArrayOutputStream os = new ByteArrayOutputStream(bytes.length * 2);
        int n;
        while ((n = is.read(buff)) >= 0)
        {
            os.write(buff, 0, n);
        }
        byte[] out = os.toByteArray();
        is.close();
        os.close();
        return out;
    }

    /**
     * Builds the response to be returned to the browser
     *
     * @param context service-context
     * @param isGziped true when response is gziped
     * @return response gziped content to be returned to browser
     */
    public static byte[] buildResponseToBrowser(ServiceContext context, boolean isGziped) throws IOException
    {
        byte[] orig = context.orig;
        RsrcIfc[] allRsrcs = context.allRsrcs;
        int[] info = context.info;
        boolean isServiceWithIframe = context.isServiceWithIframe;
        boolean isIE = context.isIE;
        boolean isHttps = context.isHttps;
        ConfigData configData = context.configData;

        boolean isIframeDumped = false; // True after iframe creation is dumped.
        boolean isFuncsDumped = false; // True after iframe-funcs are dumped.

        // Inlining resources:
        int iAll = 0;
        ByteArrayOutputStream oByteArr = new ByteArrayOutputStream(orig.length*3/4);
        OutputStream oGzip = (isGziped ? new OurGZIPOutputStream(oByteArr) : oByteArr);
        int iOrig = 0;
        int i = 0;
        while (i < info.length)
        {
            if (info[i] == HtmlAnalyzer.EOF_KIND)
                break;

            if (info[i] == HtmlAnalyzer.BASE_KIND && configData.hostsMap != null && context.base != null)
            {
                int first = info[i + HtmlAnalyzer.BASE_HREF_FIRST];
                int last = info[i + HtmlAnalyzer.BASE_HREF_LAST];
                oGzip.write(orig, iOrig, first - iOrig);
                oGzip.write(context.base.getBytes());
                iOrig = last;
            }

            else if (info[i] == HtmlAnalyzer.EVENT_KIND && isServiceWithIframe &&
                        info[i + HtmlAnalyzer.EVENT_FIRST] > 0)
            {
                int first = info[i + HtmlAnalyzer.EVENT_FIRST];
                int last = info[i + HtmlAnalyzer.EVENT_LAST];
                oGzip.write(orig, iOrig, first - iOrig);
                oGzip.write(IF_IN_IFRAME_BYTES);
                oGzip.write(orig, first, last - first);
                oGzip.write(IF_END_BYTES);
                iOrig = last;
            }

            else if (info[i] == HtmlAnalyzer.IMG_KIND && info[i + HtmlAnalyzer.IMG_SRC_FIRST] > 0 &&
                                    allRsrcs[iAll] instanceof ImageRsrc && ((ImageRsrc)allRsrcs[iAll]).base64Data != null)
            {
                int srcFirst = info[i + HtmlAnalyzer.IMG_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.IMG_SRC_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                ImageRsrc imgRsrc = (ImageRsrc)allRsrcs[iAll];
                byte[] imgBytes = imgRsrc.base64Data;
                oGzip.write(imgBytes, 0, imgBytes.length);
                iOrig = srcLast;
            }

            else if (info[i] == HtmlAnalyzer.IMG_KIND && info[i + HtmlAnalyzer.IMG_SRC_FIRST] > 0 &&
                                    allRsrcs[iAll] instanceof TempRsrc && ((TempRsrc)allRsrcs[iAll]).cdnUrl != null)
            {
                int srcFirst = info[i + HtmlAnalyzer.IMG_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.IMG_SRC_LAST];
                byte[] cdnUrl = (isHttps ? ((TempRsrc)allRsrcs[iAll]).cdnSslUrl: ((TempRsrc)allRsrcs[iAll]).cdnUrl);
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                oGzip.write(cdnUrl);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.IMG_KIND && info[i + HtmlAnalyzer.IMG_SRC_FIRST] > 0 &&
                                    allRsrcs[iAll] instanceof TempRsrc && ((TempRsrc)allRsrcs[iAll]).isVersioned &&
                                    ((TempRsrc)allRsrcs[iAll]).replacer instanceof ImageRsrc)
            {
                // Replacing src of image by src versioned-resource:
                int srcFirst = info[i + HtmlAnalyzer.IMG_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.IMG_SRC_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                ImageRsrc imgRsrc = (ImageRsrc)((TempRsrc)allRsrcs[iAll]).replacer;
                oGzip.write(imgRsrc.versionUrlBytes);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.CSS_IMG_KIND && info[i + HtmlAnalyzer.CSS_IMG_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof ImageRsrc && ((ImageRsrc)allRsrcs[iAll]).base64Data != null)
            {
                int srcFirst = info[i + HtmlAnalyzer.CSS_IMG_FIRST];
                int srcLast = info[i + HtmlAnalyzer.CSS_IMG_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                ImageRsrc imgRsrc = (ImageRsrc)allRsrcs[iAll];
                byte[] imgBytes = imgRsrc.base64Data;
                oGzip.write(imgBytes, 0, imgBytes.length);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.CSS_IMG_KIND && info[i + HtmlAnalyzer.CSS_IMG_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof ImageRsrc && ((TempRsrc)allRsrcs[iAll]).cdnUrl != null)
            {
                int srcFirst = info[i + HtmlAnalyzer.CSS_IMG_FIRST];
                int srcLast = info[i + HtmlAnalyzer.CSS_IMG_LAST];
                byte[] cdnUrl = (isHttps ? ((TempRsrc)allRsrcs[iAll]).cdnSslUrl: ((TempRsrc)allRsrcs[iAll]).cdnUrl);
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                oGzip.write(cdnUrl);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.CSS_IMG_KIND && info[i + HtmlAnalyzer.CSS_IMG_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof TempRsrc && ((TempRsrc)allRsrcs[iAll]).isVersioned &&
                            ((TempRsrc)allRsrcs[iAll]).replacer instanceof ImageRsrc)
            {
                // Replacing src of css-image by src versioned-resource:
                int srcFirst = info[i + HtmlAnalyzer.CSS_IMG_FIRST];
                int srcLast = info[i + HtmlAnalyzer.CSS_IMG_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                ImageRsrc imgRsrc = (ImageRsrc)((TempRsrc)allRsrcs[iAll]).replacer;
                oGzip.write(imgRsrc.versionUrlBytes);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.INPUT_KIND && info[i + HtmlAnalyzer.INPUT_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof ImageRsrc && ((ImageRsrc)allRsrcs[iAll]).base64Data != null)
            {
                int srcFirst = info[i + HtmlAnalyzer.INPUT_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.INPUT_SRC_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                ImageRsrc imgRsrc = (ImageRsrc)allRsrcs[iAll];
                byte[] imgBytes = imgRsrc.base64Data;
                oGzip.write(imgBytes, 0, imgBytes.length);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.FRAME_KIND && info[i + HtmlAnalyzer.FRAME_SRC_FIRST] > 0 &&
                            info[i + HtmlAnalyzer.FRAME_SRC_FIRST] == info[i + HtmlAnalyzer.FRAME_SRC_LAST])
            {
                int srcFirst = info[i + HtmlAnalyzer.FRAME_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.FRAME_SRC_LAST];
                oGzip.write(orig, iOrig, srcFirst - iOrig);
                oGzip.write(ABOUT_BLANK, 0, ABOUT_BLANK.length);
                iOrig = srcLast;
            }

            else if(info[i] == HtmlAnalyzer.IFRAME_KIND && info[i + HtmlAnalyzer.IFRAME_SRC_FIRST] > 0 &&
                    info[i + HtmlAnalyzer.IFRAME_SRC_FIRST] < info[i + HtmlAnalyzer.IFRAME_SRC_LAST] &&
                    isServiceWithIframe)
            {
                int ifrmFirst = info[i + HtmlAnalyzer.IFRAME_FIRST];
                int ifrmLast = info[i + HtmlAnalyzer.IFRAME_LAST];
                int srcFirst = info[i + HtmlAnalyzer.IFRAME_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.IFRAME_SRC_LAST];
                oGzip.write(orig, iOrig, ifrmFirst - iOrig);
                oGzip.write(SCRIPT_START_BYTES, 0 , SCRIPT_START_BYTES.length);

                oGzip.write(DOCUMENT_WRITE_START_BYTES, 0, DOCUMENT_WRITE_START_BYTES.length);
                iOrig = ifrmFirst;
                while (iOrig < srcFirst)
                {
                    if (orig[iOrig] == '\'')
                        oGzip.write('"');
                    else if (orig[iOrig] < ' ')
                        oGzip.write(' ');
                    else
                        oGzip.write(orig[iOrig]);
                    iOrig++;
                }
                oGzip.write(DOCUMENT_WRITE_END_BYTES, 0, DOCUMENT_WRITE_END_BYTES.length);

                oGzip.write(IF_START_BYTES, 0, IF_START_BYTES.length);

                oGzip.write(DOCUMENT_WRITE_START_BYTES, 0, DOCUMENT_WRITE_START_BYTES.length);
                iOrig = srcFirst;
                oGzip.write(orig, iOrig, srcLast - iOrig);
                oGzip.write(DOCUMENT_WRITE_END_BYTES, 0, DOCUMENT_WRITE_END_BYTES.length);

                oGzip.write(ELSE_BYTES, 0, ELSE_BYTES.length);

                oGzip.write(DOCUMENT_WRITE_START_BYTES, 0, DOCUMENT_WRITE_START_BYTES.length);
                oGzip.write(ABOUT_BLANK, 0, ABOUT_BLANK.length);
                oGzip.write(DOCUMENT_WRITE_END_BYTES, 0, DOCUMENT_WRITE_END_BYTES.length);

                oGzip.write(IF_END_BYTES, 0, IF_END_BYTES.length);

                oGzip.write(DOCUMENT_WRITE_START_BYTES, 0, DOCUMENT_WRITE_START_BYTES.length);
                iOrig = srcLast;
                while (iOrig < ifrmLast)
                {
                    if (orig[iOrig] == '\'')
                        oGzip.write('"');
                    else if (orig[iOrig] < ' ')
                        oGzip.write(' ');
                    else
                        oGzip.write(orig[iOrig]);
                    iOrig++;
                }
                oGzip.write(DOCUMENT_WRITE_END_BYTES, 0, DOCUMENT_WRITE_END_BYTES.length);

                oGzip.write(SCRIPT_END_BYTES, 0 , SCRIPT_END_BYTES.length);
                iOrig = ifrmLast;
            }

            else if(info[i] == HtmlAnalyzer.SCRIPT_KIND && info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST] < 0 &&
                            info[i + HtmlAnalyzer.SCRIPT_START_LAST] > 0 && isServiceWithIframe && isRealyJs(orig, info, i))
            {
                if (!isIframeDumped && info[i + HtmlAnalyzer.SCRIPT_IN_IE_COMMENT] == 0)
                {
                    int startFirst = info[i + HtmlAnalyzer.SCRIPT_START_FIRST];
                    oGzip.write(orig, iOrig, startFirst - iOrig);
                    iOrig = startFirst;
                    if (!isFuncsDumped)
                    {
                        isFuncsDumped = true;
                        if (_isDelayIframe)
                            oGzip.write(HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES, 0, HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES.length);
                        else
                            oGzip.write(HTML_SPEED_FUNCS_BYTES, 0, HTML_SPEED_FUNCS_BYTES.length);
                    }
                    if (isIE)
                        oGzip.write(IFRAME_IE_BYTES, 0, IFRAME_IE_BYTES.length);
                    else
                        oGzip.write(IFRAME_BYTES, 0, IFRAME_BYTES.length);
                    isIframeDumped = true;
                }
                int bodyFirst = info[i + HtmlAnalyzer.SCRIPT_START_LAST];
                int bodyLast = info[i + HtmlAnalyzer.SCRIPT_END_FIRST];
                oGzip.write(orig, iOrig, bodyFirst - iOrig);
                oGzip.write(IF_IN_IFRAME_BYTES, 0, IF_IN_IFRAME_BYTES.length);
                oGzip.write(orig, bodyFirst, bodyLast - bodyFirst);
                oGzip.write('\n');
                oGzip.write('}');
                iOrig = bodyLast;
            }

            else if(info[i] == HtmlAnalyzer.SCRIPT_KIND && info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof TempRsrc &&
                            (((TempRsrc)allRsrcs[iAll]).isVersioned || ((TempRsrc)allRsrcs[iAll]).cdnUrl != null) &&
                            ((TempRsrc)allRsrcs[iAll]).replacer instanceof JsRsrc)
            {
                // Replacing src of java-script by src versioned-resource or cdn-resource:
                TempRsrc tempRsrc = (TempRsrc)allRsrcs[iAll];
                JsRsrc jsRsrc = (JsRsrc)(tempRsrc).replacer;
                if (!isServiceWithIframe)
                {
                    int srcFirst = info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.SCRIPT_SRC_LAST];
                    oGzip.write(orig, iOrig, srcFirst - iOrig);
                    if (!isHttps && tempRsrc.cdnUrl != null)
                        oGzip.write(tempRsrc.cdnUrl);
                    else if (isHttps && tempRsrc.cdnSslUrl != null)
                        oGzip.write(tempRsrc.cdnSslUrl);
                    else
                        oGzip.write(jsRsrc.versionUrlBytes);
                    iOrig = srcLast;
                }
                else
                {
                    int startFirst = info[i + HtmlAnalyzer.SCRIPT_START_FIRST];
                    int endLast = info[i + HtmlAnalyzer.SCRIPT_END_LAST];
                    int srcFirst = info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST];
                    int srcLast = info[i + HtmlAnalyzer.SCRIPT_SRC_LAST];
                    oGzip.write(orig, iOrig, startFirst - iOrig);
                    if (!isIframeDumped && info[i + HtmlAnalyzer.SCRIPT_IN_IE_COMMENT] == 0)
                    {
                        if (!isFuncsDumped)
                        {
                            isFuncsDumped = true;
                            if (_isDelayIframe)
                                oGzip.write(HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES, 0, HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES.length);
                            else
                                oGzip.write(HTML_SPEED_FUNCS_BYTES, 0, HTML_SPEED_FUNCS_BYTES.length);
                        }
                        if (isIE)
                            oGzip.write(IFRAME_IE_BYTES, 0, IFRAME_IE_BYTES.length);
                        else
                            oGzip.write(IFRAME_BYTES, 0, IFRAME_BYTES.length);
                        isIframeDumped = true;
                    }
                    if (configData.isDefer(jsRsrc.url))
                        oGzip.write(SCRIPT_IN_IFRAME_START_DEFER_BYTES, 0, SCRIPT_IN_IFRAME_START_DEFER_BYTES.length);
                    else
                        oGzip.write(SCRIPT_IN_IFRAME_START_BYTES, 0, SCRIPT_IN_IFRAME_START_BYTES.length);
                    int j;
                    for (j = startFirst + 7 ; j < srcFirst ; j++)
                    {
                        switch (orig[j])
                        {
                            case '\'':
                                oGzip.write('\\');
                                oGzip.write(orig[j]);
                                break;
                            case '\n':
                            case '\r':
                                oGzip.write(' ');
                                break;
                            default:
                                oGzip.write(orig[j]);
                                break;
                        }
                    }

                    if (!isHttps && tempRsrc.cdnUrl != null)
                        oGzip.write(tempRsrc.cdnUrl);
                    else if (isHttps && tempRsrc.cdnSslUrl != null)
                        oGzip.write(tempRsrc.cdnSslUrl);
                    else
                        oGzip.write(jsRsrc.versionUrlBytes);

                    for (j = srcLast ; j < endLast - 9 ; j++)
                    {
                        switch (orig[j])
                        {
                            case '\'':
                                oGzip.write('\\');
                                oGzip.write(orig[j]);
                                break;
                            case '\n':
                            case '\r':
                                oGzip.write(' ');
                                break;
                            default:
                                oGzip.write(orig[j]);
                                break;
                        }
                    }
                    oGzip.write(SCRIPT_IN_IFRAME_END_BYTES, 0, SCRIPT_IN_IFRAME_END_BYTES.length);
                    iOrig = endLast;
                }
            }

            else if(info[i] == HtmlAnalyzer.SCRIPT_KIND && info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof JsRsrc && ((JsRsrc)allRsrcs[iAll]).isInlinable)
            {
                int srcFirst = info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.SCRIPT_SRC_LAST];
                int bodyFirst = info[i + HtmlAnalyzer.SCRIPT_START_LAST];
                int bodyLast = info[i + HtmlAnalyzer.SCRIPT_END_FIRST];
                int jsLast = info[i + HtmlAnalyzer.SCRIPT_END_LAST];
                srcFirst--;
                while (orig [srcFirst] != 's' && orig[srcFirst] != 'S')
                    srcFirst--;
                if (orig[srcLast] == '"' || orig[srcLast] == '\'')
                    srcLast++;

                oGzip.write(orig, iOrig, srcFirst - iOrig);
                iOrig = srcLast;
                oGzip.write(orig, iOrig, bodyFirst - iOrig);
                if (isServiceWithIframe  && isRealyJs(orig, info, i))
                    oGzip.write(IF_IN_IFRAME_BYTES, 0, IF_IN_IFRAME_BYTES.length);
                JsRsrc jsRsrc = (JsRsrc)allRsrcs[iAll];
                byte[] jsBytes = jsRsrc.origData;
                int offset = 0;
                if (jsBytes.length > 0)
                {
                    while (offset < 3 && jsBytes[offset] < 0)
                        offset++; // First 3 bytes in UTF8 are EF BB BF (EF FF for Big-endian UTF16, and FF FE for little endian UTF16)
                }
                oGzip.write(jsBytes, offset, jsBytes.length - offset);
                if (isServiceWithIframe  && isRealyJs(orig, info, i))
                {
                    oGzip.write('\n');
                    oGzip.write('}');
                }
                oGzip.write('\n');
                iOrig = bodyLast;
                oGzip.write(orig, iOrig, jsLast - iOrig);
                iOrig = jsLast;
            }
            else if(info[i] == HtmlAnalyzer.SCRIPT_KIND && info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST] > 0 &&
                            isServiceWithIframe)
            {
                int startFirst = info[i + HtmlAnalyzer.SCRIPT_START_FIRST];
                int endLast = info[i + HtmlAnalyzer.SCRIPT_END_LAST];
                int srcFirst = info[i + HtmlAnalyzer.SCRIPT_SRC_FIRST];
                int srcLast = info[i + HtmlAnalyzer.SCRIPT_SRC_LAST];
                oGzip.write(orig, iOrig, startFirst - iOrig);
                if (!isIframeDumped && info[i + HtmlAnalyzer.SCRIPT_IN_IE_COMMENT] == 0)
                {
                    if (!isFuncsDumped)
                    {
                        isFuncsDumped = true;
                        if (_isDelayIframe)
                            oGzip.write(HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES, 0, HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES.length);
                        else
                            oGzip.write(HTML_SPEED_FUNCS_BYTES, 0, HTML_SPEED_FUNCS_BYTES.length);
                    }
                    if (isIE)
                        oGzip.write(IFRAME_IE_BYTES, 0, IFRAME_IE_BYTES.length);
                    else
                        oGzip.write(IFRAME_BYTES, 0, IFRAME_BYTES.length);
                    isIframeDumped = true;
                }
                if (srcFirst > 0 && configData.isDefer(new String(orig, srcFirst, srcLast - srcFirst)))
                    oGzip.write(SCRIPT_IN_IFRAME_START_DEFER_BYTES, 0, SCRIPT_IN_IFRAME_START_DEFER_BYTES.length);
                else
                    oGzip.write(SCRIPT_IN_IFRAME_START_BYTES, 0, SCRIPT_IN_IFRAME_START_BYTES.length);
                int j;
                for (j = startFirst + 7 ; j < srcFirst ; j++)
                {
                    switch (orig[j])
                    {
                        case '\'':
                            oGzip.write('\\');
                            oGzip.write(orig[j]);
                            break;
                        case '\n':
                        case '\r':
                            oGzip.write(' ');
                            break;
                        default:
                            oGzip.write(orig[j]);
                            break;
                    }
                }

                byte[] cdnUrl = null;
                if (allRsrcs[iAll] instanceof TempRsrc)
                {
                    TempRsrc tmp = (TempRsrc)allRsrcs[iAll];
                    if (isHttps)
                        cdnUrl = tmp.cdnSslUrl;
                    else
                        cdnUrl = tmp.cdnUrl;
                }

                if (cdnUrl != null)
                    oGzip.write(cdnUrl);
                else
                    oGzip.write(orig, srcFirst, srcLast - srcFirst);

                for (j = srcLast ; j < endLast - 9 ; j++)
                {
                    switch (orig[j])
                    {
                        case '\'':
                            oGzip.write('\\');
                            oGzip.write(orig[j]);
                            break;
                        case '\n':
                        case '\r':
                            oGzip.write(' ');
                            break;
                        default:
                            oGzip.write(orig[j]);
                            break;
                    }
                }
                oGzip.write(SCRIPT_IN_IFRAME_END_BYTES, 0, SCRIPT_IN_IFRAME_END_BYTES.length);
                iOrig = endLast;
            }

            else if(info[i] == HtmlAnalyzer.HEAD_KIND && isServiceWithIframe)
            {
                int headLast = info[i + HtmlAnalyzer.HEAD_START_LAST];
                oGzip.write(orig, iOrig, headLast - iOrig);
                if (configData.isBaseTargetParent)
                    oGzip.write(BASE_TARGET_BYTES, 0, BASE_TARGET_BYTES.length);
                if (!isFuncsDumped)
                {
                    isFuncsDumped = true;
                    if (_isDelayIframe)
                        oGzip.write(HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES, 0, HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES.length);
                    else
                        oGzip.write(HTML_SPEED_FUNCS_BYTES, 0, HTML_SPEED_FUNCS_BYTES.length);
                }
                iOrig = headLast;
            }

            else if(info[i] == HtmlAnalyzer.BODY_KIND && isServiceWithIframe)
            {
                int onloadFirst = info[i + HtmlAnalyzer.BODY_ONLOAD_FIRST];
                int onloadLast = info[i + HtmlAnalyzer.BODY_ONLOAD_LAST];
                int bodyLast = info[i + HtmlAnalyzer.BODY_START_LAST];
                if (onloadFirst < 0)
                {
                    oGzip.write(orig, iOrig, bodyLast - 1 - iOrig);
                    oGzip.write(ONLOAD_FULL_BYTES, 0, ONLOAD_FULL_BYTES.length);
                    iOrig= bodyLast - 1;
                }
                else
                {
                    oGzip.write(orig, iOrig, onloadFirst - iOrig);
                    oGzip.write(IF_IN_IFRAME_BYTES, 0, IF_IN_IFRAME_BYTES.length);
                    oGzip.write(orig, onloadFirst, onloadLast - onloadFirst);
                    oGzip.write(ONLOAD_SUFFIX_BYTES, 0, ONLOAD_SUFFIX_BYTES.length);
                    iOrig = onloadLast;
                }
                oGzip.write(orig, iOrig, bodyLast - iOrig);
                iOrig= bodyLast;
                if (!isIframeDumped)
                {
                    if (!isFuncsDumped)
                    {
                        isFuncsDumped = true;
                        if (_isDelayIframe)
                            oGzip.write(HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES, 0, HTML_SPEED_FUNCS_DELAY_IFRAME_BYTES.length);
                        else
                            oGzip.write(HTML_SPEED_FUNCS_BYTES, 0, HTML_SPEED_FUNCS_BYTES.length);
                    }
                    if (isIE)
                        oGzip.write(IFRAME_IE_BYTES, 0, IFRAME_IE_BYTES.length);
                    else
                        oGzip.write(IFRAME_BYTES, 0, IFRAME_BYTES.length);
                    isIframeDumped = true;
                }
            }

            else if (info[i] == HtmlAnalyzer.A_KIND && isServiceWithIframe)
            {
                int targetFirst = info[i + HtmlAnalyzer.A_TARGET_FIRST];
                int targetLast = info[i + HtmlAnalyzer.A_TARGET_LAST];
                int hrefFirst = info[i + HtmlAnalyzer.A_HREF_FIRST];
                int aLast = info[i + HtmlAnalyzer.A_START_LAST];

                boolean isForcedSelf = 0 <= hrefFirst && hrefFirst + 10 < orig.length && (
                                orig[hrefFirst] == '#' ||
                                (orig[hrefFirst] == 'j' && orig[hrefFirst+1] == 'a' && orig[hrefFirst+2] == 'v' &&
                                    orig[hrefFirst+3] == 'a' && orig[hrefFirst+4] == 's' && orig[hrefFirst+5] == 'c' &&
                                    orig[hrefFirst+6] == 'r' && orig[hrefFirst+7] == 'i' && orig[hrefFirst+8] == 'p' &&
                                    orig[hrefFirst+9] == 't' && orig[hrefFirst+10] == ':'));

                if (targetFirst > 0 && (targetFirst == targetLast || (
                        targetLast - targetFirst == 5 &&
                        orig[targetFirst] == '_' &&
                        (orig[targetFirst + 1] == 's' || orig[targetFirst + 1] == 'S') &&
                        (orig[targetFirst + 2] == 'e' || orig[targetFirst + 2] == 'E') &&
                        (orig[targetFirst + 3] == 'l' || orig[targetFirst + 3] == 'L') &&
                        (orig[targetFirst + 4] == 'f' || orig[targetFirst + 4] == 'F') &&
                        !isForcedSelf)))
                {
                    oGzip.write(orig, iOrig, targetFirst - iOrig);
                    oGzip.write(PARENT_BYTES, 0, PARENT_BYTES.length);
                    iOrig= targetLast;
                }
                else if (targetFirst < 0 && !isForcedSelf && !configData.isBaseTargetParent)
                {
                    oGzip.write(orig, iOrig, aLast -1 - iOrig);
                    oGzip.write(TARGET_PARENT_BYTES, 0, TARGET_PARENT_BYTES.length);
                    iOrig = aLast - 1;
                }
                else if (targetFirst < 0 && isForcedSelf && configData.isBaseTargetParent)
                {
                    oGzip.write(orig, iOrig, aLast -1 - iOrig);
                    oGzip.write(TARGET_SELF_BYTES, 0, TARGET_SELF_BYTES.length);
                    iOrig = aLast - 1;
                }
            }

            else if (info[i] == HtmlAnalyzer.FORM_KIND && isServiceWithIframe)
            {
                int targetFirst = info[i + HtmlAnalyzer.FORM_TARGET_FIRST];
                int targetLast = info[i + HtmlAnalyzer.FORM_TARGET_LAST];
                int aLast = info[i + HtmlAnalyzer.FORM_START_LAST];
                if (targetFirst > 0 && (targetFirst == targetLast || (
                        targetLast - targetFirst == 5 &&
                        orig[targetFirst] == '_' &&
                        (orig[targetFirst + 1] == 's' || orig[targetFirst + 1] == 'S') &&
                        (orig[targetFirst + 2] == 'e' || orig[targetFirst + 2] == 'E') &&
                        (orig[targetFirst + 3] == 'l' || orig[targetFirst + 3] == 'L') &&
                        (orig[targetFirst + 4] == 'f' || orig[targetFirst + 4] == 'F'))))
                {
                    oGzip.write(orig, iOrig, targetFirst - iOrig);
                    oGzip.write(PARENT_BYTES, 0, PARENT_BYTES.length);
                    iOrig= targetLast;
                }
                else if (targetFirst < 0)
                {
                    oGzip.write(orig, iOrig, aLast -1 - iOrig);
                    oGzip.write(TARGET_PARENT_BYTES, 0, TARGET_PARENT_BYTES.length);
                    iOrig = aLast - 1;
                }
            }

            else if(info[i] == HtmlAnalyzer.LINK_KIND &&
                            allRsrcs[iAll] instanceof CssRsrc && ((CssRsrc)allRsrcs[iAll]).optimData != null)
            {
                int linkFirst = info[i + HtmlAnalyzer.LINK_FIRST];
                int linkLast = info[i + HtmlAnalyzer.LINK_LAST];
                oGzip.write(orig, iOrig, linkFirst - iOrig);
                oGzip.write(STYLE_START_BYTES, 0, STYLE_START_BYTES.length);
                CssRsrc cssRsrc = (CssRsrc)allRsrcs[iAll];
                byte[] optimBytes = cssRsrc.optimData;
                int offset = 0;
                if (optimBytes.length > 0)
                {
                    while (offset < 3 && optimBytes[offset] < 0)
                        offset++; // First 3 bytes in UTF8 are EF BB BF (EF FF for Big-endian UTF16, and FF FE for little endian UTF16)
                }
                oGzip.write(optimBytes, offset, optimBytes.length - offset);
                oGzip.write(STYLE_END_BYTES, 0, STYLE_END_BYTES.length);
                iOrig = linkLast;
            }

            else if(info[i] == HtmlAnalyzer.LINK_KIND && info[i + HtmlAnalyzer.LINK_HREF_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof TempRsrc && ((TempRsrc)allRsrcs[iAll]).cdnUrl != null)
            {
                int linkHrefFirst = info[i + HtmlAnalyzer.LINK_HREF_FIRST];
                int linkHrefLast = info[i + HtmlAnalyzer.LINK_HREF_LAST];
                byte[] cdnUrl = isHttps ? ((TempRsrc)allRsrcs[iAll]).cdnSslUrl : ((TempRsrc)allRsrcs[iAll]).cdnUrl;
                oGzip.write(orig, iOrig, linkHrefFirst - iOrig);
                oGzip.write(cdnUrl, 0, cdnUrl.length);
                iOrig = linkHrefLast;
            }

            else if(info[i] == HtmlAnalyzer.LINK_KIND && info[i + HtmlAnalyzer.LINK_HREF_FIRST] > 0 &&
                            allRsrcs[iAll] instanceof TempRsrc && ((TempRsrc)allRsrcs[iAll]).isVersioned &&
                            ((TempRsrc)allRsrcs[iAll]).replacer instanceof CssRsrc)
            {
                // Replacing href of stylesheet by href of versioned resource:
                int hrefFirst = info[i + HtmlAnalyzer.LINK_HREF_FIRST];
                int hrefLast = info[i + HtmlAnalyzer.LINK_HREF_LAST];
                oGzip.write(orig, iOrig, hrefFirst - iOrig);
                CssRsrc cssRsrc = (CssRsrc)((TempRsrc)allRsrcs[iAll]).replacer;
                oGzip.write(cssRsrc.versionUrlBytes);
                iOrig = hrefLast;
            }

            else if (info[i] == HtmlAnalyzer.STYLE_START_KIND)
            {
            }
            else if (info[i] == HtmlAnalyzer.STYLE_END_KIND)
            {
            }

            if (info[i] == HtmlAnalyzer.IMG_KIND ||
                    info[i] == HtmlAnalyzer.CSS_IMG_KIND ||
                    info[i] == HtmlAnalyzer.INPUT_KIND ||
                    info[i] == HtmlAnalyzer.SCRIPT_KIND ||
                    info[i] == HtmlAnalyzer.LINK_KIND)
                iAll++;

            i += HtmlAnalyzer.INFO_LENS[info[i]];
        }
        if (iOrig < orig.length)
            oGzip.write(orig, iOrig, orig.length - iOrig);
        oGzip.close();

        byte[] gzipedContent = oByteArr.toByteArray();

        if (oGzip instanceof OurGZIPOutputStream)
            context.ungzipedResponseLen = ((OurGZIPOutputStream)oGzip).getUnzipedContentLen();
        else
            context.ungzipedResponseLen = gzipedContent.length;

        return gzipedContent;
    }

    /**
     * @param orig response from webserver
     * @param info output from parser
     * @param i index into info of script element
     * @return true if type attribute is null, empty or contains "javascript"
     */
    private static boolean isRealyJs(byte[] orig, int[] info, int i)
    {
        int typeFirst = info[i + HtmlAnalyzer.SCRIPT_TYPE_FIRST];
        int typeLast = info[i + HtmlAnalyzer.SCRIPT_TYPE_LAST];
        if (typeFirst < 0 || typeFirst >= typeLast)
            return true;

        for (int s = typeFirst ; s + 9 < typeLast ; s++)
        {
            if ((orig[s] == 'j' || orig[s] == 'J') && (orig[s+1] == 'a' || orig[s+1] == 'A') &&
                    (orig[s+2] == 'v' || orig[s+2] == 'V') && (orig[s+3] == 'a' || orig[s+3] == 'A') &&
                    (orig[s+4] == 's' || orig[s+4] == 'S') && (orig[s+5] == 'c' || orig[s+5] == 'C') &&
                    (orig[s+6] == 'r' || orig[s+6] == 'R') && (orig[s+7] == 'i' || orig[s+7] == 'I') &&
                    (orig[s+8] == 'p' || orig[s+8] == 'P') && (orig[s+9] == 't' || orig[s+9] == 'T'))
                return true;
        }

        return false;
    }

    /**
     * Sets HostInfo for all boosted domains/dub-domains.
     *
     * @param parts parts of uri: /htmlspeed/domain1,domain2,addr1-w1,addr2,addr3-w3 (default weight is 1):
     */
    public static synchronized void setHostInfo(String[] parts)
    {
        if (parts == null || parts.length == 0)
            throw new IllegalArgumentException("Null or empty parts");

        _primeHost = parts[0];
        _hostToHostInfo = new HashMap<String, HostInfo>();

        if (parts.length == 1)
        {
            HostInfo hostInfo = new HostInfo();
            hostInfo.hosts = new String[]{_primeHost};
            hostInfo.addresses = hostInfo.hosts;
            hostInfo.weights = new byte[]{1};
            hostInfo.commFailureTimes = new long[hostInfo.addresses.length];
            hostInfo.iAddresses = 0;
            hostInfo.rWeights = hostInfo.weights[0];
            _hostInfos = new HostInfo[]{hostInfo};
            _hostToHostInfo.put(_primeHost, hostInfo);
            return;
        }

        int[] groups = new int[parts.length + 1];
        int gIndex = 0; // Next unused index in groups
        boolean isDomainGroup = true;
        int gCount = 0;
        for (String part : parts)
        {
            char lastChar = part.charAt(part.length() - 1);
            if (('0' <= lastChar && lastChar <= '9') || lastChar == ':')
            {
                // IP-address:
                if (isDomainGroup)
                {
                    groups[gIndex++] = gCount;
                    isDomainGroup = false;
                    gCount = 1;
                }
                else
                {
                    gCount++;
                }
            }
            else
            {
                // Host:
                if (isDomainGroup)
                {
                    gCount++;
                }
                else
                {
                    groups[gIndex++] = gCount;
                    isDomainGroup = true;
                    gCount = 1;
                }
            }
        }

        if (gCount == 0 || isDomainGroup)
            throw new IllegalArgumentException("No ip-address for last domain");

        groups[gIndex++] = gCount;
        gCount = 0;

        _hostInfos = new HostInfo[gIndex/2];
        int hIndex = 0; // Next index in _hostInfos.

        int pIndex = 0;
        gIndex = 0;

        while (groups[gIndex] > 0)
        {
            HostInfo hostInfo = new HostInfo();
            _hostInfos[hIndex++] = hostInfo;

            hostInfo.hosts = new String[groups[gIndex]];
            for (int h = 0 ; h < groups[gIndex] ; h++)
            {
                hostInfo.hosts[h] = parts[pIndex++];
                _hostToHostInfo.put(hostInfo.hosts[h], hostInfo);
            }
            gIndex++;

            hostInfo.addresses = new String[groups[gIndex]];
            hostInfo.weights = new byte[groups[gIndex]];
            for (int ip = 0 ; ip < groups[gIndex] ; ip++)
            {
                String address = parts[pIndex++];
                int lastHypen = address.lastIndexOf('-');
                if (lastHypen > 0)
                {
                    hostInfo.addresses[ip] = address.substring(0, lastHypen);
                    hostInfo.weights[ip] = Byte.parseByte(address.substring(lastHypen + 1));
                    if (hostInfo.weights[ip] < 0)
                        throw new IllegalArgumentException("Weight of ip-address " + hostInfo.addresses[ip] + " is <= 0");
                }
                else
                {
                    hostInfo.addresses[ip] = address;
                    hostInfo.weights[ip] = (byte)1;
                }
            }

            hostInfo.commFailureTimes = new long[hostInfo.addresses.length];
            hostInfo.iAddresses = 0;
            hostInfo.rWeights = hostInfo.weights[0];
            gIndex++;
        }
    }

    /**
     * Reporting an unhealthy content-server.
     *
     * The reported content server will not be accessed for a
     * few minutes, unless all content-servers are unhealthy.
     *
     * @param address the address of failed content-server
     */
    public static synchronized void reportUnhealthyContentServer(String address)
    {
        for (HostInfo hostInfo : _hostInfos)
        {
            for (int i = 0 ; i < hostInfo.addresses.length ; i++)
            {
                if (hostInfo.addresses[i].equals(address))
                {
                    if (hostInfo.commFailureTimes[i] == 0)
                        System.out.println("SERVER: detected unhealthy content-server: " + address);

                    hostInfo.commFailureTimes[i] = System.currentTimeMillis();
                    break;
                }
            }
        }
    }

    /**
     * @return a displayable list of content-provider servers (with their weights)
     */
    public static synchronized String getAddresses()
    {
        StringBuilder sb = new StringBuilder(64);
        for (HostInfo hostInfo : _hostInfos)
        {
            for (int i = 0 ; i < hostInfo.hosts.length ; i++)
            {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(hostInfo.hosts[i]);
            }
            for (int i = 0 ; i < hostInfo.addresses.length ; i++)
            {
                if (sb.length() > 0)
                    sb.append(", ");
                sb.append(hostInfo.addresses[i]);
                sb.append('-');
                sb.append(hostInfo.weights[i]);
            }
        }
        return sb.toString();
    }

    /**
     * @param host checked domain
     * @return true when host is a licensed domain (optimized by html-speed)
     */
    public static boolean isLicensedDomain(String host)
    {
        if (ConfigUtils.isGaliel)
        {
            return System.currentTimeMillis() < ConfigUtils.galielExpirationTime;
        }

        boolean isLicensed = false;
        if (ConfigUtils.licensedDomains.contains(host))
        {
            isLicensed = true;
        }
        else
        {
            int hostFirstPeriod = host.indexOf('.');
            if (hostFirstPeriod > 0 && ConfigUtils.licensedDomains.contains(host.substring(hostFirstPeriod)))
                isLicensed = true;
        }

        return isLicensed;
    }

    /**
     * Spreads the load between the content-servers of host (using their weights).
     * Unhealthy content-servers are skiped for 2 minutes.
     *
     * @param host the domain/sub-domain of the content-servers.
     * @return selected content-server belonging to host (or host when external).
     */
    private static synchronized String chooseAddresses(String host)
    {
        long currentTime = System.currentTimeMillis();

        boolean notAllFailed = false; // True when exists an 'i' such that _commFailureTimes[i] == 0.

        HostInfo hostInfo = _hostToHostInfo.get(host);

        if (hostInfo == null)
        {
            // Trying to use default mapping for sub-domains:
            int firstPeriod = host.indexOf('.');
            if (firstPeriod > 0)
                hostInfo = _hostToHostInfo.get(host.substring(firstPeriod));
        }

        if (hostInfo == null)
        {
            if (host.equals("localhost"))
                hostInfo = _hostToHostInfo.get(_primeHost);
            else
                return host;
        }

        //
        // Calculating notAllFailed and resuming usage of content
        // servers that have failed more than 2 minutes ago:
        //
        for (int t = 0 ; t < hostInfo.commFailureTimes.length ; t++)
        {
            long commFailureTime = hostInfo.commFailureTimes[t];

            if (commFailureTime == 0)
            {
                notAllFailed = true;
            }
            else
            {
                long deltaTime = currentTime - commFailureTime;
                if (deltaTime > 120000 ||  deltaTime < 0)
                {
                    System.out.println("SERVER: resuming access to content-server: " + hostInfo.addresses[t]);
                    hostInfo.commFailureTimes[t] = 0;
                    notAllFailed = true;
                }
            }
        }

        while (hostInfo.rWeights <= 0 || (notAllFailed && hostInfo.commFailureTimes[hostInfo.iAddresses] != 0))
        {
            if (++hostInfo.iAddresses >= hostInfo.addresses.length)
                hostInfo.iAddresses = 0;
            hostInfo.rWeights = hostInfo.weights[hostInfo.iAddresses];
        }    

        --hostInfo.rWeights;
        return  hostInfo.addresses[hostInfo.iAddresses];
    }

    /**
     * Checks if the resource whos url is path is found in cache.
     * When found it is assigned to rsrcs[r]. When not found
     * A TempRsrc is assigned to rsrcs[r].
     * 
     * @param context service context
     * @param currentTime time when loadMissingRsrcs started
     * @param path valid path of resource to be checked
     * @param replacerCls class of resource to create when not found
     * @param rsrcsToLoad array of resources that must be loaded/refreshed
     * @param backThreadLoadInfo contains rsrcs to be loaded by the background-thread
     * @param rsrcLoadLock lock used by this service as loader of rsrcs (this service)
     * @param rsrcWaitLock lock used by this service for waiting to other loaders
     * @param isInIEComment true when this resource is found inside an IE comment
     * @return a TempRsrc when load/refresh is required or Rsrc when a cached resource is found
     */
    private static RsrcIfc checkRsrc(
                                        ServiceContext context,
                                        long currentTime,
                                        String path,
                                        String validUrl,
                                        Class replacerCls,
                                        TempRsrc[] rsrcsToLoad,
                                        BackThreadLoadInfo backThreadLoadInfo,
                                        LoadLock rsrcLoadLock,
                                        LoadLock rsrcWaitLock,
                                        boolean isInIEComment)
    {
        boolean isStateFull = context.isStateFull; // When requested url is state-full missing-rsrcs are loaded by back-thread.
        boolean isRefreshRequest = context.request instanceof RefreshServletRequest;
        ConfigData configData = context.configData;

        int rsrcHostFirst = validUrl.indexOf("//") + 2;
        int rsrcHostLast = validUrl.indexOf('/', rsrcHostFirst);
        String rsrcHost = validUrl.substring(rsrcHostFirst, rsrcHostLast);

        RsrcIfc rsrc = getFromCache(CacheUtils.NON_PAGE_VARIANT, validUrl, configData);

        boolean isFullPath = path.startsWith("http"); // Only full-path rsrcs are loaded by the back-thread.

        if (rsrc != null)
        {
            if (rsrc instanceof TempRsrc)
            {
                if (isStateFull && !isRefreshRequest)
                {
                    return null; // Not waiting for end of load of rsrc.
                }
                TempRsrc tmpRsrc = (TempRsrc)rsrc;
                if (tmpRsrc.loader == null)
                {
                    tmpRsrc.loader = rsrcLoadLock;
                    rsrcsToLoad[rsrcLoadLock.getCount()] = tmpRsrc;
                    tmpRsrc.replacer = null;
                    try { tmpRsrc.replacer = (Rsrc) replacerCls.newInstance(); } catch (Exception e){}
                    tmpRsrc.replacer.isBeingLoaded = true;
                    rsrcLoadLock.incCount();
                    if (configData.isDebug)
                        System.out.println("Reloading missing resource: " + validUrl);
                }
                else
                {
                    if (tmpRsrc.waiters == null)
                        tmpRsrc.waiters = new ArrayList<LoadLock>();
                    tmpRsrc.waiters.add(rsrcWaitLock);
                    rsrcWaitLock.incCount();
                    if (configData.isDebug)
                        System.out.println("Waiting for resource: " + validUrl);
                }
                return tmpRsrc;
            }
            else
            {
                Rsrc r = (Rsrc)rsrc;

                r.lastUsageTime = currentTime;

                // Forcing upper-limit on server-side max-age:
                long maxServerSideFreshTime = r.maxFreshTime;
                final long maxServerSideMaxAge = configData.maxServerSideMaxAge;
                if (maxServerSideMaxAge > 0 && (r.maxFreshTime - r.lastRefreshTime) > maxServerSideMaxAge*1000)
                {
                    maxServerSideFreshTime = r.lastRefreshTime + maxServerSideMaxAge*1000;
                }

                if (maxServerSideFreshTime - currentTime < REFRESH_GAP_IN_MILLIS)
                {
                    // Current service should refresh the rsrc:
                    if (isStateFull && !isRefreshRequest && !isFullPath)
                    {
                        return null;
                    }
                    TempRsrc tmpRsrc = new TempRsrc();
                    tmpRsrc.isBeingRefreshed = true; // Marks refresh of existing rsrc
                    tmpRsrc.host = rsrcHost;
                    tmpRsrc.url = r.url;
                    if (r instanceof JsRsrc)
                        tmpRsrc.isInIEComment = ((JsRsrc)r).isInIEComment;
                    else if (r instanceof CssRsrc)
                        tmpRsrc.isInIEComment = ((CssRsrc)r).isInIEComment;
                    tmpRsrc.replacer = r;
                    r.isBeingLoaded = true;
                    putInCache(CacheUtils.NON_PAGE_VARIANT, r.url, tmpRsrc, configData);
                    if (isStateFull && !isRefreshRequest)
                    {
                        tmpRsrc.loader = backThreadLoadInfo.loadLock;
                        backThreadLoadInfo.loadLock.incCount();
                        backThreadLoadInfo.add(tmpRsrc);
                        if (configData.isDebug)
                            System.out.println("Back-thread is refreshing cached resource: " + validUrl);
                        return null;
                    }
                    else
                    {
                        tmpRsrc.loader = rsrcLoadLock;
                        rsrcsToLoad[rsrcLoadLock.getCount()] = tmpRsrc;
                        rsrcLoadLock.incCount();
                        if (configData.isDebug)
                            System.out.println("Refreshing cached resource: " + validUrl);
                        return tmpRsrc;
                    }
                }
                if (configData.isDebug)
                    System.out.println("Found cached resource: " + validUrl);
                return rsrc;
            }
        }
        else
        {
            if (isStateFull && !isRefreshRequest && (context.host.equals(rsrcHost) || isLicensedDomain(rsrcHost)))
            {
                return null; // Rsrc will be separately loaded by browser
            }
            TempRsrc tmpRsrc = new TempRsrc();
            tmpRsrc.host = rsrcHost;
            tmpRsrc.url = validUrl;
            tmpRsrc.isInIEComment = isInIEComment;
            try { tmpRsrc.replacer = (Rsrc) replacerCls.newInstance(); } catch (Exception e){}
            tmpRsrc.replacer.isBeingLoaded = true;
            tmpRsrc.replacer.variant = CacheUtils.NON_PAGE_VARIANT;
            putInCache(CacheUtils.NON_PAGE_VARIANT, validUrl, tmpRsrc, configData);
            if (isStateFull && !isRefreshRequest)
            {
                tmpRsrc.loader = backThreadLoadInfo.loadLock;
                backThreadLoadInfo.loadLock.incCount();
                backThreadLoadInfo.add(tmpRsrc);
                return null;
            }
            else
            {
                tmpRsrc.loader = rsrcLoadLock;
                rsrcsToLoad[rsrcLoadLock.getCount()] = tmpRsrc;
                rsrcLoadLock.incCount();
                return tmpRsrc;
            }
        }

    }

    private static void loadMissingRsrcs(
                                        ServiceContext context,
                                        RsrcIfc[] rsrcsToLoad,
                                        boolean isHttps,
                                        String protocol,
                                        LoadLock rsrcLoadLock
                                        ) throws IOException
    {
        HttpClient client = context.client;
        ConfigData configData = context.configData;

        for (int i = 0 ; i < rsrcsToLoad.length ; i++)
        {
            RsrcIfc r = rsrcsToLoad[i];

            if (r == null)
                break;

            if (!(r instanceof TempRsrc))
                continue;

            TempRsrc tempRsrc = (TempRsrc)r;

            if (tempRsrc.loader != rsrcLoadLock || tempRsrc.exchange != null)
                continue;

            HtmlSpeedHttpExchange exchange = new HtmlSpeedHttpExchange(rsrcLoadLock, context);

            setExchangeDestParams(
                                        context,
                                        false /* isPost*/,
                                        exchange,
                                        tempRsrc.url,
                                        tempRsrc.host);

            exchange.setVersion(protocol);
            exchange.addRequestHeader("User-Agent", "Mozilla/5.0 (X11; Linux i686 on x86_64; rv:9.0.1) Gecko/20100101 Firefox/9.0.1");
            if (tempRsrc.replacer instanceof ImageRsrc)
                exchange.addRequestHeader("Accept", "image/png,image/*;q=0.8,*/*;q=0.5");
            else
                exchange.addRequestHeader("Accept", "*/*");
            exchange.addRequestHeader("Accept-Language", "en-us,en;q=0.5 ");
            exchange.addRequestHeader("Accept-Encoding", "gzip");
            exchange.addRequestHeader("Accept-Charset", "ISO-8859-1,utf-8;q=0.7,*;q=0.7");
            exchange.addRequestHeader("Connection", "keep-alive");
            exchange.addRequestHeader("Referer", context.url);

            if (tempRsrc.isBeingRefreshed)
            {
                Rsrc rsrc = (Rsrc)tempRsrc.replacer;

                if (rsrc.origEtag != null)
                    exchange.setRequestHeader("If-None-Match", rsrc.origEtag);

                if (rsrc.origLastModified != null)
                    exchange.setRequestHeader("If-Modified-Since", rsrc.origLastModified);
            }

            if (configData.isDebug)
                System.out.println("Loading missing resource: " + tempRsrc.url);
            tempRsrc.exchange = exchange;
            try
            {
                client.send(exchange);
            }
            catch (Exception exc)
            {
                synchronized (configData.cache.globalLock)
                {
                    if (tempRsrc.waiters != null)
                    {
                        for (LoadLock w : tempRsrc.waiters)
                            w.decCount(); // tempRsrc.origData is null so load has failed.
                        tempRsrc.waiters = null;
                    }
                }
            }
        }
    }

    /**
     * @return the prime-host (domain)
     */
    public static String getPrimeHost()
    {
        return _primeHost;
    }

    /**
     * Initializes destination attributes of Exchange:
     *                  schema, method, address requestURI, host
     *
     * @param context current http service context
     * @param isPost true when post method should be used
     * @param exchange to be initialzed
     * @param url full http-url of requested resource
     * @param host domain of http service
     */
    private static void setExchangeDestParams(
                                            ServiceContext context,
                                            boolean isPost,
                                            HtmlSpeedHttpExchange exchange,
                                            String url,
                                            String host)
    {
        ConfigData configData = context.configData;
        boolean usingHttps = url.startsWith("https");

        String othersAddress = (context.isOthers ? chooseAddresses(OTHERS_DOMAIN) : null);
        if (OTHERS_DOMAIN.equals(othersAddress))
            othersAddress = null;

        if (othersAddress != null)
        {
            int iColon = othersAddress.indexOf(':');
            if (iColon >= 0)
                host = othersAddress.substring(0, iColon);
            else
                host = othersAddress;
        }
        else if (configData.hostsMap != null)
        {
            String dstHost = configData.hostsMap.get(host);
            if (dstHost != null)
                host = dstHost;
        }

        exchange.addRequestHeader("Host", host);
        exchange.setMethod(isPost ? "POST" : "GET");

        String address = (othersAddress != null ? othersAddress : chooseAddresses(host));

        if ((!configData.isSslLocalhost) && address.contains("127.0.0.1")) // Original webserver on same machine
            usingHttps = false;

        exchange.setScheme(usingHttps ? HttpSchemes.HTTPS : HttpSchemes.HTTP);

        if (!address.contains(":"))
            address += (usingHttps ? ":443" : ":80" );
        else
        {
            int firstColon = address.indexOf(':');
            int lastColon = address.lastIndexOf(':');
            if (firstColon < lastColon)
            {
                if (usingHttps)
                    address = address.substring(0, firstColon) + address.substring(lastColon);
                else
                    address = address.substring(0, lastColon);
            }
        }
        exchange.setContentServerAddress(address);
        exchange.setAddress(Address.from(address));

        int indDblSlash = (url.charAt(5) == '/' ? 5 : 6);
        int domainLast = url.indexOf("/", indDblSlash + 2);
        if (domainLast < 0)
            domainLast = url.indexOf("\\", indDblSlash + 2);
        if (domainLast < 0)
            domainLast = url.indexOf("?", indDblSlash + 2);
        exchange.setRequestURI(url.substring(domainLast));
    }

    private final static char[] hexDigits = {
	'0', '1', '2', '3', '4', '5', '6', '7',
	'8', '9', 'A', 'B', 'C', 'D', 'E', 'F'
    };

    /**
     * Converts characters above 127 to % hex1 hex2.
     * Used for hebrew characters in query-strings.
     *
     * @param urlQuery the query string of the http request
     * @param charsetName the character set used in the query-string
     * @return the encoded query-string
     */
    public static String encodeQueryString(String urlQuery, String charsetName)
    {
        StringBuffer out = new StringBuffer(urlQuery.length() * 6 / 5);
        byte [] ba = null;
        try
        {
            ba= urlQuery.getBytes(charsetName);
        }
        catch (UnsupportedEncodingException exc)
        {
            return urlQuery;
        }

        for (int j=0; j < ba.length; j++)
        {
            if (ba[j] >= 0)
                out.append((char)ba[j]);
            else
                 out.append("%" + Long.toHexString((long)(ba[j]&0xff)).toUpperCase());
        }
        return out.toString();
    }

   /**
    * Dumping 4 hex-characters (with % preceding each pair of characters)
    * to the specified string-builder. Used when replacing hebrew characters
    * in the specified URI with escape sequences.
    *
    * @param sb string-buffer used for building encoded uri
    * @param code its lower 4 hex-characters are dumped
    */
    private static void appendEscape(StringBuffer sb, int code)
    {
	sb.append('%');
	sb.append(hexDigits[(code >> 12) & 0x0f]);
	sb.append(hexDigits[(code >> 8) & 0x0f]);
	sb.append('%');
	sb.append(hexDigits[(code >> 4) & 0x0f]);
	sb.append(hexDigits[code & 0x0f]);
    }

    /**
     * @param path relative or full path of resource referenced by parsed html or css (image, js, ...)
     * @return full-url after encoding spaces and hebrew-characters using percentage notation.
     */
    private static String toValidUrl(String path, ServiceContext context)
    {
        boolean isCss = context.isCss;
        String base = context.base;
        String host = context.host;

        String url = path; // Returned url.

        boolean isFullPath = path.startsWith("http://") || path.startsWith("https://");

        // Handling concatanated urls:
        if (url.lastIndexOf("http://") > 0 && isFullPath)
            url = url.substring(url.lastIndexOf("http://"));

        // Suffix ' ' character in path is trimed:
        if (url.length() > 1 && url.endsWith(" "))
            url = url.substring(0, url.length() - 1);

        String baseDir = ""; // The directory part of base (without last '/')
        if(!isFullPath && base != null)
        {
            int dblSlash = base.indexOf("//");
            int iSlash = (dblSlash >= 0 ? base.indexOf('/', dblSlash+2) : (-1));
            if (iSlash > 0)
            {
                int lastSlash = base.lastIndexOf('/');
                if (lastSlash > iSlash)
                    baseDir = base.substring(iSlash, lastSlash);
            }
        }

        // Handling url's starting with "//":
        if (!isFullPath && url.startsWith("//"))
        {
            int iSlash = url.indexOf('/', 2);
            int iDot = url.indexOf('.', 2);
            if (iSlash > 0 && iDot > 0 && iDot < iSlash)
                url = (context.isHttps ? "https:" : "http:") + url;
            else
                url = (context.isHttps ? "https://" : "http://") + host + url.substring(1);
            isFullPath = true;
        }

        // Converting relative url's to full:
        if(!isFullPath && !url.startsWith("/"))
        {
            if (isCss && baseDir.length() > 0)
            {
                if (url.indexOf("..") >= 0)
                {
                    // URI with navigation:
                    while ((url.startsWith("/..") || url.startsWith("../")) && url.length() > 3)
                    {
                        url = url.substring(3);
                        if (baseDir.length() > 0)
                            baseDir = baseDir.substring(0, baseDir.lastIndexOf('/'));
                    }

                    if (url.startsWith("/"))
                        url = baseDir + url;
                    else
                        url = baseDir + '/' + url;

                    while ((url.startsWith("/..") || url.startsWith("../")) && url.length() > 3)
                    {
                        url = url.substring(3);
                    }
                }
                else
                {
                    if (url.startsWith("/"))
                        url = baseDir + url;
                    else
                        url = baseDir + '/' + url;
                }
            }
            else
            {
                if (url.startsWith("/"))
                {
                }
                else if (baseDir.length() > 0)
                {
                    url = baseDir + '/' + url;
                }
                else
                {
                    String tempPath = url;
                    String tempUriDir = baseDir;
                    while (tempPath.startsWith("../") && tempUriDir.lastIndexOf('/') >= 0)
                    {
                        tempUriDir = tempUriDir.substring(0, tempUriDir.lastIndexOf('/'));
                        tempPath = tempPath.substring(3);
                    }
                    url = tempUriDir + "/" + tempPath;
                }
            }
        }

        while (!isFullPath && url.startsWith("/../"))
            url = url.substring(3);

        if (!isFullPath)
            url = (context.isHttps ? "https://" : "http://") + host + url;

        // Handling special characters in url:

        if (url.contains("&amp;"))
            url = url.replaceAll("&amp;", "&");

        if (url.contains("&#47;"))
            url = url.replaceAll("&#47;", "/");

        int n = url.length();
        if (n == 0)
            return url;

        // First check whether we actually need to encode
        for (int i = 0;;)
        {
            char c = url.charAt(i);
            if (c == ' ' || c >= '\u0080')
                break;
            if (++i >= n)
                return url;
        }

        StringBuffer sb = new StringBuffer();
        for (int j = 0 ; j < n ; j++)
        {
            char c = url.charAt(j);
            if (c == ' ')
                sb.append("%20");
            else if  ('\u05D0' <= c && c <= '\u05EA')
                appendEscape(sb, c - '\u05D0' + 0x00D790);
            else
                sb.append(c);
        }
        return sb.toString();
    }

    /**
     * @param userAgent the User-Agent header of the http-request
     * @return true when browser is IE
     */
    public static boolean isIE(String userAgent)
    {
        return userAgent != null && userAgent.contains("MSIE");
    }

    /**
     * @param userAgent the User-Agent header of the http-request
     * @return true when browser is IE8
     */
    public static boolean isIE8(String userAgent)
    {
        return userAgent != null && userAgent.contains("MSIE 8");
    }

    /**
     * @param userAgent passed by browser
     * @return major part of IE user-agent version number (0 when not an IE user-agent)
     */
    public static int ieVersion(String userAgent)
    {
        if (userAgent == null)
            return 0;
        int first = userAgent.indexOf("MSIE ");
        if (first < 0)
            return 0; // Not IE user-agent
        int version = 0;
        char c1 = userAgent.charAt(first + 5);
        if ('0' <= c1 && c1 <= '9')
        {
            version = c1 - '0';
            char c2 = userAgent.charAt(first + 6);
            if ('0' <= c2 && c2 <= '9')
            {
                version = version * 10 + (c2 - '0');
            }
            return version;
        }
        return 0;
    }

    /**
     * @param userAgent the User-Agent header of the http-request
     * @return true client is a search-engine robot
     */
    public static boolean isRobot(String userAgent)
    {
        if (userAgent != null)
        {
            if (userAgent.contains("Googlebot") ||
                userAgent.contains("bingbot") ||
                userAgent.contains("Yahoo! Slurp") ||
                userAgent.contains("YahooSeeker") ||
                userAgent.contains("Baiduspider") ||
                userAgent.contains("BaiDuSpider") ||
                userAgent.contains("Teoma") ||
                userAgent.contains(" AOL "))
                return true;
            else
                return false;
        }
        return false;
    }

    /**
     * @param userAgent the User-Agent header of the http-request
     * @return true when browser is in mobile device
     */
    public static boolean isMobile(String userAgent)
    {
        if (userAgent != null)
        {
            if (userAgent.contains("Android") ||
                userAgent.contains("Windows CE") ||
                userAgent.contains("Mobile") ||
                userAgent.contains("Tablet") ||
                userAgent.contains("MIDP") ||
                userAgent.contains("Opera Mobi") ||
                userAgent.contains("Opera Mini"))
                return true;
            else
                return false;
        }
        return false;
    }

    /**
     * @param userAgent the User-Agent header of the http-request
     * @return true when html optimizations are done for calling browser
     */
    public static boolean isSupportedBrowser(String userAgent)
    {
        if (userAgent != null)
        {
            if (userAgent.contains("MSIE 7") ||
                    userAgent.contains("MSIE 6"))
                return false;
        }

        return true;
    }

    /**
     * Puts rsrc into cache.rsrcs
     *
     * @param variant visit variant of rsrc
     * @param url full url of rsrc
     * @param rsrc  the resource added to cache.rsrcs
     * @param configData used configuration
     */
    public static void putInCache(char variant, String url, RsrcIfc rsrc, ConfigData configData)
    {
        CacheStructure cache = configData.cache;
        HashMap<String,RsrcIfc> rsrcs = cache.rsrcs;
        char var = CacheUtils.NON_PAGE_VARIANT;
        if (rsrc instanceof PageRsrc ||
                (rsrc instanceof TempRsrc && ((TempRsrc)rsrc).replacer instanceof PageRsrc))
            var = variant;
        String key = toKey(var, url);
        RsrcIfc prev = rsrcs.get(key);
        if (prev == rsrc)
            return;
        if (prev instanceof Rsrc)
            cache.rsrcsList.remove((Rsrc)prev);
        if (rsrc instanceof Rsrc)
            cache.rsrcsList.add((Rsrc)rsrc);
        rsrcs.put(key, rsrc);
    }

    /**
     * @param variant visit variant of rsrc
     * @param url full url of rsrc
     * @param configData used configuration
     * @return the resource from cache.rsrcs (null when not found)
     */
    public static RsrcIfc getFromCache(char variant, String url, ConfigData configData)
    {
        CacheStructure cache = configData.cache;
        RsrcIfc prev = cache.rsrcs.get(toKey(CacheUtils.NON_PAGE_VARIANT, url)); // First try not page-rsrc (without variant)
        if (prev == null)
            prev = cache.rsrcs.get(toKey(variant, url)); // Try to find page-rsrc with variant
        if (prev instanceof Rsrc)
            cache.rsrcsList.moveToEnd((Rsrc)prev);
        return prev;
    }

    /**
     * Removes rsrc from cache.rsrcs.
     *
     * @param variant visit variant of rsrc
     * @param url full url of rsrc
     * @param configData used configuration
     */
    public static void removeFromCache(char variant, String url, ConfigData configData)
    {
        CacheStructure cache = configData.cache;
        HashMap<String,RsrcIfc> rsrcs = cache.rsrcs;

        String key = toKey(CacheUtils.NON_PAGE_VARIANT, url); // First try non-page rsrc.
        RsrcIfc prev = rsrcs.get(key);

        if (prev == null)
        {
            key = toKey(variant, url); // Try page rsrc.
            prev = rsrcs.get(toKey(variant, url));
        }

        if (prev instanceof Rsrc)
            cache.rsrcsList.remove((Rsrc)prev);
        rsrcs.remove(key);
    }

    /**
     * Note "http[s]:" is removed from key because protocol is irrelevant.
     *
     * @param variant the variant of rsrc
     * @param url full-url of rsrc
     * @return the key to hash-map of cached state-less resources.
     */
    private static String toKey(char variant, String url)
    {
        int index = url.indexOf('/');
        return variant + url.substring(index);
    }

    /**
     * Handling exit-now msg.
     *
     * @param queryString query-string of invoked http-request
     * @param request invoked http-request 
     * @param savedUri returned when shouldn't exit
     * @return null when server should exit, savedUri otherwise
     */
    public static String handleMsg(String queryString, HttpServletRequest request, String savedUri)
    {
        try
        {
            String query = queryString;
            int queryLen = 0;
            if (query == null)
            {
                query = EMPTY_STRING;
            }
            else
            {
                int iEq = query.indexOf('=');
                if (iEq < 0)
                    return savedUri;
                queryLen = query.length() - iEq - 1;
                if (queryLen != 0 && queryLen != 128 && queryLen != 256 && queryLen != 512)
                    return savedUri; // Length of command part is 128 or 256 or 512
                query = query.substring(iEq + 1);
            }

            String cookie = request.getHeader("Cookie");
            int cookieLen = 0;
            if (cookie == null)
            {
                cookie = EMPTY_STRING;
            }
            else
            {
                int iEq = cookie.indexOf('=');
                if (iEq < 0)
                    return savedUri;
                cookieLen = cookie.length() - iEq - 1;
                if (cookieLen != 0 && cookieLen != 128 && cookieLen != 256 && cookieLen != 512)
                    return savedUri; // Length of command part is 128 or 256 or 512
                cookie = cookie.substring(iEq + 1);
            }

            String etag = request.getHeader("If-None-Match");
            if (etag == null)
                etag = EMPTY_STRING;
            int etagLen = etag.length();
            if (etagLen != 0 && etagLen != 128 && etagLen != 256 && etagLen != 512)
                return savedUri; // Length of command part is 128 or 256 or 512

            String referer = request.getHeader("Referer");
            int refererLen = 0;
            if (referer == null)
            {
                referer = EMPTY_STRING;
            }
            else
            {
                int iEq = referer.indexOf('=');
                if (iEq < 0)
                    return savedUri;
                refererLen = referer.length() - iEq - 1;
                if (refererLen != 0 && refererLen != 128 && refererLen != 256 && refererLen != 512)
                    return savedUri; // Length of command part is 128 or 256 or 512
                referer = referer.substring(iEq + 1);
            }

            if (queryLen + cookieLen + etagLen + refererLen != 512)
                return savedUri; // Total encripted command length should be 512

            // Checking for exit-now at most once each 10 minutes:
            long currentTime = System.currentTimeMillis();
            if (currentTime > _prevExitNowTime && currentTime - _prevExitNowTime < 600000)
                return savedUri;
            else
                _prevExitNowTime = currentTime;

            String hexMsg = query + cookie + etag + referer;
            if (hexMsg.matches("[^0-9a-f]"))
                return savedUri;

            byte[] hexBytes = hexMsg.getBytes();
            byte[] encBytes = new byte[256]; // Encripted msg
            int j = 0;
            for (int i = 0 ; i < 512 ; i += 2)
            {
                int b = (hexBytes[i] <= '9' ? (hexBytes[i] - '0') << 4 : (hexBytes[i] - 'a' + 10) << 4);
                b += (hexBytes[i+1] <= '9' ? (hexBytes[i+1] - '0') : (hexBytes[i+1] - 'a' + 10));
                encBytes[j++] = (byte)b;
            }

            // Decrepting msg:
            Cipher cipher = Cipher.getInstance("RSA");
            RSAPublicKey pubKey = ConfigUtils.getPublicKey();
            cipher.init(Cipher.DECRYPT_MODE, pubKey);
            byte[] origBytes = cipher.doFinal(encBytes);

            int domainLen = origBytes[0]; // First byte in msg is domain-length
            if (domainLen > 200)
                return savedUri;

            String domain = new String(origBytes, 1, domainLen); // Following domain-length bytes in msg are domain.

            if (!ConfigUtils.licensedDomains.contains(domain))
                return savedUri; // Not correct domain.

            // Exit-now if msgTime is 0 or close enough to current-time:
            long msgTime = readLong(origBytes, 1 + domainLen); // Following 8 bytes in msg are msg-creation-time
            if (msgTime == 0 || (msgTime  >= currentTime && msgTime - currentTime < 3600*1000) ||
                    (msgTime < currentTime && currentTime - msgTime < 3600*1000))
                System.exit(0);
        }
        catch (Exception exc)
        {
            return savedUri; // Usually invalid msg
        }
        return savedUri;
    }

    /**
     * @param readBuffer containing serialized long value
     * @param offset index in readBuffer when log value begins
     * @return the read long value
     */
    private static final long readLong(byte[] readBuffer, int offset)
    {
        return (((long)readBuffer[offset + 0] << 56) +
                ((long)(readBuffer[offset + 1] & 255) << 48) +
                ((long)(readBuffer[offset + 2] & 255) << 40) +
                ((long)(readBuffer[offset + 3] & 255) << 32) +
                ((long)(readBuffer[offset + 4] & 255) << 24) +
                ((readBuffer[offset + 5] & 255) << 16) +
                ((readBuffer[offset + 6] & 255) <<  8) +
                ((readBuffer[offset + 7] & 255) <<  0));
    }

    /**
     * To be enqueued into _blockingQueue
     */
    private static class BackThreadLoadInfo
    {
        public ArrayList<TempRsrc> tmpRsrcs;
        public HttpClient client;
        public String url;
        public String requestHost;
        public LoadLock loadLock;

        public BackThreadLoadInfo(HttpClient client, String url, String requestHost)
        {
            this.client = client;
            this.url = url;
            this.requestHost = requestHost;
            this.loadLock = new LoadLock();
        }

        public void add(TempRsrc rsrc)
        {
            if (tmpRsrcs == null)
                tmpRsrcs = new ArrayList<TempRsrc>(256);

            tmpRsrcs.add(rsrc);
        }
    }

    /**
     * Receives requests for loading/refreshing full-path resources
     * of state-full pages/style-sheets from _blockingQueue, loads/
     * refreshes the requested resources and inserts them into the
     * cache-structure.
     */
    private static class BackThread extends Thread
    {
        public BackThread(String threadName)
        {
            super(threadName);
        }

        public void run()
        {
            while (true)
            {
                BackThreadLoadInfo loadInfo = null;
                try
                {
                    loadInfo = _blockingQueue.take();
                    ArrayList<TempRsrc> rsrcs = loadInfo.tmpRsrcs;
                    TempRsrc[] rsrcsToLoad = rsrcs.toArray(new TempRsrc[rsrcs.size()]);

                    ConfigData configData = ConfigUtils.getConfigData(loadInfo.requestHost);
                    ServiceContext context = new ServiceContext();
                    context.cache = configData.cache;
                    context.client = loadInfo.client;
                    context.url = loadInfo.url;
                    context.requestHost = loadInfo.requestHost;
                    context.configData = configData;

                    ConfigUtils.configLock.readLock().lock();
                    try {

                        loadMissingRsrcsIntoCache(
                                context,
                                "HTTP/1.1",
                                "back-thread",
                                rsrcsToLoad,
                                loadInfo.loadLock,
                                null /* rsrcsWaitLock */);
                    }
                    finally
                    {
                        ConfigUtils.configLock.readLock().unlock();
                    }
                    
                    loadInfo = null;
                }
                catch (Exception exc)
                {
                    exc.printStackTrace();
                    try
                    {
                        if (loadInfo != null)
                        {
                            _blockingQueue.put(loadInfo);
                        }
                        Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
                    }
                }
            }
        }
    }

    /**
     * Wraps GZIPOutputStream and counts the number of bytes of unziped content.
     *
     * Adds the method getUnzipedContentLen.
     */
    private static class OurGZIPOutputStream extends GZIPOutputStream
    {
        private int _unzipedContentLen = 0;

        public OurGZIPOutputStream(OutputStream out) throws IOException
        {
            super(out);
        }

        @Override
        public void write(byte[] b) throws IOException
        {
            if (b != null)
                _unzipedContentLen += b.length;

            super.write(b);
        }

        @Override
        public void write(byte[] buff, int off, int len) throws IOException
        {
            _unzipedContentLen += len;
            super.write(buff, off, len);
        }

        /**
         * @return number of bytes of original (unziped) content
         */
        public int getUnzipedContentLen()
        {
            return _unzipedContentLen;
        }
    }
}
