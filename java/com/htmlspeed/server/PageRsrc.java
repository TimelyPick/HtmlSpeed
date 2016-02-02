/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

/**
 * PageRsrc
 *
 * Query-strings of stateless web-pages are encoded as last part of url.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.8 $$Date: 2013/01/04 08:10:40 $
 */
public class PageRsrc extends Rsrc
{
    /**
     * Few PageRsrc instances may be created for the same
     * content-server's page, depending on the browser-kind.
     *
     * The browserKind prefixes the path in the CacheStructure.rsrcs.
     *
     * Existing Kinds: IE5, IE6, IE7, IE8, IE9, PC, MO
     */
    public String browserKind;

}
