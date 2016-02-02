/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashSet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.client.HttpClient;

/**
 * ServiceContext
 *
 * Context information regarding current http-service.
 * Passed to all methods called by the service.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.40 $$Date: 2013/08/21 12:30:00 $
 */
public class ServiceContext
{
    /**
     * Full url of request sent by browser.
     */
    public String url;

    /**
     * Single instance of CacheStructure
     */
    public CacheStructure cache;

    /**
     * Currently executed servlet-request.
     */
    public HttpServletRequest request;

    /**
     * Input to servlet-request.
     */
    public InputStream in;

    /**
     * Servlet-response of currently executed request.
     */
    public HttpServletResponse response;

    /**
     * Used for invoking http-services on content server.
     */
    public HtmlSpeedHttpExchange exchange;

    /**
     * True if request is secured.
     */
    public boolean isHttps;

    /**
     * Http-headers to be droped by from http-request
     * when routing to content-server.
     */
    public HashSet<String> dropedHeaders;

    /**
     * Http headers returned from content server
     */
    public String[] responseHeaders;

    /**
     * Cached for reuse with the stateless resource
     */
    public ArrayList<String> cachedHeaders;

    /**
     * Status of http-request from content-server
     */
    public int status;

    /**
     * True when current service is to GET html page
     */
    public boolean isHtml;

    /**
     * True when current service is to GET css file
     */
    public boolean isCss;

    /*
     * Unziped response from content server
     */
    public byte[] orig;

    /**
     * Results of analyzing orig
     */
    public int[] info;

    /**
     * Resources found in orig
     */
    public RsrcIfc[] allRsrcs;

    /**
     * Used for invoking http services
     */
    public HttpClient client;

    /**
     * True when uri of service has iframe.
     */
    public boolean isServiceWithIframe;

    /**
     * When not null value of href attribute in base tag
     */
    public String base;

    /**
     * True when requested resource is statefull
     */
    public boolean isStateFull;

    /**
     * The previous md5 of content of requested statefull-resource.
     * Passed from browser to server by http-header "If-None-Match".
     * When the value of If-None-Match header ends with {xxx}
     * then xxx is the prevMd5.
     */
    public String prevMd5;

    /**
     * True when requested url/uri is versioned.
     */
    public boolean isVersionedRsrc;

    /**
     * Selects the variant of returned resource.
     * Example CacheUtils.FIRST_VISIT_VARIANT
     */
    public char variant;

    /**
     * True when request method is POST
     */
    public boolean isPost;

    /**
     * Content that is posted to server (relevent only when isPost is true).
     */
    public byte[] postedContent;

    /**
     * Allocated by ServiceUtils.tryToUseCachedRsrc when the
     * current service must refresh an existing state-less rsrc.
     */
    public TempRsrc refreshRsrc;

    /**
     * Set by ServiceUtils.findFreshCachedRsrc.
     * The time when freshness of cached resource expires.
     */
    public long maxFreshTime;

    /**
     * Set by ServiceUtils.findFreshCachedRsrc.
     * The max-age of rsrc when last loaded or refreshed.
     */
    public long maxAge;

    /**
     * True when browser is IE.
     */
    public boolean isIE;

    /**
     * True when browser is IE8.
     */
    public boolean isIE8;

    /**
     * True when client is a search-engine robot.
     */
    public boolean isRobot;

    /**
     * True when browser is mobile.
     */
    public boolean isMobile;

    /**
     * True when html-speed routes this http service to
     * content server without optimizing the response.
     */
    public boolean isRouter;

    /**
     * Host http-header of request.
     *
     * Can be null only in http/1.0 (and then defaulted to _contentProvider).
     */
    public String requestHost;

    /**
     * Host http-header of request for non-versioned rsrc, original-host for versioned-rsrc.
     *
     * Can be null only in http/1.0 (and then defaulted to _contentProvider).
     */
    public String host;

    /**
     * Number of bytes in ungziped response (set by ServiceUtils.buildResponseToBrowser).
     */
    public int ungzipedResponseLen;

    /**
     * True when host is unknown and "others" is specified in hostinfo.txt.
     */
    public boolean isOthers;

    /**
     * The configuration for host
     */
    public ConfigData configData;
}
