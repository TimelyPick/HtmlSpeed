/*
 *  Copyright 2001 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 */

/*
 * ConfigData.java
 *
 * Created on 21 Aug 2013
 */
package com.htmlspeed.server;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Properties;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * ConfigData.
 *
 *  Configuration data for a single domain
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.10 $$Date: 2013/08/28 12:07:01 $
 */
public class ConfigData
{
    /**
     * Used for for Read/Write locking this instance of ConfigData.
     */
    public ReentrantReadWriteLock lock = new ReentrantReadWriteLock(true);

    /**
     * The domain for whom this configuration is used.
     */
    public String domain;

    /**
     * When not-null lists other-domains with same config-data.
     */
    public String[] otherDomains;

    /**
     * Path to directory containing the directory domain.
     */
    public String dirPath;

   /**
     * Minimum size of TimedBuffer considered to be huge (never inlined).
     */
    public int minHugeBuffer = 64*1024;

    /**
     * Minimum size of TimedBuffer considered to be large.
     */
    public int minLargeBuffer = 32*1024;

    /**
     * Minimum size of TimedBuffer considered to be medium-sized.
     */
    public int minMediumBuffer = 8*1024;

    /**
     * Minimum size of TimedBuffer considered to be small-sized.
     * Buffers smaller than that are considered tiny.
     */
    public int minSmallBuffer = 200;

    /**
     * Used for forcing selected uri's/paths to be state-full.
     *
     * Htmlspeed automaticaly calssifies each uri/path as state-full
     * or state-less depending on caching http-headers. The state-
     * full patterns enables overriding the default behaviour.
     *
     * Each group of 4 entries in the array is used as a pattern:
     *     cmd, starts-with, contains, ends-with.
     *
     * cmd: ACCEPT or REJECT selected uri's.
     * starts-with: when not null selected uri's must start with this string-value
     * contains: when not null selected uri's must contain this string-value
     * ends-with: when not null selected uri's must end with this string-value.
     *
     * When starts-with equals EQUALS then contains selects a constant string value.
     */
    private String[] _stateFullPatterns = ConfigUtils.DEFAULT_STATE_FULL_PATTERNS;

    /**
     * Used for forcing selected uri's/paths to be state-full.
     *
     * Htmlspeed automaticaly calssifies each uri/path as state-full
     * or state-less depending on caching http-headers. The state-
     * full patterns enables overriding the default behaviour.
     *
     */
    private String[] _stateLessPatterns = ConfigUtils.DEFAULT_STATE_LESS_PATTERNS;

    /**
     * Used for selecting page uri's/paths for whom advertisement optimizations
     * are relevant. These are usually pages with many advertisements.
     *
     * Each group of 4 entries in the array is used as a pattern:
     *     cmd, starts-with, contains, ends-with.
     *
     * cmd: ACCEPT or REJECT selected uri's.
     * starts-with: when not null selected uri's must start with this string-value
     * contains: when not null selected uri's must contain this string-value
     * ends-with: when not null selected uri's must end with this string-value.
     *
     * When starts-with equals EQUALS then contains selects a constant string value.
     *
     * The first pattern selecting the uri determines its kind.
     *
     * Default patterns are used when no pattern is specified in configuration-file.
     */
    private String[] _contentFirstPatterns = ConfigUtils.DEFAULT_CONTENT_FIRST_PATTERNS;

    private static String[] _deferPatterns = ConfigUtils.DEFAULT_DEFER_PATTERNS;

    /**
     * Used for selecting resources that are not inlined in their container.
     * Mainly used when only a small number of images in a stylesheet are needed by a single-page.
     */
    private String[] _noInlinePatterns = ConfigUtils.DEFAULT_NO_INLINE_PATTERNS;

    /**
     * Contains all configuration properties.
     */
    private Properties _properties = new Properties();

