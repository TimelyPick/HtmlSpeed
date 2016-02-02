/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 12 April 2012
 */
package com.htmlspeed.server;

import java.util.ArrayList;

/**
 * TempRsrc
 *
 * A stateless resource of unknown type that is currently being loaded from content servers.
 * The temporary resource will be replaced by an instance of a subclass of Rsrc.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.8 $$Date: 2013/03/04 14:46:29 $
 */
public class TempRsrc implements RsrcIfc
{
    /**
     * The domain/sub-domain containing the loaded rsrc.
     */
    public String host;

    /**
     * Full http[s] url of resource.
     */
    public String url;

    /**
     * When not null the resource should be fetched by using this cdn-url.
     */
    public byte[] cdnUrl;

    /**
     * When not null the resource should be fetched by using this cdn-url when SSL is used
     */
    public byte[] cdnSslUrl;

    /**
     * True when rsrc is inside IE comment.
     */
    public boolean isInIEComment;

    /**
     * Loader (or refresher) of this resource.
     */
    public LoadLock loader;

    /**
     * Waiters for loader to finish.
     */
    public ArrayList<LoadLock> waiters;

    /**
     * True when resource is duplicated in same page/css
     */
    public boolean isDuplicated;

    /**
     * The resource being loaded/refreshed from content-server (its kind is known).
     */
    public Rsrc replacer;

    /**
     * True when replacer is not inlined and it should be versioned (with strong caching).
     */
    public boolean isVersioned;

    /**
     * The async http invocation (contains the loaded content).
     */
    public HtmlSpeedHttpExchange exchange;

    /**
     * True when loaded resource is known to be state-full
     */
    public boolean isStateFull;

    /**
     * True when loader is refreshing the replacer resource, by
     * conditionaly fetching the resource from the content-servers.
     */
    public boolean isBeingRefreshed;

}
