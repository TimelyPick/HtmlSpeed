/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 12 April 2012
 */
package com.htmlspeed.server;

import java.util.HashMap;
import java.util.HashSet;

/**
 * CacheStructure
 *
 * Cache of resources loaded from content-server and from
 * other websites into the memory of the html-speed server.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.20 $$Date: 2013/08/21 12:59:12 $
 */
public class CacheStructure
{
    /**
     * Used for synchronizing access to cache-structure.
     * rsrcs should only be accessed when globalLock is locked.
     */
    public final Object globalLock = new Object();

    /**
     * A collection of all stateless (and half-state-full) resources that are currently
     * loaded into cache-structure. This collection is a hash map from url to rsrc.
     *
     * Important: rsrcs should only be accessed when globalLock is locked.
     */
    public HashMap<String, RsrcIfc> rsrcs = new HashMap<String,RsrcIfc>(8*1024);

    /**
     * Urls of all previously loaded state-full resources.
     * Should only be accessed while globalLock is locked.
     */
    public HashSet<String> stateFullRsrcs = new HashSet<String>(8*1024);

    /**
     * A doubly linked list of buffers containing original/optimized/gziped resources
     * ordered by load/usage time. Currently bufs is the only timed-buffers-list.
     */
    public RsrcsList rsrcsList;

    /**
     * Private constructor of the single instance of CacheStructure
     */
    public CacheStructure(ConfigData configData)
    {
        rsrcsList = new RsrcsList(configData);
    }

    /**
     * Note: should only be called when globalLock is locked.
     *
     * @param url full url of resource
     * @param configData used configuration
     * @return true when url is a previously loaded state-full rsrc or configured to be state-full
     */
    public boolean isStateFull(String url, ConfigData configData)
    {
        if (stateFullRsrcs.contains(url))
            return true;
        if (configData.isStateFull(url))
            return true;
        return false;
    }

    /**
     * Adds full-url to list of previously-loaded (known) state-full resources.
     *
     * Note: should only be called when globalLock is locked.
     *
     * @param url full url of newly encountered state-full resource.
     */
    public void addStateFull(String url)
    {
        stateFullRsrcs.add(url);
    }

}
