/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 18 June 2012
 */
package com.htmlspeed.server;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Hashtable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * CacheUtils
 *
 * Utility methods used for controling caching of resources by the browser.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.50 $$Date: 2013/08/26 14:16:16 $
 */
public class CacheUtils
{

    /**
     * Length in characters of md5 used as Etag.
     */
    public static final int MD5_LENGTH = 22;

    /**
     * Suffix of versioned paths
     */
    public static String VERSION_SUFFIX = "htmlspeed";

    /**
     * Length of VERSION_SUFFIX
     */
    public static final int VERSION_SUFFIX_LEN = VERSION_SUFFIX.length();

    /**
     * The single variant of non-page rsrc
     */
    public static final char NON_PAGE_VARIANT = '#';

    /**
     * In first visit aggressive inline is applied.
     */
    public static final char FIRST_VISIT_VARIANT = '0';

    /**
     * In first-plus visit (first visit when referrer is in site) similar to
     * second visit with the exception that all images remain inline.
     */
    public static final char FIRST_PLUS_VISIT_VARIANT = '1';

    /**
     * In second visit 1/3 of rsrcs are not inlined.
     */
    public static final char SECOND_VISIT_VARIANT = '2';

    /**
     * In third visit 2/3 of rsrcs are not inlined.
     */
    public static final char THIRD_VISIT_VARIANT = '3';

    /**
     * In forth visit all rsrcs are not inlined.
     */
    public static final char FORTH_VISIT_VARIANT = '4';

    /**
     * In first visit aggressive inline is applied.
     * In IE8 devices images larget than 24K (binary) are not inlined.
     * Also content-first optimization is applied.
     */
    public static final char IE8_FIRST_VISIT_VARIANT = 'a';

    /**
     * In first-plus visit (first visit when referrer is in site) similar to
     * second visit with the exception that all images remain inline.
     * In IE8 devices images larget than 24K (binary) are not inlined.
     * Also content-first optimization is applied.
     */
    public static final char IE8_FIRST_PLUS_VISIT_VARIANT = 'b';

    /**
     * In second visit 1/3 of rsrcs are not inlined.
     * In IE8 devices images larget than 24K (binary) are not inlined.
     * Also content-first optimization is applied.
     */
    public static final char IE8_SECOND_VISIT_VARIANT = 'c';

    /**
     * In third visit 2/3 of rsrcs are not inlined.
     * In IE8 devices images larget than 24K (binary) are not inlined.
     * Also content-first optimization is applied.
     */
    public static final char IE8_THIRD_VISIT_VARIANT = 'd';

    /**
     * In forth visit all rsrcs are not inlined.
     * In IE8 devices images larget than 24K (binary) are not inlined.
     * Also content-first optimization is applied.
     */
    public static final char IE8_FORTH_VISIT_VARIANT = 'e';

    /**
     * In first visit aggressive inline is applied.
     * In mobile devices images larget than 24K (binary) are not inlined.
     */
    public static final char MOBILE_FIRST_VISIT_VARIANT = 'A';

    /**
     * In first-plus visit (first visit when referrer is in site) similar to
     * second visit with the exception that all images remain inline.
     * In mobile devices images larget than 24K (binary) are not inlined.
     */
    public static final char MOBILE_FIRST_PLUS_VISIT_VARIANT = 'B';

    /**
     * In second visit 1/3 of rsrcs are not inlined.
     * In mobile devices images larget than 24K (binary) are not inlined.
     */
    public static final char MOBILE_SECOND_VISIT_VARIANT = 'C';

    /**
     * In third visit 2/3 of rsrcs are not inlined.
     * In mobile devices images larget than 24K (binary) are not inlined.
     */
    public static final char MOBILE_THIRD_VISIT_VARIANT = 'D';

    /**
     * In forth visit all rsrcs are not inlined.
     * In mobile devices images larget than 24K (binary) are not inlined.
     */
    public static final char MOBILE_FORTH_VISIT_VARIANT = 'E';

    /**
     * A list of all possible variants of a page:
     */
    public static final char[] ALL_VARIANTS = {
            FIRST_VISIT_VARIANT,
            FIRST_PLUS_VISIT_VARIANT,
            SECOND_VISIT_VARIANT,
            THIRD_VISIT_VARIANT,
            FORTH_VISIT_VARIANT,
            IE8_FIRST_VISIT_VARIANT,
            IE8_FIRST_PLUS_VISIT_VARIANT,
            IE8_SECOND_VISIT_VARIANT,
            IE8_THIRD_VISIT_VARIANT,
            IE8_FORTH_VISIT_VARIANT,
            MOBILE_FIRST_VISIT_VARIANT,
            MOBILE_FIRST_PLUS_VISIT_VARIANT,
            MOBILE_SECOND_VISIT_VARIANT,
            MOBILE_THIRD_VISIT_VARIANT,
            MOBILE_FORTH_VISIT_VARIANT
    };