    /**
     * Used by HtmlSpeedHttpExchange for replacing strings in responses
     */
    public boolean defIsReplacing = false;
    public byte[] defReplaceSrc = null;
    public int defReplaceDstSkipLen = 0;
    public byte[] defReplaceDstPrefix = null;
    public byte[] defReplaceDstSuffix = null;
    public byte[] defReplaceBefore = null;
    public byte[] defReplaceAfter = null;

    /**
     * Suffixes of file-name for whom special replacing rule is defined
     */
    public String[] fileSuffixes = null;

    public byte[][] fileReplaceSrcs = null;
    public int[] fileReplaceDstSkipLens = null;
    public byte[][] fileReplaceDstPrefixes = null;
    public byte[][] fileReplaceDstSuffixes = null;
    public byte[][] fileReplaceBefores = null;
    public byte[][] fileReplaceAfters = null;

    /**
     * True when home-page is state-full for mobile devices.
     */
    public boolean isMobileHome = false;

    /**
     * Minimum size in bytes of jpeg files to be optimized.
     */
    public int jpegMin = (-1);

    /**
     * Quality of optimized jpeg files
     */
    public float jpegQuality = 1.0f;

    /**
     * When not null maps src-host to debug-host.
     */
    public HashMap<String, String> hostsMap = null;

    /**
     * When true (default is true) debug information is dumped to log.
     *
     * Contains the value of the property "debug".
     */
    public boolean isDebug = false;

    /**
     * When true (default is true) resources returning
     * Set-Cookie header are considered statefull.
     */
    public boolean isSetCookieStatefull = true;

    /**
     * Maps a single size-range to a single domain that is used by the CDN
     */
    public static class SizeRange
    {
        public int minSize; // Minimum rsrc size for serving by CDN
        public int maxSize; // Maximum rsrc size for serving by CDN
        public String cdnHost; // Domain used by CDN
        public String cdnSslHost; // Domain used by CDN for https

        public SizeRange(int minSize, int maxSize, String cdnHast, String cdnSslHost)
        {
            this.minSize = minSize;
            this.maxSize = maxSize;
            this.cdnHost = cdnHast;
            this.cdnSslHost = cdnSslHost;
        }
    }

    /**
     * Maps size-ranges to domains that are used by the CDN's
     */
    public static class SrcHostInfo
    {
        public String srcHost; // Original hosts (domains) of static-rsrcs
        public ArrayList<SizeRange> sizeRanges = new ArrayList<SizeRange>();

        public SrcHostInfo(String srcHost)
        {
            this.srcHost = srcHost;
        }
    }

    /**
     * Maps srcHost to SrcHostInfo
     */
    public HashMap<String, SrcHostInfo> cdnSrcHostInfos = new HashMap<String, SrcHostInfo>();

    public byte[] removeFromHtml; // Text to remove from html responses (when not null).
    public byte[] removeFromCss; // Text to remove from CSS responses (when not null).

    /**
     * When true (the default) HtmlSpeed invokes https requests even on localhost.
     */
    public boolean isSslLocalhost = true;

    /**
     * When true fixed max-age of cached rsrc is returned.
     */
    public boolean isFixedMaxAge = false;

    /**
     * Limits on allowed variants.
     * Can be overriden by:
     *      -Dhtmlspeed.variants=minStateLessVariant,maxStateLessVariant,minStateFullVariant,maxStateFullVariant
     */
    public int minStateLessVariant = 0;
    public int maxStateLessVariant = 4;
    public int minStateFullVariant = 0;
    public int maxStateFullVariant = 4;

    /**
     * When true (default is false) auto file-name versioning is enabled.
     */
    public boolean isVersioning = false;

    /**
     * Minimum number of max-age seconds for state-less resources
     */
    public long minStateLessMaxAge = 7*60;

    /**
     * Maximum max-age seconds for state-less resources in HtmlSpeed server (0 means unlimited)
     */
    public long maxServerSideMaxAge = 0;

