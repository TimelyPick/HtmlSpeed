/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 26 Februar 2013
 */
package com.htmlspeed.server;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * Cdn
 *
 * Routes src (or href) of state-less resources to configured CDN's.
 *
 * Also push state-less resources to push CDN's (such as Rackspace over Akamai)
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.8 $$Date: 2013/08/21 07:42:54 $
 */
public class Cdn
{

    /**
     * Checks if routing of tempRsrc.raplacer to a CDN is required, and
     * when true puts in tempRsrc.cdnUrl the url that is used by the CDN.
     *
     * @param tempRsrc the checked just loaded resource
     * @param configData used configuration
     */
    public static void handleCdn(TempRsrc tempRsrc, ConfigData configData)
    {
        if (tempRsrc.url == null)
            return;

        if (tempRsrc.replacer == null || tempRsrc.replacer instanceof PageRsrc)
            return;

        if (configData.cdnSrcHostInfos.size() == 0)
            return; // No configured CDNs.

        int hostFirst = tempRsrc.url.indexOf("//") + 2;
        if (hostFirst < 2)
            return;
        int hostLast = tempRsrc.url.indexOf('/', hostFirst);
        if (hostLast < 0)
            return;

        String srcHost = tempRsrc.url.substring(hostFirst, hostLast);

        ConfigData.SrcHostInfo srcHostInfo = configData.cdnSrcHostInfos.get(srcHost);
        if (srcHostInfo == null)
            return;

        int size = 0; // Size of checked rsrc

        // Finding size of tempRsrc.replacer:
        Rsrc replacer = tempRsrc.replacer;
        if (replacer instanceof ImageRsrc)
        {
            ImageRsrc img = (ImageRsrc)replacer;
            if (img.origData != null)
                size = img.origData.length;
        }
        else if (replacer instanceof JsRsrc)
        {
            JsRsrc js = (JsRsrc)replacer;
            if (js.optimGzip != null)
                size = js.optimGzip.length;
            else if (js.origGzip != null)
                size = js.origGzip.length;
            else if (js.optimData != null)
                size = js.optimData.length;
            else if (js.origData != null)
                size = js.origData.length;
        }
        else if (replacer instanceof CssRsrc)
        {
            CssRsrc css = (CssRsrc)replacer;
            if (css.optimGzip != null)
                size = css.optimGzip.length;
            else if (css.origGzip != null)
                size = css.origGzip.length;
        }

        String cdnHost = null; // The domain of selected CDN (null when no CDN has been selected)
        String cdnSslHost = null; // The SSL domain of selected CDN (null when cdnHost is null).

        // Checking if size is in range of any size-range of src-host:
        for (ConfigData.SizeRange sizeRange : srcHostInfo.sizeRanges)
        {
            if (sizeRange.minSize <= size && size <= sizeRange.maxSize)
            {
                cdnHost = sizeRange.cdnHost;
                cdnSslHost = sizeRange.cdnSslHost;
                break;
            }
        }

        // Assigning host (domain) of selected CDN to tempRsrc.cdnUrl:
        if (cdnHost != null)
        {
            String cdnUrl = "//" + cdnHost + tempRsrc.url.substring(hostLast);
            tempRsrc.cdnUrl = cdnUrl.getBytes();
            String cdnSslUrl = "//" + cdnSslHost + tempRsrc.url.substring(hostLast);
            tempRsrc.cdnSslUrl = cdnSslUrl.getBytes();
        }
    }

}