    /**
     * Offsets to apply to first-visit variants of listed url's.
     *
     * Entries are created on the fly for url's that return a response that is larger than
     * 500 Kbytes for first-visit. For each such url an entry is created that maps the
     * url to the offset used for incrementing the first-visit variants of the url.
     *
     * Example: if "http://www.kuku.com/" is mapped to 3 then when this url is requested
     * variant FIRST_VISIT_VARIANT will be replaced by THIRD_VISIT_VARIANT,
     * and variant IE8_FIRST_PLUS_VISIT_VARIANT will be replaced by IE8_THIRD_VISIT_VARIANT,
     * and variant MOBILE_FIRST_PLUS_VISIT_VARIANT will be replaced by MOBILE_THIRD_VISIT_VARIANT.
     */
    public static ConcurrentHashMap<String, Integer> firstVisitOffsets = new ConcurrentHashMap<String, Integer>(32);

    /**
     * An MD5 message-digest
     */
    private static MessageDigest _msgDigest = null;

    static
    {
        // Initializing _msgDigest:
        try
        {
            _msgDigest =  MessageDigest.getInstance("MD5");
        }
        catch (NoSuchAlgorithmException exc)
        {
            System.out.println("Error calling MessageDigest.getInstance(MD5), " + exc.getMessage());
        }
    }

    /**
     *  Path of versioned-resource:  modified-url + md5 + variant + CacheUtils.VERSION_PREFIX
     *      modified-url: url with "http[s]:" modified to "/http[s]"
     *      variant: a single character selecting the variant of resource
     *      md5: md5 of origData,
     *      suffix: js, css, ...
     *
     * Note: Resources that are cached for more than 14 days are not versioned.
     *          Also versioning is disabled when value of property versioning is false.
     *
     * @param url full url of resource.
     * @param variant selects the variant of resource.
     * @param content the content of a resource-file
     * @param configData used configuration
     * @return a versioned url to specified content with specified suffix (null when rsrc shouldn't be versioned).
     */
    public static String versionUrlOf(
                                    String host,
                                    String url,
                                    char variant,
                                    byte[] content,
                                    HtmlSpeedHttpExchange exchange,
                                    ConfigData configData)
    {
        if (!configData.isVersioning || maxAgeOf(exchange, false, configData) >= 3600*24*14)
            return null; // Versioning is disabled or no need to version rsrc that is cached for more than 14 days

        StringBuilder sb = new StringBuilder(32);

        // Replacing "http[s]://" by "/http[s]/":
        if (url.startsWith("http://"))
            sb.append("http:/").append(host).append("/http/").append(url.substring(7));
        else if (url.startsWith("https://"))
            sb.append("https:/").append(host).append("/https/").append(url.substring(8));
        else
            sb.append(url);

        sb.append(md5Of(content));
        sb.append(VERSION_SUFFIX);
        return sb.toString();
    }

    /**
     * @param rsrcUrl url of resource
     * @return true when rsrcUrl is versioned
     */
    public static boolean isVersionUrl(String rsrcUrl)
    {
        return
                rsrcUrl.length() > VERSION_SUFFIX_LEN + MD5_LENGTH &&
                rsrcUrl.endsWith(VERSION_SUFFIX);
    }

    /**
     * @param versionUrl versioned-path of a resource
     * @return the original url of the resource
     */
    public static String originalUrlOf(String versionUrl)
    {
        if (!isVersionUrl(versionUrl))
            return versionUrl;

        final int fullSuffixLen = VERSION_SUFFIX_LEN + MD5_LENGTH;

        if (versionUrl.startsWith("http:"))
        {
            int first = versionUrl.indexOf("/http") + 6;
            return "http://" + versionUrl.substring(first, versionUrl.length() - fullSuffixLen);
        }
        else if (versionUrl.startsWith("https:"))
        {
            int first = versionUrl.indexOf("/https") + 7;
            return "https://" + versionUrl.substring(first, versionUrl.length() - fullSuffixLen);
        }
        else
            return versionUrl.substring(0, versionUrl.length() - fullSuffixLen);
    }