     /**
     * When true <base target='_parent'> is used for content-first.
     */
    public boolean isBaseTargetParent = true;

    /**
     * Character-encoding of url's. When null ISO-8859-1 is used.
     * For Hebrew websites ISO-8859-8 is used.
     */
    public String queryEncoding = "ISO-8859-1";

    /**
     * Default value of ie.min.content.first
     */
    public static final int IE_MIN_CONTENT_FIRST_DEFAULT = 9;

    /**
     * The smallest version of IE browser for whom
     * content-first optimizations are applied.
     */
    public int ieMinContentFirst = IE_MIN_CONTENT_FIRST_DEFAULT;

    /**
     * Maximum heap-size in bytes.
     */
    private static final long MAX_HEAP = Runtime.getRuntime().maxMemory();

    /**
     * cache.rsrcsList.bufs.totalSize is not allowed bo be larger than maxCacheTotalSize.
     */
    public long maxCacheTotalSize = MAX_HEAP * 60 / 100;

    /**
     * When cache.rsrcsList.bufs.totalSize is going to be larger than maxCacheTotalSize we free
     * enough buffers so that bufs.totalSize drops to be bellow lowerCacheTotalSize.
     */
    public long lowerCacheTotalSize = MAX_HEAP * 40 / 100;

    public transient CacheStructure cache = new CacheStructure(this);

    /**
     * CONSTRUCTOR.
     */
    public void ConfigData()
    {
        clean();
    }

    /**
     * Sets new value to property stateFullPatterns
     *
     * @param stateFullPatterns new value of property
     */
    public void setStateFullPatterns(String[] stateFullPatterns)
    {
        _stateFullPatterns = stateFullPatterns;
    }

    /**
     * @param url full url of resource
     * @return true when url is state-full
     */
    public boolean isStateFull(String url)
    {
        return isSelected(url.toLowerCase(), _stateFullPatterns, true);
    }

    /**
     * Sets new value to property stateLessPatterns
     *
     * @param stateLessPatterns new value of property
     */
    public void setStateLessPatterns(String[] stateLessPatterns)
    {
        _stateLessPatterns = stateLessPatterns;
    }

    /**
     * @param url full url of resource
     * @return true when url is state-less
     */
    public boolean isStateLess(String url)
    {
        return isSelected(url.toLowerCase(), _stateLessPatterns, true);
    }

    /**
     * Sets new value to property contentFirstPatterns
     *
     * @param contentFirstPatterns new value of property
     */
    public void setContentFirstPatterns(String[] contentFirstPatterns)
    {
        _contentFirstPatterns = contentFirstPatterns;
    }

    /**
     * @param url full url of resource
     * @param wildcardsAllowed when false patterns with '*' are skipped
     * @return true when content-first-optimizations should be applied to url
     */
    public boolean isContentFirst(String url, boolean wildcardsAllowed)
    {
        return isSelected(url.toLowerCase(), _contentFirstPatterns, wildcardsAllowed);
    }

    /**
     * Sets new value to property deferPatterns
     *
     * @param deferPatterns new value of property
     */
    public void setDeferPatterns(String[] deferPatterns)
    {
        _deferPatterns = deferPatterns;
    }

    /**
     * @param url full url of resource
     * @return true when defer attribute should be applied to url (should only be used for JS rsrcs)
     */
    public boolean isDefer(String url)
    {
        return isSelected(url.toLowerCase(), _deferPatterns, true);
    }

    /**
     * Sets new value to property noInlinePatterns
     *
     * @param noInlinePatterns new value of property
     */
    public void setNoInlinePatterns(String[] noInlinePatterns)
    {
        _noInlinePatterns = noInlinePatterns;
    }

    /**
     * Resources defined as no-inline and resources defined as state-full are not inlined.
     *
     * @param url the full url of a resource
     * @return true when resource shouldn't be inlined in its container
     */
    public boolean isNoInline(String url)
    {
        return isSelected(url.toLowerCase(), _noInlinePatterns, true);
    }

