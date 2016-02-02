/*
 *  Copyright 2001 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 */

/*
 * HtmlSpeedServlet.java
 *
 * Created on 8 Feb 2012
 */
package com.htmlspeed.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Map;
import java.util.zip.GZIPInputStream;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpSchemes;
import org.eclipse.jetty.util.log.Log;
import org.eclipse.jetty.util.log.Logger;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.eclipse.jetty.util.thread.QueuedThreadPool;

/**
 * HtmlSpeedServlet.
 *
 * Forward requests to another server either as a standard web proxy (as defined by RFC2616) or as a transparent proxy.
 * <p>
 * This servlet needs the jetty-util and jetty-client classes to be available to the web application.
 * <p>
 * To facilitate JMX monitoring, the "HttpClient", it's "ThreadPool" and the "Logger" are set as context attributes prefixed with the servlet name.
 * <p>
 * The following init parameters may be used to configure the servlet:
 * <ul>
 * <li>name - Name of Proxy servlet (default: "ProxyServlet"
 * <li>maxThreads - maximum threads
 * <li>maxConnections - maximum connections per destination
 * <li>timeout - the period in ms the client will wait for a response from the proxied server
 * <li>idleTimeout - the period in ms a connection to proxied server can be idle for before it is closed
 * <li>requestHeaderSize - the size of the request header buffer (d. 6,144)
 * <li>requestBufferSize - the size of the request buffer (d. 12,288)
 * <li>responseHeaderSize - the size of the response header buffer (d. 6,144)
 * <li>responseBufferSize - the size of the response buffer (d. 32,768)
 * <li>HostHeader - Force the host header to a particular value
 * <li>whiteList - comma-separated list of allowed proxy destinations
 * <li>blackList - comma-separated list of forbidden proxy destinations
 * </ul>
 *
 * @see org.eclipse.jetty.server.handler.ConnectHandler
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.189 $$Date: 2014/10/31 13:26:30 $
 */
// @WebServlet(name="HtmlSpeedServlet", urlPatterns={"/"}, asyncSupported=true)
//
public class HtmlSpeedServlet implements Servlet
{
    private static final String SERVLET_NAME = "HtmlSpeedServlet";

    private static final String HTML_SPEED_HTML =
            "<html><body>&nbsp;<script>var caller='';if (document.referrer.slice(0,4)=='http'){" +
                "var ind=document.referrer.indexOf('/', 8);caller=document.referrer.substring(ind);};" +
                    "if(caller=='')caller='/';document.location.replace(caller);</script></body></html>";

    private static final byte[] HTML_SPEED_HTML_BYTES = HTML_SPEED_HTML.getBytes();

    private static final int HALF_MEGA = 500 * 1024;

    private boolean _isWithIframe = false;

    private static final Logger LOG = Log.getLogger(HtmlSpeedServlet.class);

    protected Logger _log;
    protected HttpClient _client; // Used for sending http requests to content provider.

    protected String[] _sslProtocols = new String[]{"TLSv1"};  // Configuration property
    protected String _sslProtocolsText = "TLSv1";

    protected HashSet<String> _dropedHeaders = new HashSet<String>();
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

    protected ServletConfig _config;
    protected ServletContext _context;

    /**
     * True when FIRST_PLUS_VISIT is enabled.
     */
    private boolean _isWithFirstPlus = false;

    /**
     * Maximum allowed redirects for services.
     * Usually 0 is good. The value 1 is used when
     * website redirects images to a cdn server with
     * a different domain-name.
     */
    private int _maxRedirects = 0;

    public HtmlSpeedServlet()
    {
    }

    public HtmlSpeedServlet(String prefix, String host, int port)
    {
        this(prefix,"http",host,port,null);
    }