    /**
     * @param content of resource fetched from content-servers
     * @return the md5 of content in base64 representation (each '/' is replaced by '-'),
     *                                      null when count is null or empty
     */
    public static synchronized String md5Of(byte[] content)
    {
        if (content == null || content.length == 0)
            return null;

        if (_msgDigest == null)
            return null;
        _msgDigest.reset();
        _msgDigest.update(content);
        byte[] digest = _msgDigest.digest();
        String base64 = ImageUtils.encode(digest);
        base64 = base64.substring(0, base64.length() - 2); // Removing trailing "==".
        base64 = base64.replace('/', '-');
        String md5 = base64.replace('+', '_');
        return md5;
    }

    /**
     * @param variant char code of a page variant
     * @return offset of variant from low limit of the group of variants
     */
    public static int offsetOf(char variant)
    {
        if (FIRST_VISIT_VARIANT <= variant && variant <= FORTH_VISIT_VARIANT)
            return variant - FIRST_VISIT_VARIANT;
        else if (IE8_FIRST_VISIT_VARIANT <= variant && variant <= IE8_FORTH_VISIT_VARIANT)
            return variant - IE8_FIRST_VISIT_VARIANT;
        else
            return variant - MOBILE_FIRST_VISIT_VARIANT;
    }

    /**
     * @param variant char code of a page variant
     * @return first in group of variants containing variant
     */
    public static char firstInGroupOf(char variant)
    {
        if (variant == NON_PAGE_VARIANT)
            return variant;

        if (FIRST_VISIT_VARIANT <= variant && variant <= FORTH_VISIT_VARIANT)
            return FIRST_VISIT_VARIANT;
        else if (IE8_FIRST_VISIT_VARIANT <= variant && variant <= IE8_FORTH_VISIT_VARIANT)
            return IE8_FIRST_VISIT_VARIANT;
        else
            return MOBILE_FIRST_VISIT_VARIANT;
    }

    /**
     * When etag is null or unrecognized FIRST_VISIT_VARIANT is returned.
     *
     * Otherwise the variant identified by the etag is the just before the last character
     * (the last character is ']' or '}').
     *
     * @param url the url of http request
     * @param etag passed to the service by the browser via http-header If-None-Match.
     * @param isIE8 true when requesting browser is IE8
     * @param isMobile true when requesting browser is a mobile device
     * @param isRobot true when requesting client is a search-engine robot
     * @return the variant to be looked-up (stateless)
     */
    public static char variantOf(String url, String etag, boolean isIE8, boolean isMobile, boolean isRobot)
    {
        if (isRobot)
            return FORTH_VISIT_VARIANT; // No inline for search-engines.

        if (etag != null)
        {
            int len = etag.length();

            if (len >= MD5_LENGTH + 3 && etag.charAt(len - 1) == '}' &&
                    etag.charAt(len - MD5_LENGTH - 3) == '{')
            {
                return etag.charAt(len - 2); // ETag of state-full page.
            }

            if (len >= MD5_LENGTH + 3 && etag.charAt(len - 1) == ']' &&
                    etag.charAt(len - MD5_LENGTH - 3) == '[')
            {
                return etag.charAt(len - 2); // ETag of state-less page or any non-page resource.
            }
        }

        // Using first-visit when etag is null or not a recognized ETag:
        char variant;
        if (isIE8)
            variant = IE8_FIRST_VISIT_VARIANT;
        else if (isMobile)
            variant = MOBILE_FIRST_VISIT_VARIANT;
        else
            variant = FIRST_VISIT_VARIANT;

        // Incrementing first-visit when url is known to generate content larger than 500 Kbytes:
        Integer offsetObj = firstVisitOffsets.get(url);
        if (offsetObj != null)
            variant += offsetObj.intValue();

        return variant;
    }

    /**
     * @param etag http ETag header received from browser
     * @return true when etag is signed by HtmlSpeed
     */
    public static boolean isHtmlSpeedEtag(String etag)
    {
        if (etag == null)
            return false;

        int len = etag.length();

        if (len >= MD5_LENGTH + 3 && etag.charAt(len - 1) == '}' &&
                etag.charAt(len - MD5_LENGTH - 3) == '{')
        {
            return true;
        }

        if (len >= MD5_LENGTH + 3 && etag.charAt(len - 1) == ']' &&
                etag.charAt(len - MD5_LENGTH - 3) == '[')
        {
            return true;
        }

        return false;
    }

    /**
     * @param etag passed to the service by the browser via http-header If-None-Match.
     * @return the md5 (in base64 format) contained in etag
     */
    public static String md5Of(String etag)
    {
        int len = etag.length();
        return etag.substring(0, len-1);
    }

    /**
     * @param md5 the md5 (in base64 format) of the content of the resource.
     * @param variant the variant of the rsrc selected by the etag.
     * @return an ETag built using md5 and variant of rsrc (used for checking if rsrc has changed)
     */
    public static String toEtag(String md5, char variant)
    {
        return md5 + variant;
    }

