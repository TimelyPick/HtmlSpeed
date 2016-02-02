/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

import java.io.Serializable;

/**
 * Rsrc
 *
 * The base-class of all cached resources.
 *
 * The variables maxFreshTime, lastRefreshTime, isBeingLoaded of cached
 * resources are only allowed to be read/write while cache.globalLock is locked.
 *.
 * Other variables of cached resources are not allowed to be modified.
 * Instead of modifying them, the entire resource instance is replaced.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.30 $$Date: 2013/08/21 09:05:28 $
 */
public abstract class Rsrc implements RsrcIfc, Serializable
{
    /**
     * 14 days (in milli-seconds) are considered recent-time.
     */
    public static final long RECENT_TIME_MILLIS = 1000*3600*24*14;

    public String host; // Domain from which rsrc has been loaded.

    public String url; // Full http[s] url of style-sheet.

    /**
     * Url of version-resource:  '/' + CacheUtils.VERSION_PREFIX + xxx + '.' + suffix
     *      xxx: md5 of origData,
     *      suffix: js, css, ...
     */
    public String versionUrl;

    /**
     * versionUrl.getBytes()
     */
    public byte[] versionUrlBytes;

    public boolean isBeingLoaded; // True when resource is being loaded or refreshed.

    public long maxAge; // max-age of rsrc when last loaded or refreshed.
    public long maxFreshTime; // Until when this rsrc is fresh.
    public long lastRefreshTime; // Load or last-refresh time.
    public long lastUsageTime = System.currentTimeMillis(); // The last time when the resource has been used.

    public String[] httpHeaders;
                    //
                    // Pairs of (header-name, value) to be returned to browsers,
                    // when not inlined, except for Etag and max-age
                    // may be null for delta-pages

    public String origEtag; // ETag header field (from content-server)
    public String origLastModified; // Last-Modified header field (from content-server)
    public String origMd5; // The md5 (in base64) of origData.

    public byte[] origData; // May be null for delta-pages, always null for large images
    public byte[] origGzip; // origData gzipped.
    public byte[] optimData; // Optimized resource.
    public byte[] optimGzip; // optimData gzipped.
    public String origGzipEncoding; // When origGzip != null equals "gzip" or "deflate"
    public String optimGzipEncoding; // When optimGzip != null equals "gzip" or "deflate"

    public char variant; // variant of resource.

    /**
     * A more recently accessed instance.
     */
    private Rsrc _next;

    /**
     * A less recently accessed instance.
     */
    private Rsrc _prev;

    /**
     * @return the total-size of all timed-buffers.
     */
    public int getTotalTimedBuffersSize()
    {
        int total = 0;
        total += (origData != null ? origData.length : 0);
        total += (origGzip != null ? origGzip.length : 0);
        total += (optimData != null ? optimData.length : 0);
        total += (optimGzip != null ? optimGzip.length : 0);
        return total;
    }

    /**
     * @return true when resource is being loaded or refreshed.
     */
    public boolean isBeingLoaded()
    {
        return isBeingLoaded;
    }

    /**
     * @param currentTime value returned by System.currentTimeMillis (saves a lot of invocations)
     * @return true when this resource has been accessed recently compared to currentTime
     */
    public boolean isRecentlyUsed(long currentTime)
    {
        return currentTime - lastUsageTime < RECENT_TIME_MILLIS;
    }

    /**
     * @return The host from which resource has been loaded
     */
    public String getHost()
    {
        return host;
    }

    /**
     * @return full-url of resource.
     */
    public String getUrl()
    {
        return url;
    }

    /**
     * @return the variant (visit-code) of resource
     */
    public char getVariant()
    {
        return variant;
    }

    /**
     * @return The url of the versioned-resource attached to the resource (may be null).
     */
    public String getVersionUrl()
    {
        return versionUrl;
    }

    /**
     * @return the next timed-buffer in the linked-list (more recent)
     */
    public Rsrc getNext()
    {
        return _next;
    }

    /**
     * Assign new value to property next.
     *
     * The next timed-buffer in the linked-list (more recent)
     *
     * @param next new value of property
     */
    public void setNext(Rsrc next)
    {
        _next = next;
    }

    /**
     * @return the previous timed-buffer in the linked-list (less recent)
     */
    public Rsrc getPrev()
    {
        return _prev;
    }

    /**
     * Assign new value to property prev.
     *
     * The prev timed-buffer in the linked-list (more recent)
     *
     * @param next new value of property
     */
    public void setPrev(Rsrc prev)
    {
        _prev = prev;
    }

}