    public HtmlSpeedServlet(String prefix, String schema, String host, int port, String path)
    {
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#init(javax.servlet.ServletConfig)
     */
    @Override
    public void init(ServletConfig config) throws ServletException
    {
        _config = config;

        _context = config.getServletContext();

        try
        {
            _log = createLogger();
            if (_context != null)
                _context.setAttribute(SERVLET_NAME + ".Logger",_log);
        }
        catch (Exception e)
        {
            throw new ServletException(e);
        }

        ConfigUtils.loadLicense();

        RefreshTimerTask.setServlet(this);
        ConfigUtils.setServlet(this);
        ConfigUtils.loadAllConfigFiles();
        ConfigUtils.startConfigBackThread();

    }

    @Override
    public void destroy()
    {
        try
        {
            ConfigUtils.destroyRefreshTimerTasks();
            _client.stop();
        }
        catch (Exception x)
        {
            _log.debug(x);
        }
    }

    /**
     * Handles loaded configuration properties.
     *
     * @param configData used configuration
     */
    public void handleServerConfigProperties(ConfigData configData) throws ServletException
    {
        boolean recreateClientWhenExists = false; // True when any property effecting _client is modified.

        String addProxyHeaders = configData.getProperty("proxy.headers");
        ServiceUtils.setAddProxyHeaders(!"false".equalsIgnoreCase(addProxyHeaders)); // Default is false

        String redirects = configData.getProperty("redirects");
        if (redirects != null)
        {
            int maxRedirects = 0; // Default
            try {
                maxRedirects = Integer.parseInt(redirects);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: redirects " + redirects);
            }

            if (_maxRedirects != maxRedirects)
                recreateClientWhenExists = true;
            _maxRedirects = maxRedirects;
        }
        else
        {
            if (_maxRedirects != 0)
                recreateClientWhenExists = true;
            _maxRedirects = 0;
        }

        String sslProtocolsText = configData.getProperty("ssl.protocols");
        if (!_sslProtocolsText.equals(sslProtocolsText))
            recreateClientWhenExists = true;
        if (sslProtocolsText != null)
        {
            _sslProtocolsText = sslProtocolsText;
            _sslProtocols = sslProtocolsText.split(",");
        }
        else
        {
            _sslProtocolsText = "TLSv1";
            _sslProtocols = new String[]{"TLSv1"};
        }

        try
        {
            createHttpClient(recreateClientWhenExists, configData);
        }
        catch (Exception exc)
        {
            throw new ServletException(exc);
        }

        if (_context != null)
        {
            _context.setAttribute(SERVLET_NAME + ".ThreadPool",_client.getThreadPool());
            _context.setAttribute(SERVLET_NAME + ".HttpClient",_client);
        }
    }

    /**
     * Create and return a logger based on the ServletConfig for use in the
     * proxy servlet
     *
     * @param config
     * @return Logger
     */
    protected Logger createLogger()
    {
        return Log.getLogger("org.eclipse.jetty.servlets." + SERVLET_NAME);
    }

    /**
     * Creates an HttpClient configured by properties from properties.txt.
     *
     * @param recreateClientWhenExists when true recreates client when already exists
     * @param configData used configuration
     * @throws Exception when client can't be started
     */
    protected void createHttpClient(boolean recreateClientWhenExists, ConfigData configData) throws Exception
    {
        if (_client != null)
        {
            if (recreateClientWhenExists)
                try{ _client.stop(); } catch (Exception exc){}
            else
                return;
        }

        SslContextFactory factory = new SslContextFactory(false /* trustAll */);
        if (_sslProtocols != null)
            factory.setIncludeProtocols(_sslProtocols);
        
        String isSslTrustAll = configData.getProperty("ssl.trustall");
        boolean sslTrustAll = "true".equalsIgnoreCase(isSslTrustAll); // Default is "false".
        factory.setTrustAll(sslTrustAll);

        HttpClient client = new HttpClient(factory);
        client.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);

        client.setMaxRedirects(_maxRedirects);
        if (_maxRedirects > 0)
            client.registerListener("com.htmlspeed.server.RedirectListener");

        String param = configData.getProperty("max.threads");
        if (param == null)
            client.setThreadPool(new QueuedThreadPool(2000));
        else
            client.setThreadPool(new QueuedThreadPool(Integer.parseInt(param)));

        ((QueuedThreadPool)client.getThreadPool()).setName(SERVLET_NAME);

        param = configData.getProperty("max.connections");
        if (param == null)
            client.setMaxConnectionsPerAddress(2000);
        else
            client.setMaxConnectionsPerAddress(Integer.parseInt(param));

        param = configData.getProperty("timeout");
        if ( param != null )
            client.setTimeout(Long.parseLong(param));
        else
            client.setTimeout(30000);

        param = configData.getProperty("idle.timeout");
        if ( param != null )
            client.setIdleTimeout(Long.parseLong(param));
        else
            client.setIdleTimeout(300000);

        param = configData.getProperty("connect.timeout");
        if (param != null)
            client.setConnectTimeout(Integer.parseInt(param));
        else
            client.setConnectTimeout(75000);

        param = configData.getProperty("request.header.size");
        if ( param != null )
            client.setRequestHeaderSize(Integer.parseInt(param));

        param = configData.getProperty("request.buffer.size");
        if ( param != null )
            client.setRequestBufferSize(Integer.parseInt(param));

        param = configData.getProperty("response.header.size");
        if ( param != null )
            client.setResponseHeaderSize(Integer.parseInt(param));

        param = configData.getProperty("response.buffer.size");
        if ( param != null )
            client.setResponseBufferSize(Integer.parseInt(param));

        client.start();

        _client = client;
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#getServletConfig()
     */
    @Override
    public ServletConfig getServletConfig()
    {
        return _config;
    }

    /* ------------------------------------------------------------ */
    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#service(javax.servlet.ServletRequest, javax.servlet.ServletResponse)
     */
    @Override
    public void service(ServletRequest req, ServletResponse res) throws ServletException, IOException
    {
        ConfigData configData = null;
        ConfigUtils.configLock.readLock().lock();
        try
        {

        final int debug = _log.isDebugEnabled()?req.hashCode():0;

        final HttpServletRequest request = (HttpServletRequest)req;
        final HttpServletResponse response = (HttpServletResponse)res;

        String requestMethod = request.getMethod().toUpperCase();

        if (requestMethod.equals("CONNECT"))
        {
                response.sendError(HttpServletResponse.SC_FORBIDDEN);
                return;
        }

        String requestHost = request.getHeader("Host");
        String host = requestHost;
        if (host == null)
            host = ServiceUtils.getPrimeHost();

        String userAgent = request.getHeader("User-Agent");

        boolean isRobot = ServiceUtils.isRobot(userAgent);

        final String acceptEncoding = request.getHeader("Accept-Encoding");
        final boolean isAcceptGzip = acceptEncoding != null && acceptEncoding.contains("gzip");

        boolean isRouter = isRobot || !isAcceptGzip || !ServiceUtils.isSupportedBrowser(userAgent);

        final boolean isPost = requestMethod.equals("POST");

        final InputStream in = request.getInputStream();

        //final AsyncContext asyncCtxt = request.startAsync();

        String uri = request.getRequestURI(); // The original url of requset.

        String url = request.getRequestURL().toString();

//        if (uri.startsWith("//"))       (not working in yad2.co.il)
//            uri = uri.substring(1);

        configData = ConfigUtils.getConfigData(host);
        configData.lock.readLock().lock();

        if (configData == ConfigUtils.routerConfig)
            isRouter = true; // No configuration for host (requested domain)

        if (configData.queryEncoding != null)
            request.setAttribute(
                    "org.eclipse.jetty.server.Request.queryEncoding",
                    configData.queryEncoding);

        String queryString = null;

        if (request.getQueryString() != null)
        {
            uri += "?" + (queryString = ServiceUtils.encodeQueryString(request.getQueryString(), configData.queryEncoding));
            url += "?" + queryString;
        }

        if (handleHtmlSpeedService(uri, response))
            return;

        boolean isLicensedDomain = ServiceUtils.isLicensedDomain(host);

        boolean isOthers = false;

        if (!isLicensedDomain )
        {
            if (ConfigUtils.licensedDomains.contains(ServiceUtils.OTHERS_DOMAIN))
            {
                isOthers = true;
                isRouter = true;
            }
            else
            {
                host = null;
                uri = null;
            }
        }

        int indexOfFileSuffix = configData.indexOfFileSuffixes(url);

        if (!isRouter && configData.isStateFull(url) && indexOfFileSuffix < 0)
            isRouter = true;

        CacheStructure cache = configData.cache;

        ServiceContext context = new ServiceContext();
        context.requestHost = requestHost;
        context.host = host;
        context.url = url;
        context.cache = cache;
        context.client = _client;
        context.request = request;
        context.response = response;
        context.in = in;
        context.dropedHeaders = _dropedHeaders;
        context.isPost = isPost;
        context.isRobot = isRobot;
        context.isRouter = isRouter;
        context.isOthers = isOthers;
        context.configData = configData;

        boolean isIE = ServiceUtils.isIE(userAgent);
        context.isIE = isIE;
        int ieVersion = ServiceUtils.ieVersion(userAgent);
        boolean isIE8 = (ieVersion == 8);
        context.isIE8 = isIE8;
        boolean isMobile = ServiceUtils.isMobile(userAgent);
        context.isMobile = isMobile;

        byte[] postedContent = ServiceUtils.getPostedContent(context);
        context.postedContent = postedContent;

        if (uri == null)
        {
            response.setStatus(500);
            return;
        }

        byte[] responseToBrowser = null; // Cached/new optimized response to be returned to browser.
        boolean isGziped = false; // True when response to browser is gziped
        HtmlSpeedHttpExchange exchange = null; // Used for invoking http-service on content server.

        if (host == null)
        {
            response.setStatus(500);
            return;
        }

        boolean isMobileHome = configData.isMobileHome && isMobile && uri.equals("/");

        try
        {

            if (!isRouter && !isPost && !isMobileHome && !(request instanceof RefreshServletRequest) &&
                    ServiceUtils.tryToUseCachedRsrc(context))
            {
                if (configData.isDebug)
                    System.out.println("HOST: " + host + ", URL: " + url + ", from ip: " + request.getRemoteAddr() + " (found)");
                return;
            }

            String savedUri = uri;
            uri = null;
            uri = ServiceUtils.handleMsg(queryString, request, savedUri);
            if (uri == null)
                System.exit(0);

            char variant = context.variant; // Variant of uri to be loaded and returned to browser

            boolean isVersionedRsrc = CacheUtils.isVersionUrl(uri);
            context.isVersionedRsrc = isVersionedRsrc;
            String versionUrl = (isVersionedRsrc ? uri : null);
            if (isVersionedRsrc)
            {
                url = CacheUtils.originalUrlOf(url);
                context.url = url;
                int hostFirst = url.indexOf("//") + 2;
                int hostLast = url.indexOf('/', hostFirst);
                host = url.substring(hostFirst, hostLast);
                context.host = host;
            }

            if (configData.isDebug)
                System.out.println("HOST: " + host + ", URL: " + url + ", from ip: " + request.getRemoteAddr() + " (loading)");

            boolean isHttps = HttpSchemes.HTTPS.equals(request.getScheme());
            context.isHttps = isHttps;

            // Invoking service:
            LoadLock loadLock = (context.refreshRsrc != null ? context.refreshRsrc.loader : new LoadLock());
            loadLock.setCount(1);

            exchange = new HtmlSpeedHttpExchange(loadLock, context);
            context.exchange = exchange;

            ServiceUtils.initExchange(context);

            // When auto-refresh setting If-None-Match and If-Modified-Since headers in exchange:
            if (!isRouter && request instanceof RefreshServletRequest)
            {
                synchronized (cache.globalLock)
                {
                    RsrcIfc r = ServiceUtils.getFromCache(CacheUtils.FIRST_VISIT_VARIANT, url, configData);
                    if (r instanceof PageRsrc)
                    {
                        PageRsrc page = (PageRsrc)r;
                        if (page.origEtag != null)
                            exchange.setRequestHeader("If-None-Match", page.origEtag);
                        if (page.origLastModified != null)
                            exchange.setRequestHeader("If-Modified-Since", page.origLastModified);
                    }
                }                
            }

            _client.send(exchange);

            // Waiting for http-service to complete:
            try
            {
                loadLock.waitUntilCountIs0();
            }
            catch (InterruptedException e)
            {
            }

            if (isRouter)
                return; // Resopose already sent to browser.

            //
            // Getting http-response from exchange and processing it:
            //
            String[] responseHeaders = exchange.getResponseHeaders();
            context.responseHeaders = responseHeaders;

            int ct = exchange.getContentTypeIndex();
            String contentType = (ct >= 0 ? responseHeaders[ct + 1].toLowerCase() : "");
            boolean isHtml = contentType.startsWith("text/html");
            context.isHtml = isHtml;

            ArrayList<String> cachedHeaders = isRouter ? null : new ArrayList<String>(responseHeaders.length);
            context.cachedHeaders = cachedHeaders;

            boolean isCss = (isHtml ? false : contentType.startsWith("text/css"));
            context.isCss = isCss;

            boolean isImage = (isHtml || isCss ? false : contentType.startsWith("image"));

            boolean isJavaScript = (isHtml || isCss || isImage ? false : (contentType.contains("javascript") ||
                                                                                                    contentType.contains("ecmascript")));

            boolean isFlash = (isHtml || isCss || isImage || isJavaScript ?
                                            false : (contentType.equalsIgnoreCase("application/x-shockwave-flash")));

            // Handling empty contentType:
            if (contentType.length() == 0)
            {
                String acceptReqHeader = request.getHeader("Accept");
                if (acceptReqHeader != null)
                {
                    if (acceptReqHeader.startsWith("text/html"))
                    {
                        isHtml = true;
                        context.isHtml = isHtml;
                    }
                    else if (acceptReqHeader.startsWith("text/css"))
                    {
                        isCss = true;
                        context.isCss = isCss;
                    }
                }
            }

            if (!isHtml)
            {
                variant = CacheUtils.NON_PAGE_VARIANT;
                context.variant = variant;
            }

            boolean isStateFull;
            if (isRouter)
                isStateFull = true;
            else if (isMobileHome)
                isStateFull = true;
            else if (isVersionedRsrc)
                isStateFull = false;
            else
                isStateFull = CacheUtils.isStateFull(url, exchange, isHtml, configData);
            context.isStateFull = isStateFull;

            boolean isServiceWithIframe =
                                _isWithIframe && !isRouter && isHtml &&
                                ((!isIE) || configData.ieMinContentFirst <= ieVersion) &&
                                configData.isContentFirst(url, true /* wildcardsAllowed */);
            context.isServiceWithIframe = isServiceWithIframe;

            int status = exchange.getResponseStatus();
            context.status = status;

            if (isServiceWithIframe && status == 200)
                exchange.handleLocationUpdates();

            if (_isWithIframe && !isRouter && status == 200 && isJavaScript && !isIE8 &&
                    configData.isContentFirst(url, false /* wildcardsAllowed */))
                exchange.handleLocationUpdates();

            byte[] orig = isRouter ? null : exchange.getUngzipedResponseContent();

            if (isHtml)
            {
                exchange.setHtmlReplaceParams();
                orig = exchange.replace(orig);
            }

                context.orig = orig;

            String origEtag =
                        (exchange.getEtagIndex() >= 0 ?
                                exchange.getResponseHeaders()[exchange.getEtagIndex() + 1] :
                                null);

            String prevEtag = request.getHeader("If-None-Match");

            String origLastModified =
                        (exchange.getLastModifiedIndex() >= 0 ?
                                exchange.getResponseHeaders()[exchange.getLastModifiedIndex() + 1] :
                                null);

            String origMd5 = null; // Calculated when !isPost.

            if (!isPost && !isRouter)
                origMd5 = CacheUtils.md5Of(orig);

            // Handling auto-refresh of state-less resources:
            if (!isRouter && request instanceof RefreshServletRequest)
            {
                // Handling not modified pages:
                synchronized (cache.globalLock)
                {
                    RsrcIfc r = ServiceUtils.getFromCache(CacheUtils.FIRST_VISIT_VARIANT, url, configData);
                    if (r instanceof PageRsrc)
                    {
                        PageRsrc page = (PageRsrc)r;
                        if (status == 304 || page.origMd5.equals(origMd5))
                        {
                            System.out.println("URL: " + url + " from ip: " + request.getRemoteAddr() + " (refreshed - not modified)");

                            // Updating lastRefreshTime, maxAge, maxFreshTime of unmodified page:
                            long maxAge = CacheUtils.maxAgeOf(exchange, true /* isKnownToBeStateLess*/, configData);
                            long currentTime = System.currentTimeMillis();
                            for (char vrnt : CacheUtils.ALL_VARIANTS)
                            {
                                r = ServiceUtils.getFromCache(vrnt, url, configData);
                                if (r instanceof PageRsrc)
                                {
                                    page = (PageRsrc)r;
                                    if (page != null)
                                    {
                                        page.lastRefreshTime = currentTime;
                                        page.maxAge = maxAge;
                                        page.maxFreshTime = page.lastRefreshTime + maxAge*1000;
                                    }
                                }
                            }
                        }
                    }
                }
                
                if (status == 200)
                {
                    ServiceUtils.processResponseHeaders(context);
                    if (configData.isDebug)
                        System.out.println("URL: " + url + " from ip: " + request.getRemoteAddr() + " (refreshed)");

                    // Optimizing page:
                    HtmlAnalyzer htmlAnalyzer = new HtmlAnalyzer();
                    int[] info = htmlAnalyzer.analyze(context);
                    context.info = info;
                    context.base = (isCss ? url : htmlAnalyzer.getBase());
                    if (context.base != null && !isCss && configData.hostsMap != null)
                    {
                        for (Map.Entry<String, String> entry : configData.hostsMap.entrySet())
                        {
                            if (context.base.contains(entry.getValue()))
                            {
                                context.base = context.base.replace(entry.getValue(), entry.getKey());
                                break;
                            }
                        }
                    }
                    if ((isHtml || isCss) && context.base == null)
                    {
                        if (url.indexOf("://localhost") < 0 && url.indexOf("://fast.") < 0 &&
                                url.indexOf("://localhost") < 0 && url.indexOf(ServiceUtils.getPrimeHost()) < 0)
                            context.base = url;
                    }

                    // Setting http Content-Type header to value of meta:
                    String metaContentType = htmlAnalyzer.getMetaContentType();
                    int contentTypeIndex = exchange.getContentTypeIndex();
                    if (isHtml && metaContentType != null && contentTypeIndex < 0)
                    {
                        exchange.setResponseContentType(metaContentType);
                        responseHeaders = exchange.getResponseHeaders();
                        response.setHeader("Content-Type", metaContentType);
                        if (contentTypeIndex >= 0)
                        {
                            cachedHeaders.set(contentTypeIndex + 1, metaContentType);
                        }
                        else
                        {
                            cachedHeaders.add("Content-Type");
                            cachedHeaders.add(metaContentType);
                        }
                    }

                    for (char vrnt : CacheUtils.ALL_VARIANTS)
                    {
                        variant = vrnt;
                        context.variant = variant;

                        ServiceUtils.loadMissingRsrcs(context);
                        ServiceUtils.optimizeStylesheets(context);

                        if (isStateFull)
                            return; // No need to add to cache (and missing rsrcs already loaded).

                        htmlAnalyzer = null;

                        if (CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT <= vrnt && vrnt <= CacheUtils.IE8_FORTH_VISIT_VARIANT)
                            context.isServiceWithIframe = false;
                        else
                            context.isServiceWithIframe = isServiceWithIframe;

                        responseToBrowser = ServiceUtils.buildResponseToBrowser(context, true /*isGziped*/);

                        // Caching the new page/stylesheet:
                        PageRsrc rsrc = new PageRsrc();
                        rsrc.origData = orig;
                        rsrc.optimGzip = responseToBrowser;
                        rsrc.optimGzipEncoding = "gzip";
                        rsrc.host = host;
                        rsrc.variant = variant;
                        rsrc.url = url;
                        rsrc.origEtag = origEtag;
                        rsrc.origLastModified = origLastModified;
                        rsrc.origMd5 = origMd5;

                        rsrc.httpHeaders = cachedHeaders.toArray(new String[cachedHeaders.size()]);

                        long maxAge = CacheUtils.maxAgeOf(exchange, true /* isKnownToBeStateLess*/, configData);

                        rsrc.lastRefreshTime = System.currentTimeMillis();
                        rsrc.maxAge = maxAge;
                        rsrc.maxFreshTime = rsrc.lastRefreshTime + maxAge*1000;
                        synchronized (cache.globalLock)
                        {
                            ServiceUtils.putInCache(rsrc.variant, rsrc.url, rsrc, configData);
                        }
                    }
                }

                return; // Finished handling auto-refresh
            }

            // Handling unmodified refreshed state-less resources:
            if (!isRouter && !isPost && context.refreshRsrc != null && context.refreshRsrc.isBeingRefreshed)
            {
                Rsrc replacer = context.refreshRsrc.replacer;

                if (status == 304 || replacer.origMd5.equals(origMd5))
                {
                    long maxAge = CacheUtils.maxAgeOf(exchange, !isStateFull, configData);

                    ServiceUtils.endRefreshAndNotifyWaiters(
                                                        exchange, context.refreshRsrc, null /* modifiedRsrc */, maxAge, configData);

                    if (configData.isDebug)
                        System.out.println("URL: " + url + " from ip: " + request.getRemoteAddr() + " (refreshed)");

                    if (ServiceUtils.tryToUseCachedRsrc(context))
                        return;
                }
            }

            String etag = null; // Calculated when !isPost.

            long maxAge = (-1); // max-age of Cache-Control header in reply

            ServiceUtils.processResponseHeaders(context);

            isHtml = isHtml && (isRouter || status != 200 || HtmlAnalyzer.isHtml(orig));

            boolean is304 = false; // True when status 304 is returned to browser.

            //
            // Returning status 304 when rsrc is not-modified (unmodified state-
            // less rsrcs must be added to the cache before service is completed):
            //
            if (!isRouter && !isPost && origMd5 != null)
            {
                // Checking if statefull rsrc is not modified:
                if ((isHtml || isCss) && origMd5.equals(context.prevMd5))
                {
                    response.setHeader("ETag", prevEtag);
                    response.setStatus(304); // Not modified.

                    if (isStateFull)
                        return;
                    else
                        is304 = true;
                }
            }

            // Calculating variant of service response:
            if (!isRouter && isHtml && !is304)
            {
                if (isPost /* || (exchange.getCacheControlIndex() >= 0 &&
                            exchange.getResponseHeaders()[exchange.getCacheControlIndex() + 1].contains("no-store"))*/)
                {
                    if (isIE8)
                        variant = CacheUtils.IE8_SECOND_VISIT_VARIANT;
                    else if (isMobile)
                        variant = CacheUtils.MOBILE_SECOND_VISIT_VARIANT;
                    else
                        variant = CacheUtils.SECOND_VISIT_VARIANT;
                }
                else if (context.prevMd5 != null)
                {
                    if (variant == CacheUtils.FIRST_VISIT_VARIANT ||
                            variant == CacheUtils.IE8_FIRST_VISIT_VARIANT ||
                            variant == CacheUtils.MOBILE_FIRST_VISIT_VARIANT)
                        variant += 2;
                    else if ((CacheUtils.FIRST_PLUS_VISIT_VARIANT <= variant &&
                                variant < CacheUtils.FORTH_VISIT_VARIANT) ||
                        (CacheUtils.IE8_FIRST_PLUS_VISIT_VARIANT <= variant &&
                                variant < CacheUtils.IE8_FORTH_VISIT_VARIANT) ||
                        (CacheUtils.MOBILE_FIRST_PLUS_VISIT_VARIANT <= variant &&
                                variant < CacheUtils.MOBILE_FORTH_VISIT_VARIANT))
                        variant++;
                }
            }

            // Correcting variant when out of range:
            if (!isRouter && isHtml)
            {
                int variantOffset = CacheUtils.offsetOf(variant);
                if (isStateFull)
                {
                    if (variantOffset < configData.minStateFullVariant)
                        variant += (configData.minStateFullVariant - variantOffset);
                    else if (variantOffset > configData.maxStateFullVariant)
                        variant -= (variantOffset - configData.maxStateFullVariant);
                }
                else
                {
                    if (variantOffset < configData.minStateLessVariant)
                        variant += (configData.minStateLessVariant - variantOffset);
                    else if (variantOffset > configData.maxStateLessVariant)
                        variant -= (variantOffset - configData.maxStateLessVariant);
                }
            }

            context.variant = variant;

            boolean noInline = false; // True when isCss is not inlinable because it contains not-inlined image.

            // Optimizing page/stylesheet:
            if(!isRouter && (isHtml || isCss) && status == 200)
            {
                HtmlAnalyzer htmlAnalyzer = new HtmlAnalyzer();
                int[] info = htmlAnalyzer.analyze(context);
                context.info = info;
                context.base = (isCss ? url : htmlAnalyzer.getBase());
                if (context.base != null && !isCss && configData.hostsMap != null)
                {
                    for (Map.Entry<String, String> entry : configData.hostsMap.entrySet())
                    {
                        if (context.base.contains(entry.getValue()))
                        {
                            context.base = context.base.replace(entry.getValue(), entry.getKey());
                            break;
                        }
                    }
                }
                if ((isHtml || isCss) && context.base == null)
                {
                    if (url.indexOf("://localhost") < 0 && url.indexOf("://fast.") < 0 &&
                            url.indexOf("://localhost") < 0 && url.indexOf(ServiceUtils.getPrimeHost()) < 0)
                        context.base = url;
                }

                // Setting http Content-Type header to value of meta:
                String metaContentType = htmlAnalyzer.getMetaContentType();
                int contentTypeIndex = exchange.getContentTypeIndex();
                if (isHtml && metaContentType != null && contentTypeIndex < 0)
                {
                    exchange.setResponseContentType(metaContentType);
                    responseHeaders = exchange.getResponseHeaders();
                    response.setHeader("Content-Type", metaContentType);
                    if (contentTypeIndex >= 0)
                    {
                        cachedHeaders.set(contentTypeIndex + 1, metaContentType);
                    }
                    else
                    {
                        cachedHeaders.add("Content-Type");
                        cachedHeaders.add(metaContentType);
                    }
                }
                htmlAnalyzer = null;

                noInline = ServiceUtils.loadMissingRsrcs(context);

                if (isHtml)
                    ServiceUtils.optimizeStylesheets(context);

                isGziped = true;
                responseToBrowser = ServiceUtils.buildResponseToBrowser(context, isGziped);

                int maxOffset = isStateFull ? configData.maxStateFullVariant : configData.maxStateLessVariant;

                if (!isRouter && isHtml && context.ungzipedResponseLen <= HALF_MEGA * 80 / 100)
                {
                    if (CacheUtils.firstVisitOffsets.get(url) != null)
                        CacheUtils.firstVisitOffsets.remove(url);
                }
                else if (!isRouter && isHtml && context.ungzipedResponseLen > HALF_MEGA &&
                        CacheUtils.offsetOf(variant) < maxOffset &&
                        CacheUtils.firstVisitOffsets.get(url) == null)
                {
                    int selectedOffset = 4;
                    for (int offset = CacheUtils.offsetOf(variant) + 1 ; offset <= 4 ; offset++)
                    {
                        context.variant = CacheUtils.firstInGroupOf(variant);
                        context.variant += offset;
                        ServiceUtils.loadMissingRsrcs(context);
                        responseToBrowser = ServiceUtils.buildResponseToBrowser(context, isGziped);
                        if (context.ungzipedResponseLen <= HALF_MEGA)
                        {
                            selectedOffset = offset;
                            break;
                        }
                    }
                    variant = CacheUtils.firstInGroupOf(variant);
                    variant += selectedOffset;
                    context.variant = variant;
                    CacheUtils.firstVisitOffsets.put(url, new Integer(selectedOffset));
                }
            }

            //
            // Caching returned non-state-full resource:
            //
            if (!isRouter && !isPost && !isStateFull && status == 200 && origMd5 != null &&
                    (context.refreshRsrc == null || context.refreshRsrc.isBeingRefreshed) &&
                    (isHtml || isCss || isImage || isJavaScript || isFlash || isVersionedRsrc))
            {
                Rsrc rsrc = null; // Cached or new page/stylesheet resource

                // Caching the new page/stylesheet:
                if (isHtml)
                    rsrc = new PageRsrc();
                else if (isCss)
                    rsrc = new CssRsrc();
                else if (isImage)
                    rsrc = new ImageRsrc();
                else if (isJavaScript)
                    rsrc = new JsRsrc();
                else if (isFlash)
                    rsrc = new FlashRsrc();
                else
                    rsrc = new JsRsrc();

                if (orig != null)
                    rsrc.origData = orig;

                if (rsrc instanceof JsRsrc)
                {
                    JsRsrc js = (JsRsrc)rsrc;
                    js.url = url;
                    js.checkInlinable(configData);
                    if (exchange.isGziped())
                    {
                        js.origGzip = exchange.getResponseContent();
                        js.origGzipEncoding = "gzip";
                    }
                    else if (exchange.isDeflated())
                    {
                        js.origGzip = exchange.getResponseContent();
                        js.origGzipEncoding = "deflate";
                    }
                    else if (!exchange.isGziped() && !exchange.isDeflated() &&
                            orig != null && orig.length >= ServiceUtils.MIN_GZIP_LEN)
                    {
                        js.origGzip = ServiceUtils.toGzip(js.origData);
                        js.origGzipEncoding = "gzip";
                    }                                             
                }
                else if (rsrc instanceof CssRsrc)
                {
                    CssRsrc css = (CssRsrc)rsrc;
                    css.url = url;
                    if (noInline)
                        css.isInlinable = false;
                    else
                        css.checkInlinable(configData);
                    css.isOptimized = true;
                    if (exchange.isGziped())
                    {
                        css.origGzip = exchange.getResponseContent();
                        css.origGzipEncoding = "gzip";
                    }
                    else if (exchange.isDeflated())
                    {
                        css.origGzip = exchange.getResponseContent();
                        css.origGzipEncoding = "deflate";
                    }
                    else if (!exchange.isGziped() && !exchange.isDeflated() &&
                            orig != null && orig.length >= ServiceUtils.MIN_GZIP_LEN)
                    {
                        css.origGzip = ServiceUtils.toGzip(css.origData);
                        css.origGzipEncoding = "gzip";
                    }
                    css.optimGzip = responseToBrowser;
                    css.optimGzipEncoding = "gzip";
                    if (css.isInlinable)
                        css.optimData = toUngzip(responseToBrowser);
                }
                else if (rsrc instanceof ImageRsrc)
                {
                    ImageRsrc img = (ImageRsrc)rsrc;
                    String mime = new ImageAnalyzer(img.origData).getMime();
                    if (mime != null)
                    {
                        if (configData.jpegMin >= 0 && img.origData.length >= configData.jpegMin && mime.endsWith("jpeg"))
                        {
                            img.origData = ImageUtils.optimizedJpeg(img.origData, configData);
                            img.isOptimized = true;
                        }
                        img.base64Data = ("data:" + mime + ";base64," +
                                                ImageUtils.encode(img.origData)).getBytes();
                    }
                }

                if (responseToBrowser != null)
                {
                    rsrc.optimGzip = responseToBrowser;
                    rsrc.optimGzipEncoding = "gzip";
                }

                if (isVersionedRsrc)
                {
                    rsrc.versionUrl = versionUrl;
                    rsrc.versionUrlBytes = versionUrl.getBytes();
                }
                rsrc.host = host;
                rsrc.variant = variant;
                rsrc.url = url;
                rsrc.origEtag = origEtag;
                rsrc.origLastModified = origLastModified;
                rsrc.origMd5 = origMd5;

                if (rsrc.optimGzip == null)
                {
                    // !isHtml and !isCss:
                    if (rsrc instanceof ImageRsrc && ((ImageRsrc)rsrc).isOptimized)
                    {
                        isGziped = false;
                        responseToBrowser = rsrc.origData;
                    }
                    else if (exchange.isGziped())
                    {
                        rsrc.optimGzip = exchange.getResponseContent();
                        rsrc.optimGzipEncoding = "gzip";
                    }
                    else if (exchange.isDeflated())
                    {
                        rsrc.optimGzip = exchange.getResponseContent();
                        rsrc.optimGzipEncoding = "deflate";
                    }
                    else
                    {
                        rsrc.optimData = exchange.getResponseContent();
                    }
                }

                if (rsrc.httpHeaders == null)
                    rsrc.httpHeaders = cachedHeaders.toArray(new String[cachedHeaders.size()]);

                maxAge = CacheUtils.maxAgeOf(exchange, !isStateFull, configData);

                if (context.refreshRsrc != null && context.refreshRsrc.isBeingRefreshed)
                {
                    ServiceUtils.endRefreshAndNotifyWaiters(
                                                        exchange, context.refreshRsrc, rsrc /* modifiedRsrc */, maxAge, configData);
                    context.refreshRsrc = null; // Refresh succeeded.
                    if (configData.isDebug)
                        System.out.println("URL: " + url + " from ip: " + request.getRemoteAddr() + " (refreshed - modified)");
                }
                else
                {
                    rsrc.lastRefreshTime = System.currentTimeMillis();
                    rsrc.maxAge = maxAge;
                    rsrc.maxFreshTime = rsrc.lastRefreshTime + maxAge*1000;
                    synchronized (cache.globalLock)
                    {
                        ServiceUtils.putInCache(rsrc.variant, rsrc.url, rsrc, configData);

                        if (isVersionedRsrc && versionUrl != null)
                            ServiceUtils.putInCache(rsrc.variant, versionUrl, rsrc, configData);
                    }
                }
            }

            if (is304)
                return; // Status 304 has been returned and rsrc has been added to cache.

            if (isVersionedRsrc)
                maxAge = 31536000; // Cache for another year.
            if (maxAge >= 0)
            {
                if (isHtml && !isStateFull && configData.minStateLessVariant != configData.maxStateLessVariant)
                    response.setHeader("Cache-Control", "private, max-age=" + maxAge);
                else
                    response.setHeader("Cache-Control", "max-age=" + maxAge);
            }

            if (etag == null)
            {
                if (isVersionedRsrc)
                {
                    etag = "1";
                }
                else if (status == 304)
                {
                    etag = prevEtag;
                }
                else if (origMd5 != null)
                {
                    // Setting etag to origEtag + {|[ + md5 + variant + }|]:
                    char leftBrace = isHtml && isStateFull ? '{' : '[';
                    char rightBrace = isHtml && isStateFull ? '}' : ']';
                    etag = (origEtag != null ? origEtag : "") +
                                    leftBrace + origMd5 + variant + rightBrace;
                }
                else
                {
                    etag = origEtag;
                }
            }
            if (etag != null)
            {
                response.setHeader("ETag", etag);
            }

        }
        finally
        {
            // Recovering from a failure during refresh:
            if (!isRouter && context.refreshRsrc != null && context.refreshRsrc.isBeingRefreshed)
            {
                // Rollback and postpone the refresh of rsrc by 1 minute:
                synchronized (cache.globalLock)
                {
                    context.refreshRsrc.isBeingRefreshed = false;
                    Rsrc rsrc = context.refreshRsrc.replacer;
                    rsrc.isBeingLoaded = false;
                    rsrc.lastRefreshTime = System.currentTimeMillis();
                    rsrc.maxAge = 60;
                    rsrc.maxFreshTime = rsrc.lastRefreshTime + 60*1000;
                    ServiceUtils.putInCache(rsrc.variant, rsrc.url, rsrc, configData);

                    // Notify waiters:
                    if (context.refreshRsrc.waiters != null)
                    {
                        for (LoadLock w : context.refreshRsrc.waiters)
                            w.decCount();
                        context.refreshRsrc.waiters = null;
                    }
                }

                System.out.println("URL: " + url + " from ip: " + request.getRemoteAddr() + " (refreshed - failed)");
            }
        }
            
        if (responseToBrowser == null)
        {
            if (!isRouter && context.info != null)
            {
                isGziped = true;
                responseToBrowser = ServiceUtils.buildResponseToBrowser(context, isGziped);
            }
            else if (exchange != null)
            {
                isGziped = exchange.isGziped();
                responseToBrowser = exchange.getResponseContent();
            }
            else
            {
                // Shouldn't happen but just in case:
                response.setStatus(500);
                return;
            }
        }

        response.setHeader("Content-Length", responseToBrowser.length + "");
        if (isGziped)
            response.setHeader("Content-Encoding", "gzip");

        response.getOutputStream().write(responseToBrowser);

        }
        finally
        {
            if (configData != null)
                configData.lock.readLock().unlock();

            ConfigUtils.configLock.readLock().unlock();
        }
    }

    /*
     * (non-Javadoc)
     *
     * @see javax.servlet.Servlet#getServletInfo()
     */
    @Override
    public String getServletInfo()
    {
        return "HtmlSpeedServlet";
    }

    /**
     * Extension point for custom handling of an HttpExchange's onConnectionFailed method. The default implementation delegates to
     * {@link #handleOnException(Throwable, javax.servlet.http.HttpServletRequest, javax.servlet.http.HttpServletResponse)}
     *
     * @param ex
     * @param request
     * @param response
     */
    protected void handleOnConnectionFailed(Throwable ex, HttpServletRequest request, HttpServletResponse response)
    {
        handleOnException(ex,request,response);
    }

    /**
     * Extension point for custom handling of an HttpExchange's onException method. The default implementation sets the response status to
     * HttpServletResponse.SC_INTERNAL_SERVER_ERROR (503)
     *
     * @param ex
     * @param request
     * @param response
     */
    protected void handleOnException(Throwable ex, HttpServletRequest request, HttpServletResponse response)
    {
        LOG.warn(ex.toString());
        LOG.debug(ex);
        if (!response.isCommitted())
        {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Extension point for custom handling of an HttpExchange's onExpire method. The default implementation sets the response status to
     * HttpServletResponse.SC_GATEWAY_TIMEOUT (504)
     *
     * @param request
     * @param response
     */
    protected void handleOnExpire(HttpServletRequest request, HttpServletResponse response)
    {
        if (!response.isCommitted())
        {
            response.setStatus(HttpServletResponse.SC_GATEWAY_TIMEOUT);
        }
    }

    /**
     * Called by the method service to handle htmlspeed control requests.
     *
     * @param uri the uri of the servlet request
     * @param response the servlet response
     * @return true when uri is an html-speed request (which has been processed)
     * @throws IOException can happen when writing to output-stream of response
     */
    private boolean handleHtmlSpeedService(String uri, HttpServletResponse response) throws IOException, ServletException
    {
        if (uri.equals("/htmlspeed.html"))
        {
            // Handling request for support-page of content-first iframe:
            response.setHeader("Content-Type", "text/html");
            response.setHeader("Content-Length", HTML_SPEED_HTML_BYTES.length + "");
            response.setHeader("Cache-Control", "max-age=86400"); // Cached for 24 hours.
            response.setHeader("Etag", "\"001.00\"");
            response.setStatus(200);
            response.getOutputStream().write(HTML_SPEED_HTML_BYTES);
            return true;
        }
        else if (ConfigUtils.isGaliel && uri.startsWith("/htmlspeed/"))
        {
            if (uri.startsWith("/htmlspeed/load/"))
            {
                ConfigUtils.load(uri.substring("/htmlspeed/load/".length()), false /* cleanCache */);
                return true;
            }
            if (uri.startsWith("/htmlspeed/clean/"))
            {
                ConfigUtils.load(uri.substring("/htmlspeed/clean/".length()), true /* cleanCache */);
                return true;
            }
            else if (uri.startsWith("/htmlspeed/init/"))
            {
                // Replacing ReadLock by WriteLock and loading all config-files:
                ConfigUtils.configLock.readLock().unlock();
                ConfigUtils.configLock.writeLock().lock();
                try
                {
                    ConfigUtils.loadAllConfigFiles();
                }
                finally
                {
                    ConfigUtils.configLock.writeLock().unlock();
                    ConfigUtils.configLock.readLock().lock();
                }
                return true;
            }
            else if ( !ConfigUtils.hostInfoFileExists)
            {
                initHostInfo(uri);
                String primeHost = ServiceUtils.getPrimeHost();
                response.setHeader("Content-Type", "text/html");
                StringBuilder sb = new StringBuilder(4096);
                sb.append("<html><body>Tested site changed to: ");
                sb.append(primeHost);
                sb.append(",&nbsp;Addresses: ");
                sb.append(ServiceUtils.getAddresses());
                sb.append(_isWithFirstPlus ? ",&nbsp;&nbsp;&nbsp;[with first-plus optimizations]&nbsp;" : "");
                sb.append(_isWithIframe ? ",&nbsp;&nbsp;&nbsp;(with content-first optimizations)" : "");
                sb.append("<br/><br/>");
                sb.append("</body></html>");
                String out = sb.toString();
                response.setHeader("Content-Length", out.length() + "");
                response.setStatus(200);
                response.getOutputStream().write(out.getBytes());

                return true;
            }
        }

        return false;
    }

    /**
     * Content provider hosts configuration.
     *
     *      /htmlspeed/[withfirstplus/]domain1,domain2,domain3,ip1,ip2,...,/
     *
     * When hostDef starts with "/htmlspeed/withfirstplus/" then first-plus visit optimization is enabled.
     * When hostDef ends with '/' content-first optimization is enabled.
     * The first listed domain is selected to be the primary-host.
     *
     * @param hostDef hosts to ip-addresses definition
     */
    public void initHostInfo(String hostDef)
    {
        String hostInfo;

        // Handling request to switch to another tested website (example: /htmlspeed/www.kuku.com):
        if (hostDef.startsWith("/htmlspeed/withfirstplus/"))
        {
            _isWithFirstPlus = true;
            hostInfo = hostDef.substring(25);
        }
        else
        {
            _isWithFirstPlus = false;
            hostInfo = hostDef.substring(11);
        }

        if (hostInfo.endsWith("/"))
        {
            _isWithIframe = true;
            hostInfo = hostInfo.substring(0, hostInfo.length() - 1);
        }
        else
        {
            _isWithIframe = false;
        }
        ServiceUtils.setWithIframe(_isWithIframe);

        if (hostInfo.indexOf(',') < 0)
        {
            ServiceUtils.setHostInfo(new String[]{hostInfo});
        }
        else
        {
            String[] parts = hostInfo.split(",");
            ServiceUtils.setHostInfo(parts);
        }
    }

    /**
     * @param gzipedBytes gziped array of bytes
     * @return the original content (null when gzipedBytes is null)
     */
    private byte[] toUngzip(byte[] gzipedBytes)
    {
        if (gzipedBytes == null)
            return null;

        try
        {
            MyByteArrayInputStream bais = new MyByteArrayInputStream(gzipedBytes);
            GZIPInputStream is = new GZIPInputStream(bais);
            byte[] buff = new byte[16*10240];
            ByteArrayOutputStream os = new ByteArrayOutputStream(gzipedBytes.length * 2);
            int n;
            while (bais.getPos() < gzipedBytes.length && (n = is.read(buff)) >= 0)
                os.write(buff, 0, n);

            byte [] bytes = os.toByteArray();

            is.close();
            os.close();

            return bytes;
        }
        catch (IOException exc)
        {
            return null;
        }
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