    /**
     * @param url full url of resource
     * @param exchange contains response from webserver
     * @param isHtml true when url is an html page
     * @param configData used configuration
     * @return true when path is state-full
     */
    public static boolean isStateFull(String url, HtmlSpeedHttpExchange exchange, boolean isHtml, ConfigData configData)
    {

        if (configData.isSetCookieStatefull && exchange.getSetCookieIndexes() != null)
            return true; // Response contains "Set-Cookie" header
        else if (configData.isStateFull(url))
            return true;
        else if (configData.isStateLess(url))
            return false;
        else if (isHtml && configData.isStateFull("html"))
            return true;
        else if (isHtml && configData.isStateLess("html"))
            return false;

        long maxAge = maxAgeOf(exchange, false, configData);
        return (0 <= maxAge && maxAge < configData.minStateLessMaxAge);
    }

    /**
     * @param exchange contains http response headers
     * @param calcDefault when true a calculated value is returned when unspecified, otherwise -1 is returned when unspecified
     * @param isKnownToBeStateLess true when rsrc is known to be state-less
     * @param configData used configuration
     * @return value of max-age computed from Exipres, Cache-Control, and Pragma http-headers
     */
    public static long maxAgeOf(HtmlSpeedHttpExchange exchange, boolean isKnownToBeStateLess, ConfigData configData)
    {
        String[] httpHeaders = exchange.getResponseHeaders();
        int cacheControlIndex = exchange.getCacheControlIndex();
        int expiresIndex = exchange.getExpiresIndex();
        int pragmaIndex = exchange.getPragmaIndex();

        //
        // Decision is based on http-headers:
        //
        long maxAge = (-1); // -1 stands for undefined

        if (cacheControlIndex >= 0)
        {
            String cacheControlValue = httpHeaders[cacheControlIndex + 1].toLowerCase();
            if ((cacheControlValue.contains("no-cache") || cacheControlValue.contains("private") ||
                    cacheControlValue.contains("no-store")) && !isKnownToBeStateLess)
            {
                maxAge = 0; // Resource contains user-dependent content
            }
            else
            {
                int maxAgeIndex = cacheControlValue.indexOf("max-age");
                if (maxAgeIndex >= 0)
                {
                    int len = cacheControlValue.length();

                    int first = maxAgeIndex + 7;
                    while (first < len && !Character.isDigit(cacheControlValue.charAt(first)))
                        first++;

                    int last = first;
                    while (last < len && Character.isDigit(cacheControlValue.charAt(last)))
                        last++;

                    if (first == last)
                        maxAge = 0;
                    else
                        maxAge = Integer.parseInt(cacheControlValue.substring(first, last));
                }
            }
        }

        if (expiresIndex >= 0 && maxAge < 0)
        {
            String expiresValue = httpHeaders[expiresIndex + 1];
            try
            {
                if (expiresValue.equals("0"))
                    maxAge = 0;
                else
                    maxAge = (DateUtils.parseDate(expiresValue).getTime() - System.currentTimeMillis()) / 1000;

                if (maxAge < 0)
                    maxAge = 0;
            }
            catch (Exception exc)
            {
                maxAge = 0;
                System.out.println("!!!!!!!!!!!! maxAgeOf can't parse Expires header: " + expiresValue);
            }
        }

        if (pragmaIndex >= 0)
        {
            String pragmaValue = httpHeaders[pragmaIndex + 1];
            if (pragmaValue.equalsIgnoreCase("no-cache"))
                maxAge = 0;
        }

        // Calculating default value for max-age (similar to IE algorithm),
        // when caching info is not specified in response:
        if (maxAge == (-1))
        {
            int index = exchange.getLastModifiedIndex();

            if (index >= 0)
            {
                String lastModified = httpHeaders[index + 1];
                try
                {
                    // maxAge is 10% of time interval from lastModified to now:
                    maxAge = (System.currentTimeMillis() - DateUtils.parseDate(lastModified).getTime()) / (10*1000);

                    if (maxAge < 0)
                        maxAge = 0;
                }
                catch (Exception exc)
                {
                    System.out.println("!!!!!!!!!!!! maxAgeOf can't parse Last-Modified header: " + lastModified);
                }
            }
            else
            {
                maxAge = configData.minStateLessMaxAge; // Minimum state-lesss max-age when Last-Modified is missing
            }
        }

        if (isKnownToBeStateLess && maxAge < configData.minStateLessMaxAge)
            maxAge = configData.minStateLessMaxAge;

        return maxAge;
    }

}