    /**
     * Sets new value to property properties
     *
     * @param properties new value of property
     */
    public void setProperties(Properties properties)
    {
        _properties = properties;
    }

    /**
     * @param key property name
     * @return value of property from file properties.txt (null when not found)
     */
    public String getProperty(String key)
    {
            return _properties.getProperty(key);
    }

    public void setDefaultReplaceParams(
                                                    boolean defIsReplacing,
                                                    byte[] defReplaceSrc,
                                                    int defReplaceDstSkipLen,
                                                    byte[] defReplaceDstPrefix,
                                                    byte[] defReplaceDstSuffix,
                                                    byte[] defReplaceBefore,
                                                    byte[] defReplaceAfter)
    {
        this.defIsReplacing = defIsReplacing;
        this.defReplaceSrc = defReplaceSrc;
        this.defReplaceDstSkipLen = defReplaceDstSkipLen;
        this.defReplaceDstPrefix = defReplaceDstPrefix;
        this.defReplaceDstSuffix = defReplaceDstSuffix;
        this.defReplaceBefore = defReplaceBefore;
        this.defReplaceAfter = defReplaceAfter;
    }

    public void setFileReplaceParams (
                                                    String[] fileSuffixes,
                                                    byte[][] fileReplaceSrcs,
                                                    int[] fileReplaceDstSkipLens,
                                                    byte[][] fileReplaceDstPrefixes,
                                                    byte[][] fileReplaceDstSuffixes,
                                                    byte[][] fileReplaceBefores,
                                                    byte[][] fileReplaceAfters)
    {
        this.fileSuffixes = fileSuffixes;
        this.fileReplaceSrcs = fileReplaceSrcs;
        this.fileReplaceDstSkipLens = fileReplaceDstSkipLens;
        this.fileReplaceDstPrefixes = fileReplaceDstPrefixes;
        this.fileReplaceDstSuffixes = fileReplaceDstSuffixes;
        this.fileReplaceBefores = fileReplaceBefores;
        this.fileReplaceAfters = fileReplaceAfters;
    }

    /**
     * @param url the url of fetched resource
     * @return index of suffix of specified url in fileSuffixes (-1 when not found)
     */
    public int indexOfFileSuffixes(String url)
    {
        if (fileSuffixes == null)
            return -1;

        for (int i = 0 ; i < fileSuffixes.length ; i++)
        {
            if (url.endsWith(fileSuffixes[i]))
            {
                return i;
            }
        }
        return -1;
    }

    /**
     * Clears cdnSrcHostInfos.
     */
    public void clearCdns()
    {
        cdnSrcHostInfos.clear();
    }

    /**
     * Adds a cdn declaration from properties.txt.
     *
     * Format:   cdn-xxx  srcHost,minSize,maxSize,cdnHost   (xxx is a serial number)
     * 
     * @param srcHost domain of checked rsrc
     * @param minSize minimum size for serving by CDN
     * @param maxSize maximum size for serving by CDN
     * @param cdnHost domain used by CDN
     * @param cdnSslHost domain used by CDN for https
     */
    public void addCdn(String srcHost, int minSize, int maxSize, String cdnHost, String cdnSslHost)
    {
        SizeRange sizeRange = new SizeRange(minSize, maxSize, cdnHost, cdnSslHost);
        SrcHostInfo hostInfo = cdnSrcHostInfos.get(srcHost);
        if (hostInfo == null)
        {
            hostInfo = new SrcHostInfo(srcHost);
            cdnSrcHostInfos.put(srcHost, hostInfo);
        }
        hostInfo.sizeRanges.add(sizeRange);
    }

    /**
     * @param url the checked string
     * @param patterns the string-selection patterns
     * @param wildcardsAllowed when false patterns with '*' are skipped
     * @return true when url is selected by patterns
     */
    private boolean isSelected(String url, String[] patterns, boolean wildcardsAllowed)
    {
        if (patterns == null)
            return false;

        if (url.startsWith("http://"))
            url = url.substring(5);
        else if (url.startsWith("https://"))
            url = url.substring(6);

        for (int i = 0 ; i < patterns.length ; i += 4)
        {
            boolean isAccept = ConfigUtils.ACCEPT.equals(patterns[i]);

            if (ConfigUtils.ALL.equals(patterns[i+1]))
            {
                if (!wildcardsAllowed)
                    continue;
                return isAccept;
            }
            if (ConfigUtils.EQUALS.equals(patterns[i+1]))
            {
                if (url.equals(patterns[i+2]))
                    return isAccept;
                continue;
            }
            if (!wildcardsAllowed)
                continue;
            if (patterns[i+1] != null && !url.startsWith(patterns[i+1]))
                continue;
            if (patterns[i+2] != null)
            {
                String t = url; // text or substring of text that is checked to contain patterns[i+2]
                int first = 0; // First index of substring of text
                int last = url.length(); // Last index of substring of text
                boolean isSubString = false; // True when t is a substring of text.
                if (patterns[i+1] != null)
                {
                    isSubString = true;
                    first = patterns[i+1].length();
                }
                if (patterns[i+3] != null)
                {
                    isSubString = true;
                    last = url.length() - patterns[i+3].length();
                }
                if (isSubString)
                    t = url.substring(first, last);
                if (!t.contains(patterns[i+2]))
                    continue;
            }
            if (patterns[i+3] != null && !url.endsWith(patterns[i+3]))
                continue;

            return isAccept;
        }

        return false;
    }

    public void clean()
    {
        otherDomains = null;

        minHugeBuffer = 64*1024;
        minLargeBuffer = 32*1024;
        minMediumBuffer = 8*1024;
        minSmallBuffer = 200;

        _stateFullPatterns = ConfigUtils.DEFAULT_STATE_FULL_PATTERNS;
        _stateLessPatterns = ConfigUtils.DEFAULT_STATE_LESS_PATTERNS;
        _contentFirstPatterns = ConfigUtils.DEFAULT_CONTENT_FIRST_PATTERNS;
        _deferPatterns = ConfigUtils.DEFAULT_DEFER_PATTERNS;
        _noInlinePatterns = ConfigUtils.DEFAULT_NO_INLINE_PATTERNS;
        _properties = new Properties();

        defIsReplacing = false;
        defReplaceSrc = null;
        defReplaceDstSkipLen = 0;
        defReplaceDstPrefix = null;
        defReplaceDstSuffix = null;
        defReplaceBefore = null;
        defReplaceAfter = null;
        fileSuffixes = null;
        fileReplaceSrcs = null;
        fileReplaceDstSkipLens = null;
        fileReplaceDstPrefixes = null;
        fileReplaceDstSuffixes = null;
        fileReplaceBefores = null;
        fileReplaceAfters = null;

        isMobileHome = false;

        jpegMin = (-1);
        jpegQuality = 1.0f;

        hostsMap = null;

        isDebug = false;

        isSetCookieStatefull = true;

        cdnSrcHostInfos = new HashMap<String, SrcHostInfo>();

        removeFromHtml = null;
        removeFromCss = null;

        isSslLocalhost = true;

        isFixedMaxAge = false;

        minStateLessVariant = 0;
        maxStateLessVariant = 4;
        minStateFullVariant = 0;
        maxStateFullVariant = 4;

        isVersioning = false;

        minStateLessMaxAge = 7*60;
        maxServerSideMaxAge = 0;

        isBaseTargetParent = true;

        queryEncoding = "ISO-8859-1";

        ieMinContentFirst = IE_MIN_CONTENT_FIRST_DEFAULT;

        maxCacheTotalSize = MAX_HEAP * 60 / 100;
        lowerCacheTotalSize = MAX_HEAP * 40 / 100;
    }

}
