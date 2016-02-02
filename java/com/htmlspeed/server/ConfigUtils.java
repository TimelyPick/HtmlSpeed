/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 12 April 2012
 */
package com.htmlspeed.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Scanner;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import javax.crypto.Cipher;
import javax.servlet.ServletException;

/**
 * ConfigUtils
 *
 * Utilities regarding configuration-files.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.49 $$Date: 2013/08/28 12:10:35 $
 */
public class ConfigUtils
{

    /**
     * Directory in which the file license.dat is stored.
     * CONFIG_DIR contains the other configuration-files when not in testing environment.
     */
    private static final String CONFIG_DIR = System.getProperty("jetty.home") + File.separator + "htmlspeed";

    /**
     * Route to current write-to config-directory
     */
    private static int[] _configDirs = new int[3];

    /**
     * Read-Write lock used for synchronizing access to configuration-info.
     *
     * Locked for write by the single instance of ConfigUtils.ConfigBackThread when loading modified configuration-files.
     * Locked for read by servicing threads.
     * Locked for read by the single instance of ServiceUtils.BackThread.
     */
    public static final ReentrantReadWriteLock configLock = new ReentrantReadWriteLock(true);

    /**
     * Licensed domains (allowed to be boosted by htmlspeed).
     *
     * The value ".xxx" stands for any subdomain of "xxx" (including "www.xxx" but excluding "xxx" itself).
     * The domains "xxx" and "www.xxx" are separately licensed when both are required.
     */
    public static HashSet<String> licensedDomains;

    /**
     * True in testing environment.
     *
     * Set by the method loadLicense when server is licensed to galiel314.
     */
    public static boolean isGaliel = false;

    /**
     * License expiration-time (relevant only when isGaliel is true).
     */
    public static long galielExpirationTime;

    private static HtmlSpeedServlet _servlet;

    /**
     * Full path to license file.
     */
    private static final String LICENSE_FILE_PATH = CONFIG_DIR + File.separator + "license.dat";

    /**
     * Last modification time of license.dat
     */
    private static long _licenseLastModified = 0;

    /**
     * Full path of hostinfo configuration-file.
     */
    private static final String HOST_INFO_FILE_PATH = CONFIG_DIR + File.separator + "hostinfo.txt";

    /**
     * When true then hostInfo can't be modified by using the /htmlspeed url
     */
    public static boolean hostInfoFileExists;

    /**
     * Content of hostinfo.txt configuration-file.
     *      [withfirstplus/]domain,...,domain,address[-weight],...,address[-weight],...[/]
     */
    private static String _hostInfo;

    /**
     * Last modification time of hostinfo.txt
     */
    private static long _hostInfoLastModified = 0;

    /**
     * Selects any uri/path
     */
    public static final String ALL = "ALL";

    /**
     * Template command to accept selected uri/path
     */
    public static final String ACCEPT = "ACCEPT";

    /**
     * Template command to reject selected uri/path
     */
    public static final String REJECT = "REJECT";

    /**
     * Used as the "starts-with" in patterns for selecting the constant value in "contains".
     * We use a character that is invalid in a URL
     */
    public static final String EQUALS = "\n";

    /**
     * Used as default value of _stateFullPatterns (see bellow).
     */
    public static final String[] DEFAULT_STATE_FULL_PATTERNS = {};

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
    private static String[] _stateFullPatterns = DEFAULT_STATE_FULL_PATTERNS;

    private static String _stateFullFileName;
    private static String _stateFullFilePath;
    private static int _stateFullVersion = 0;

    /**
     * Last modification time of state-full.txt
     */
    private static long _stateFullLastModified = 0;

    /**
     * Used as default value of _stateLessPatterns (see bellow).
     */
    public static final String[] DEFAULT_STATE_LESS_PATTERNS = {};

    /**
     * Used for forcing selected uri's/paths to be state-full.
     *
     * Htmlspeed automaticaly calssifies each uri/path as state-full
     * or state-less depending on caching http-headers. The state-
     * full patterns enables overriding the default behaviour.
     *
     */
    private static String[] _stateLessPatterns = DEFAULT_STATE_LESS_PATTERNS;

    private static String _stateLessFileName;
    private static String _stateLessFilePath;
    private static int _stateLessVersion = 0;

    /**
     * Last modification time of state-less.txt
     */
    private static long _stateLessLastModified = 0;

    /**
     * Used as default value of _contentFirstPatterns (see bellow).
     */
    public static final String[] DEFAULT_CONTENT_FIRST_PATTERNS = {};

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
    public static String[] _contentFirstPatterns = DEFAULT_CONTENT_FIRST_PATTERNS;

    private static String _contentFirstFileName;
    private static String _contentFirstFilePath;
    private static int _contentFirstVersion = 0;

    /**
     * Last modification time of content-first.txt
     */
    private static long _contentFirstLastModified = 0;

    /**
     * Used as default value of _deferPatterns (see bellow).
     */
    public static final String[] DEFAULT_DEFER_PATTERNS = {};

    private static String[] _deferPatterns = DEFAULT_DEFER_PATTERNS;

    private static String _deferFileName;
    private static String _deferFilePath;
    private static int _deferVersion = 0;

    /**
     * Last modification time of defer.txt
     */
    private static long _deferLastModified = 0;

    /**
     * Used as default value of _noInlinePatterns (see bellow).
     */
    public static final String[] DEFAULT_NO_INLINE_PATTERNS ={REJECT, ALL, null, null,};

    /**
     * Used for selecting resources that are not inlined in their container.
     * Mainly used when only a small number of images in a stylesheet are needed by a single-page.
     */
    private static String[] _noInlinePatterns = DEFAULT_NO_INLINE_PATTERNS;

    private static String _noInlineFileName;
    private static String _noInlineFilePath;
    private static int _noInlineVersion = 0;

    /**
     * Last modification time of no-inline.txt
     */
    private static long _noInlineLastModified = 0; 

    private static String _autoRefreshedFileName;
    private static String _autoRefreshedFilePath;

    /**
     * Last modification time of auto-refreshed.txt
     */
    private static long _autoRefreshedLastModified = 0; 

    private static String _propertiesFileName;
    private static String _propertiesFilePath;

    /**
     * Content of properties.txt configuration-file.
     */
    private static String _propertiesText = "";

    /**
     * Contains all configuration properties loaded from file properties.txt.
     */
    private static Properties _properties = new Properties();

    /**
     * Last modification time of properties.txt
     */
    private static long _propertiesLastModified = 0;

    /**
     * Timer used for periodically-refreshing important state-less pages
     * and for loading specified state-full pages once. 
     */
    private static Timer _refreshTimer;

    /**
     * Hashed list of URI's of all pages that are automatically refreshed by _refreshTimer
     */
    private static HashSet<String> _autoRefreshedPagesSet = new HashSet<String>();

    /**
     * List of URI's of all pages that are automatically refreshed by _refreshTimer
     */
    private static ArrayList<String> _autoRefreshedPages = new ArrayList<String>();

    /**
     * Thread-pool, used for refreshing pages.
     */
    private static ExecutorService _pool = Executors.newFixedThreadPool(5);

    /**
     * The single instance of ConfigBackThread.
     * Created by method startConfigBackThread.
     */
    private static BackThread _configBackThread = null;

    /**
     * Used for all domains when not in CDN (isGaliel == false).
     */
    private static ConfigData _configData = new ConfigData();

    /**
     * When used then HtmlSpeed server acts as a router.
     *
     * Used when isGaliel is true and no configuration is defined for the requested domain.
     */
    public static ConfigData routerConfig = new ConfigData();

    /**
     * Maps domains to their configurations (used when isGaliel).
     */
    private static HashMap<String, ConfigData> _configs;

    /**
     * Sets value to property servlet.
     * @param servlet new value of property
     */
    public static void setServlet(HtmlSpeedServlet servlet)
    {
        _servlet = servlet;
    }

    /**
     * @param host of http request
     * @return the configuration for host
     */
    public static ConfigData getConfigData(String host)
    {
        if (!isGaliel)
        {
            return _configData;
        }

        ConfigData configData = _configs.get(host);

        if (configData != null)
        {
            return configData;
        }
        return routerConfig;
    }

    /**
     * Starts the single instance of ConfigBackThread.
     *
     * Periodicaly checks if any of the configuration-files
     * have changed and loads the changed configuration-files.
     */
    public static synchronized void startConfigBackThread()
    {
        if (_configBackThread != null)
            return;

        _configBackThread = new BackThread("HtmlSpeedConfigUtils");
        _configBackThread.start();
    }

    /**
     * Loads file license.dat from CONFIG_DIR.
     */
    public static void loadLicense()
    {
        byte[] encLicense = new byte[256];
        String license = null;

        try
        {
            // Reading license file into encLicense:
            FileInputStream fis = new FileInputStream(LICENSE_FILE_PATH);
            fis.read(encLicense);
            fis.close();

            RSAPublicKey pubKey = getPublicKey();

            // Assigning the decoded license to license:
            Cipher cipher = Cipher.getInstance("RSA");
            cipher.init(Cipher.DECRYPT_MODE, pubKey);
            byte[] decLicenseBytes = cipher.doFinal(encLicense);
            license = new String(decLicenseBytes);
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            return;
        }

        if (license == null)
            return;

        _licenseLastModified = new File(LICENSE_FILE_PATH).lastModified();

        // Analyzing the license:
        String galiel = "galiel314.com ";
        if (license.startsWith(galiel))
        {
            // Super license:
            String dateStr = license.substring(galiel.length(), galiel.length() + DateUtils.PATTERN_RFC1123.length());
            try
            {
                Date date = DateUtils.parseDate(dateStr);
                galielExpirationTime = 1000 * 3600 * 24 * 14 + date.getTime(); // Valid for 2 weeks from issue time.
                isGaliel = true;
                return; // licensedDomains will be initialized by ServiceUtils.setHostInfo.
            }
            catch (IllegalArgumentException exc)
            {
                exc.printStackTrace();
                return;
            }
        }

        String[] domains = license.split(",");
        licensedDomains = new HashSet<String>(domains.length);
        for (String d : domains)
            licensedDomains.add(d);
    }

    /**
     * @return used public key.
     * @throws IOException
     * @throws ClassNotFoundException 
     */
    public static RSAPublicKey getPublicKey() throws IOException, ClassNotFoundException
    {
        // Deserializing the public-key:
        byte[] serPublicKey = new byte[SERIALIZED_PUBLIC_KEY1.length +
                                                        SERIALIZED_PUBLIC_KEY2.length +
                                                        SERIALIZED_PUBLIC_KEY3.length];
        System.arraycopy(
                        SERIALIZED_PUBLIC_KEY1,
                        0,
                        serPublicKey,
                        0,
                        SERIALIZED_PUBLIC_KEY1.length);
        System.arraycopy(
                        SERIALIZED_PUBLIC_KEY2,
                        0,
                        serPublicKey,
                        SERIALIZED_PUBLIC_KEY1.length,
                        SERIALIZED_PUBLIC_KEY2.length);
        System.arraycopy(
                        SERIALIZED_PUBLIC_KEY3,
                        0,
                        serPublicKey,
                        SERIALIZED_PUBLIC_KEY1.length + SERIALIZED_PUBLIC_KEY2.length,
                        SERIALIZED_PUBLIC_KEY3.length);
        ByteArrayInputStream bais = new ByteArrayInputStream(serPublicKey);
        ObjectInputStream ois = new ObjectInputStream(bais);
        RSAPublicKey pubKey = (RSAPublicKey) ois.readObject();
        ois.close();
        bais.close();
        return pubKey;
    }

    /**
     * Loads other-domains from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return assigns other-domains to configData.otherDomains
     */
    public static void loadOtherDomains(String dirName, String fileName, ConfigData configData)
    {
        try
        {
            String filePath = dirName + fileName;

            File file = new File(filePath);
            if (!file.exists())
            {
                return;
            }

            ArrayList<String> otherDomains = new ArrayList<String>();
            FileInputStream fis = new FileInputStream(filePath);
            Scanner scan = new Scanner(fis);
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                if (otherDomains == null)
                    otherDomains = new ArrayList<String>();
                otherDomains.add(line);
            }
            scan.close();
            fis.close();

            if (otherDomains != null)
            {
                configData.otherDomains = otherDomains.toArray(new String[otherDomains.size()]);
            }
        }
        catch(Exception e)
        {
        }
    }

    /**
     * Loads the hostinfo.txt configuration file
     *
     * @return /htmlspeed/[withfirstplus/]domain,...,domain,addr,...,addr, ...[/]
     */
    public static String loadHostInfo() throws ServletException
    {
        try
        {
            File file = new File(HOST_INFO_FILE_PATH);
            if (!file.exists())
            {
                hostInfoFileExists = false;
                _hostInfo = null;
                _hostInfoLastModified = 0;
                return HOST_INFO_FILE_PATH + " doesn't exist";
            }

            StringBuilder ipsBuilder = new StringBuilder(512);
            FileInputStream fis = new FileInputStream(HOST_INFO_FILE_PATH);
            Scanner scan = new Scanner(fis);
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                ipsBuilder.append(line);
            }
            scan.close();
            fis.close();
            _hostInfo = ipsBuilder.toString();
            _hostInfoLastModified = new File(HOST_INFO_FILE_PATH).lastModified();

            String hostInfo = "/htmlspeed/" + _hostInfo;
            _servlet.initHostInfo(hostInfo);
            hostInfoFileExists = true;
            return hostInfo;
        }
        catch (Exception exc)
        {
            hostInfoFileExists = false;
            _hostInfo = null;
            _hostInfoLastModified = 0;
            throw new ServletException(exc);
        }
    }

    /**
     * Loads state-full patterns from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error-msg to be displayed by browser
     */
    public static String loadStateFullPatterns(String dirName, String fileName, ConfigData configData)
    {
        String filePath = dirName + fileName;
        _stateFullFileName = fileName;
        _stateFullFilePath = filePath;
        _stateFullLastModified = 0;
        _stateFullPatterns = DEFAULT_STATE_FULL_PATTERNS;

        PatternsInfo patternsInfo = loadPatterns(filePath, "state-full");
        if (patternsInfo.patterns != null)
        {
            _stateFullPatterns = patternsInfo.patterns;
            _stateFullVersion = patternsInfo.version;
            _stateFullLastModified = new File(filePath).lastModified();
        }
        configData.setStateFullPatterns(_stateFullPatterns);
        return patternsInfo.html.toString();
    }

    /**
     * @return html markup describing currently loaded statefull patterns.
     */
    public static String dumpStateFullPatterns()
    {
        return dumpPatterns(_stateFullPatterns, "state-full", _stateFullFilePath, _stateFullVersion).toString();
    }

    /**
     * Loads state-less patterns from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error-msg to be displayed by browser
     */
    public static String loadStateLessPatterns(String dirName, String fileName, ConfigData configData)
    {
        String filePath = dirName + fileName;
        _stateLessFileName = fileName;
        _stateLessFilePath = filePath;
        _stateLessLastModified = 0;
        _stateLessPatterns = DEFAULT_STATE_LESS_PATTERNS;

        PatternsInfo patternsInfo = loadPatterns(filePath, "state-less");
        if (patternsInfo.patterns != null)
        {
            _stateLessPatterns = patternsInfo.patterns;
            _stateLessVersion = patternsInfo.version;
            _stateLessLastModified = new File(filePath).lastModified();
        }
        configData.setStateLessPatterns(_stateLessPatterns);
        return patternsInfo.html.toString();
    }

    /**
     * @return html markup describing currently loaded state-less patterns.
     */
    public static String dumpStateLessPatterns()
    {
        StringBuilder html = dumpPatterns(_stateLessPatterns, "state-less", _stateLessFilePath, _stateLessVersion);
        return html.toString();
    }

    /**
     * Loads content-first-patterns from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error-msg to be displayed by browser
     */
    public static String loadContentFirstPatterns(String dirName, String fileName, ConfigData configData)
    {
        String filePath = dirName + fileName;
        _contentFirstFileName = fileName;
        _contentFirstFilePath = filePath;
        _contentFirstLastModified = 0;
        _contentFirstPatterns = DEFAULT_CONTENT_FIRST_PATTERNS;

        PatternsInfo patternsInfo = loadPatterns(filePath, "content-first");
        if (patternsInfo.patterns != null)
        {
            _contentFirstPatterns = patternsInfo.patterns;
            _contentFirstVersion = patternsInfo.version;
            _contentFirstLastModified = new File(filePath).lastModified();
        }
        configData.setContentFirstPatterns(_contentFirstPatterns);
        return patternsInfo.html.toString();
    }

    /**
     * @return html markup describing currently loaded content-first patterns.
     */
    public static String dumpContentFirstPatterns()
    {
        return dumpPatterns(_contentFirstPatterns, "content-first", _contentFirstFilePath, _contentFirstVersion).toString();
    }

    /**
     * Loads defer-patterns from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error-msg to be displayed by browser
     */
    public static String loadDeferPatterns(String dirName, String fileName, ConfigData configData)
    {
        String filePath = dirName + fileName;
        _deferFileName = fileName;
        _deferFilePath = filePath;
        _deferLastModified = 0;
        _deferPatterns = DEFAULT_DEFER_PATTERNS;

        PatternsInfo patternsInfo = loadPatterns(filePath, "defer");
        if (patternsInfo.patterns != null)
        {
            _deferPatterns = patternsInfo.patterns;
            _deferVersion = patternsInfo.version;
            _deferLastModified = new File(filePath).lastModified();
        }
        configData.setDeferPatterns(_deferPatterns);
        return patternsInfo.html.toString();
    }

    /**
     * @return html markup describing currently loaded defer patterns.
     */
    public static String dumpDeferPatterns()
    {
        return dumpPatterns(_deferPatterns, "defer", _deferFilePath, _deferVersion).toString();
    }

    /**
     * Loads no-inline-patterns from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error-msg to be displayed by browser
     */
    public static String loadNoInlinePatterns(String dirName, String fileName, ConfigData configData)
    {
        String filePath = dirName + fileName;
        _noInlineFileName = fileName;
        _noInlineFilePath = filePath;
        _noInlineLastModified = 0;
        _noInlinePatterns = DEFAULT_NO_INLINE_PATTERNS;

        PatternsInfo patternsInfo = loadPatterns(filePath, "no-inline");
        if (patternsInfo.patterns != null)
        {
            _noInlinePatterns = patternsInfo.patterns;
            _noInlineVersion = patternsInfo.version;
            _noInlineLastModified = new File(filePath).lastModified();
        }
        configData.setNoInlinePatterns(_noInlinePatterns);
        return patternsInfo.html.toString();
    }

    /**
     * @return html markup describing currently loaded no-inline patterns.
     */
    public static String dumpNoInlinePatterns()
    {
        return dumpPatterns(_noInlinePatterns, "no-inline", _noInlineFilePath, _noInlineVersion).toString();
    }

    /**
     * Loads properties from fileName
     *
     * @param dirName name of directory containing fileName
     * @param fileName name of file containing the state-full patterns
     * @param configData used configuration
     * @return file-content or error message
     */
    public static String loadProperties(String dirName, String fileName, ConfigData configData) throws ServletException
    {
        String filePath = dirName + fileName;
        _propertiesFileName = fileName;
        _propertiesFilePath = filePath;

        _properties.clear();
        _propertiesText = "";
        _propertiesLastModified = 0;
        configData.setProperties(_properties);

        File file = new File(filePath);
        if (!file.exists())
        {
            _propertiesText = "File " + filePath + " doesn't exist<br/>";
            handleDomainConfigProperties(configData);
            if (configData == _configData)
                _servlet.handleServerConfigProperties(configData);
            return _propertiesText;
        }
        if (!file.canRead())
        {
            _propertiesText = "File " + filePath + " is not readable<br/>";
            handleDomainConfigProperties(configData);
            if (configData == _configData)
                _servlet.handleServerConfigProperties(configData);
            return _propertiesText;
        }

        // Loading filePath into _properties (as properties) and into _propertiesText as text:
        FileInputStream is = null;
        try
        {
            // Loading properties in filePath into _properties:
            is = new FileInputStream(filePath);
            _properties.load(is);
            is.close();

            // Loading content of filePath into _propertiesText:
            Scanner scan = null;
            try
            {
                scan = new Scanner(file);
            }
            catch (FileNotFoundException exc)
            {
                // Will not happen.
            }
            StringBuilder sb = new StringBuilder(512);
            sb.append("Current properties loaded from file: ").append(filePath).append(":<br/>");
            while (scan.hasNextLine())
            {
                String line = scan.nextLine();
                sb.append(line).append("<br/>");
            }
            scan.close();
            _propertiesText = sb.toString();
            _propertiesLastModified = new File(filePath).lastModified();
        }
        catch (IOException exc)
        {
            _properties.clear();
            _propertiesText = exc.getMessage();
            try { if (is != null) is.close(); }catch (IOException e){}
        }

        handleDomainConfigProperties(configData);
        if (configData == _configData)
            _servlet.handleServerConfigProperties(configData);
        return _propertiesText;
    }

    /**
     * @return html markup describing currently loaded properties.
     */
    public static String dumpProperties()
    {
            return _propertiesText;
    }

    /**
     * Loads the list of auto-refreshed-pages from file fileName.
     *
     * Each line in the file contains:
     *      dddd full-page-url         (dddd: period in seconds between refresh invocations).
     *
     * For each listed full-page-url creates a RefreshTimerTask instance to
     * be executed once if dddd equals 0 or periodically each dddd seconds.
     *
     * @param fileName name of file containing the list of auto-refreshed pages
     */
    public static void loadAutoRefreshed(String fileName)
    {
        String filePath = CONFIG_DIR + fileName;
        _autoRefreshedFileName = fileName;
        _autoRefreshedFilePath = filePath;
        _autoRefreshedLastModified = 0;

        _autoRefreshedPagesSet.clear();
        _autoRefreshedPages.clear();

        File file = new File(filePath);
        if (!file.exists() || !file.canRead())
            return;

        if (_refreshTimer != null)
        {
            _refreshTimer.cancel();
            _refreshTimer.purge();
        }
        _refreshTimer = new Timer("RefreshTimer", true);

        _autoRefreshedLastModified = new File(filePath).lastModified();

        Scanner scan = null;
        try
        {
            scan = new Scanner(file);
        }
        catch (FileNotFoundException exc)
        {
            // Will not happen.
        }

        long firstTime = 1000; // Delay before first execution of load of next listed page.

        while (scan.hasNextLine())
        {
            String line = scan.nextLine();
            char ch = line.charAt(0);

            if (!Character.isDigit(ch))
                continue; // Line doesn't begin with a digit (assumed to be a comment)

            int lastDigit = 1; // Index of first non-digit character.

            int lineLen = line.length();
            while (lastDigit < lineLen && Character.isDigit(ch = line.charAt(lastDigit)))
                ++lastDigit;

            if (lastDigit >= lineLen)
                continue; // Line contains only digits

            int httpIndex = line.indexOf("http", lastDigit);
            if (httpIndex < 0)
                continue; // Line doesn't contain a full url

            long period = Long.parseLong(line.substring(0, lastDigit));
            String fullUrl = line.substring(httpIndex);

            RefreshTimerTask task = new RefreshTimerTask(fullUrl, _pool);
            if (period == 0)
                _refreshTimer.schedule(task, firstTime);
            else
                _refreshTimer.schedule(task, firstTime, period*1000);
            firstTime += 1000;

            _autoRefreshedPagesSet.add(fullUrl);
            _autoRefreshedPages.add(fullUrl);
        }

        scan.close();
    }

    public static String dumpAutoRefreshed()
    {
        StringBuilder html = new StringBuilder(1028*8);
        html.append("Current auto-refreshed pages loaded from file: ").append(_autoRefreshedFilePath).append(":<br/>");

        for (String line : _autoRefreshedPages)
        {
            html.append(line);
            html.append("<br/>");
        }
        return html.toString();
    }

    /**
     * @param uri of a state-less page-rsrc
     * @return true when uri is automaticlly refreshed
     */
    public static boolean isAutoRefreshedPage(String uri)
    {
        return _autoRefreshedPagesSet.contains(uri);
    }

    /**
     * Destroy instances of RefreshTimerTask instances.
     * Called by HtmlSpeedServlet.destroy() and when file auto-refreshed.txt is modified.
     */
    public static void destroyRefreshTimerTasks()
    {
        if (_refreshTimer != null)
        {
            _refreshTimer.cancel();
            _refreshTimer.purge();
            _refreshTimer = null;
        }
    }

    private static StringBuilder dumpPatterns(String[] patterns, String kind, String filePath, int version)
    {
        StringBuilder html = new StringBuilder(1028*8);
        html.append("Current ").append(kind).append(" patterns loaded from file: ").append(filePath).append(":<br/>");
        html.append(version).append("<br/>");

        if (patterns == null)
            return html;

        for (int i = 0 ; i < patterns.length ; i += 4)
        {
            if (patterns[i] != null && patterns[i].equals(REJECT))
                html.append('!');

            if (patterns[i+1] != null && patterns[i+1].equals(ALL))
            {
                html.append('*');
            }
            else if(patterns[i+1] != null && patterns[i + 1].equals(EQUALS))
            {
                html.append(patterns[i+2]);
            }
            else
            {
                if (patterns[i + 1] != null)
                    html.append(patterns[i + 1]).append('*');

                if (patterns[i + 2] != null)
                {
                    if (patterns[i + 1] == null)
                        html.append('*');
                    html.append(patterns[i + 2]).append('*');
                }

                if (patterns[i + 3] != null)
                {
                    if (patterns[i + 1] == null && patterns[i + 2] == null)
                        html.append('*');
                    html.append(patterns[i + 3]);
                }
            }

            html.append("<br/>");
        }

        return html;
    }

    /**
     * Patterns loaded from config-file by the method loadPatterns
     */
    private static class PatternsInfo
    {
        /**
         * Each pattern is described by 4 adjescent entries in the
         * array of strings. Null when html contains an error-msg.
         */
        public String[] patterns = null;

        /**
         * Html markup describing the content of read file,
         * or an error-msg when patterns is null.
         */
        public StringBuilder html = new StringBuilder(1028*8);

        /**
         * Version of loaded config-file
         */
        public int version;
    }

    /**
     * Loads all patterns from the specified configuration-file.
     *
     * Note: fileName must contain kind as a substring.
     *
     * @param filePath the full name (including path) of the configuration-file
     * @param kind the purpose of the config-file (example: "content-first")
     * @return pattersInfo loaded from config-file (patternsInfo.patterns is null when html contains an error-msg).
     */
    private static PatternsInfo loadPatterns(String filePath, String kind)
    {
        PatternsInfo patternsInfo = new PatternsInfo();

        if (!filePath.toLowerCase().contains(kind))
        {
            patternsInfo.html.append("ERROR: invalid configuration file-name: '', (file-name should contain '").
                        append(kind).append("') !!!");
            return patternsInfo;
        }

        File file = new File(filePath);
        if (!file.exists())
        {
            patternsInfo.html.append("File ").append(filePath).append(" does not exist.");
            return patternsInfo;
        }
        else if (!file.canRead())
        {
            patternsInfo.html.append("ERROR: Can't read existing file: ").append(filePath);
            return patternsInfo;
        }

        Scanner scan = null;
        try
        {
            scan = new Scanner(file);
        }
        catch (FileNotFoundException exc)
        {
            // Will not happen.
        }

        // Verifying that first-line is a positive integer (version number of file):
        int version = (-1);
        if (!scan.hasNextLine())
        {
            patternsInfo.html.append("ERROR: configuration file: ").append(filePath).append(" is empty");
            return patternsInfo;
        }

        String firstLine = scan.nextLine();
        try
        {
            version = Integer.parseInt(firstLine);
            if (version <= 0)
                throw new NumberFormatException();
            patternsInfo.version = version;

            patternsInfo.html.append("Loaded ").append(kind).append("-patterns configuration-file: ").
                        append(filePath).append(":<br/>");
            patternsInfo.html.append(firstLine).append("<br/>");
        }
        catch (NumberFormatException exc)
        {
            patternsInfo.html.append("ERROR: first line in confuguration file: ").
                                            append(filePath).append(" is not a positive integer");
            return patternsInfo;
        }

        ArrayList<String> patterns = new ArrayList<String>();

        while(scan.hasNextLine())
        {
            boolean isReject = false;

            String text = scan.nextLine().toLowerCase();

            // Removing http:  and https: because they don't effect selection:
            text = text.replace("http://", "//");
            text = text.replace("https://", "//");

            if (text.startsWith("!"))
            {
                isReject = true;
                patterns.add(REJECT);
                text = text.substring(1);
            }
            else
            {
                patterns.add(ACCEPT);
            }

            if (text.equals("*"))
            {
                patterns.add(ALL);
                patterns.add(null);
                patterns.add(null);
            }
            else if(!text.contains("*"))
            {
                // Fixed string:
                patterns.add(EQUALS);
                patterns.add(text);
                patterns.add(null);
            }
            else
            {
                int firstAsterix = text.indexOf('*');
                int secondAsterix = text.indexOf('*', firstAsterix + 1);

                if (firstAsterix > 0)
                    patterns.add(text.substring(0, firstAsterix)); // starts-with
                else // firstAsterix == 0:
                    patterns.add(null);

                if (firstAsterix + 1 < secondAsterix)
                    patterns.add(text.substring(firstAsterix + 1, secondAsterix)); // contains
                else
                    patterns.add(null);

                int maxAsterix = Math.max(firstAsterix, secondAsterix);
                if (maxAsterix < text.length() - 1)
                    patterns.add(text.substring(maxAsterix + 1)); // ends-with
                else
                    patterns.add(null);
            }
            patternsInfo.html.append(isReject ? "!" : "").append(text).append("<br/>");
        }

        scan.close();

        patternsInfo.patterns = patterns.toArray(new String[patterns.size()]);
        return patternsInfo;
    }

    /**
     * Handles change of properties of a specific domain
     *
     * @param configData configuration of domain
     */
    private static void handleDomainConfigProperties(ConfigData configData)
    {
        int filesCount = 0;
        for (int i = 1 ; i < 1000 ; i++)
        {
            if (configData.getProperty("file" + Integer.toString(i) + ".suffix") != null)
                filesCount = i;
            else
                break;
        }
        configData.setFileReplaceParams(null, null, null, null, null, null, null);
        if (filesCount > 0)
        {
            String[] fileSuffixes = new String[filesCount];
            byte[][] fileReplaceSrcs = new byte[filesCount][];
            int[] fileReplaceDstSkipLens = new int[filesCount];
            byte[][] fileReplaceDstPrefixes = new byte[filesCount][];
            byte[][] fileReplaceDstSuffixes = new byte[filesCount][];
            byte[][] fileReplaceBefores = new byte[filesCount][];
            byte[][] fileReplaceAfters = new byte[filesCount][];
            for (int i = 0 ; i < filesCount ; i++)
            {
                String fileSuffix = configData.getProperty("file" + Integer.toString(i+1)  +".suffix");
                if (fileSuffix != null)
                    fileSuffixes[i] = fileSuffix;

                String fileReplaceSrc = configData.getProperty("file" + Integer.toString(i+1)  +".replace.src");
                if (fileReplaceSrc != null)
                {
                    fileReplaceSrc = fileReplaceSrc.replace("&lt;", "<");
                    fileReplaceSrc = fileReplaceSrc.replace("&gt;", ">");
                    fileReplaceSrcs[i] = fileReplaceSrc.getBytes();
                }

                String fileReplaceDstSkip = configData.getProperty("file" + Integer.toString(i+1)  +".replace.dst.skip");
                if (fileReplaceDstSkip != null)
                {
                    try { fileReplaceDstSkipLens[i] = Integer.parseInt(fileReplaceDstSkip); } catch (Exception e){};
                }

                String fileReplaceDstPrefix = configData.getProperty("file" + Integer.toString(i+1)  +".replace.dst.prefix");
                if (fileReplaceDstPrefix != null)
                {
                    fileReplaceDstPrefix = fileReplaceDstPrefix.replace("&lt;", "<");
                    fileReplaceDstPrefix = fileReplaceDstPrefix.replace("&gt;", ">");
                    fileReplaceDstPrefixes[i] = fileReplaceDstPrefix.getBytes();
                }

                String fileReplaceDstSuffix = configData.getProperty("file" + Integer.toString(i+1)  +".replace.dst.suffix");
                if (fileReplaceDstSuffix != null)
                {
                    fileReplaceDstSuffix = fileReplaceDstSuffix.replace("&lt;", "<");
                    fileReplaceDstSuffix = fileReplaceDstSuffix.replace("&gt;", ">");
                    fileReplaceDstSuffixes[i] = fileReplaceDstSuffix.getBytes();
                }

                String fileReplaceBefore = configData.getProperty("file" + Integer.toString(i+1)  +".replace.before");
                if (fileReplaceBefore != null)
                {
                    fileReplaceBefore = fileReplaceBefore.replace("&lt;", "<");
                    fileReplaceBefore = fileReplaceBefore.replace("&gt;", ">");
                    fileReplaceBefores[i] = fileReplaceBefore.getBytes();
                }

                String fileReplaceAfter = configData.getProperty("file" + Integer.toString(i+1)  +".replace.after");
                if (fileReplaceAfter != null)
                {
                    fileReplaceAfter = fileReplaceAfter.replace("&lt;", "<");
                    fileReplaceAfter = fileReplaceAfter.replace("&gt;", ">");
                    fileReplaceAfters[i] = fileReplaceAfter.getBytes();
                }
            }
            configData.setFileReplaceParams(
                                                    fileSuffixes,
                                                    fileReplaceSrcs,
                                                    fileReplaceDstSkipLens,
                                                    fileReplaceDstPrefixes,
                                                    fileReplaceDstSuffixes,
                                                    fileReplaceBefores,
                                                    fileReplaceAfters);
        }

        String replaceSrc = configData.getProperty("replace.src");
        byte[] defReplaceSrc = null;
        if (replaceSrc != null)
        {
            replaceSrc = replaceSrc.replace("&lt;", "<");
            replaceSrc = replaceSrc.replace("&gt;", ">");
            defReplaceSrc = replaceSrc.getBytes();
        }

        int defReplaceDstSkipLen = 0;
        String replaceDstSkipLen = configData.getProperty("replace.dst.skip");
        if (replaceDstSkipLen != null)
        {
            try { defReplaceDstSkipLen = Integer.parseInt(replaceDstSkipLen); } catch (Exception e){}
        }

        String replaceDstPrefix = configData.getProperty("replace.dst.prefix");
        byte[] defReplaceDstPrefix = null;
        if (replaceDstPrefix != null)
        {
            replaceDstPrefix = replaceDstPrefix.replace("&lt;", "<");
            replaceDstPrefix = replaceDstPrefix.replace("&gt;", ">");
            defReplaceDstPrefix = replaceDstPrefix.getBytes();
        }

        String replaceDstSuffix = configData.getProperty("replace.dst.suffix");
        byte[] defReplaceDstSuffix = null;
        if (replaceDstSuffix != null)
        {
            replaceDstSuffix = replaceDstSuffix.replace("&lt;", "<");
            replaceDstSuffix = replaceDstSuffix.replace("&gt;", ">");
            defReplaceDstSuffix = replaceDstSuffix.getBytes();
        }

        String replaceBefore = configData.getProperty("replace.before");
        byte[] defReplaceBefore = null;
        if (replaceBefore != null)
        {
            replaceBefore = replaceBefore.replace("&lt;", "<");
            replaceBefore = replaceBefore.replace("&gt;", ">");
            defReplaceBefore = replaceBefore.getBytes();
        }

        String replaceAfter = configData.getProperty("replace.after");
        byte[] defReplaceAfter = null;
        if (replaceAfter != null)
        {
            replaceAfter = replaceAfter.replace("&lt;", "<");
            replaceAfter = replaceAfter.replace("&gt;", ">");
            defReplaceAfter = replaceAfter.getBytes();
        }

        boolean defIsReplacing = defReplaceSrc != null;

        configData.setDefaultReplaceParams(
                                                    defIsReplacing,
                                                    defReplaceSrc,
                                                    defReplaceDstSkipLen,
                                                    defReplaceDstPrefix,
                                                    defReplaceDstSuffix,
                                                    defReplaceBefore,
                                                    defReplaceAfter);

        String isMobileHome = configData.getProperty("mobile.home");
        configData.isMobileHome = "true".equalsIgnoreCase(isMobileHome); // Default is "false".

        configData.jpegMin = (-1);
        String jpegMin = configData.getProperty("jpeg.min"); // Minimum size of optimized jpeg in Kbytes
        if (jpegMin != null)
        {
            try
            {
                configData.jpegMin = Integer.parseInt(jpegMin) * 1024;

                String jpegQuality = configData.getProperty("jpeg.quality");
                if (jpegQuality == null)
                {
                    configData.jpegMin = (-1);
                    System.out.println("Missing property: jpeg.quality");
                }
                else
                {
                    try
                    {
                        configData.jpegQuality = Float.parseFloat(jpegQuality);

                        if (configData.jpegQuality < 0.0f || configData.jpegQuality > 1.0f)
                        {
                            configData.jpegMin = (-1);
                            System.out.println("Illegal property: jpeg.quality " + jpegQuality);
                        }
                    }
                    catch (NumberFormatException exc)
                    {
                        configData.jpegMin = (-1);
                        System.out.println("Illegal property: jpeg.quality " + jpegQuality);
                    }
                }
            }
            catch (NumberFormatException exc)
            {
                configData.jpegMin = (-1);
                System.out.println("Illegal property: jpeg.min " + jpegMin);
            }
        }

        // A comma separated list: srcHost1,dstHost1,srcHost2,dstHost2,...
        String hostMap = configData.getProperty("hosts.map");
        if (hostMap == null)
        {
            configData.hostsMap = null;
        }
        else
        {
            String[] hostList = hostMap.split(",");
            if (hostList.length % 2 != 0)
            {
                System.out.println("Illegal property: hosts.map " + hostMap);
            }
            else
            {
                configData.hostsMap = new HashMap<String, String>();
                for (int i = 0 ; i < hostList.length ; i += 2)
                {
                    configData.hostsMap.put(hostList[i], hostList[i+1]);
                }
            }
        }

        String minHuge = configData.getProperty("min.huge");
        int minHugeBuffer = 64*1024;
        if (minHuge != null)
        {
            try {
                minHugeBuffer = Integer.parseInt(minHuge);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: min.huge " + minHuge);
            }
        }
        configData.minHugeBuffer = minHugeBuffer;

        String minLarge = configData.getProperty("min.large");
        int minLargeBuffer = 32*1024;
        if (minLarge != null)
        {
            try {
                minLargeBuffer = Integer.parseInt(minLarge);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: min.large " + minLarge);
            }
            
        }
        configData.minLargeBuffer = minLargeBuffer;

        String minMedium = configData.getProperty("min.medium");
        int minMediumBuffer = 8*1024;
        if (minMedium != null)
        {
            try {
                minMediumBuffer = Integer.parseInt(minMedium);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: min.medium " + minMedium);
            }
            
        }
        configData.minMediumBuffer = minMediumBuffer;

        String minSmall = configData.getProperty("min.small");
        int minSmallBuffer = 200;
        if (minSmall != null)
        {
            try {
                minSmallBuffer = Integer.parseInt(minSmall);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: min.small " + minSmall);
            }
            
        }
        configData.minSmallBuffer = minSmallBuffer;

        String isDebug = configData.getProperty("debug");
        configData.isDebug = "true".equalsIgnoreCase(isDebug); // Default is "false".

        String isSetCookieStatefull = configData.getProperty("set.cookie.statefull");
        configData.isSetCookieStatefull = !"false".equalsIgnoreCase(isSetCookieStatefull); // Default is "true"

        // Handling CDN rules:
        configData.clearCdns();
        for (int cdnInd = 1 ; cdnInd < 1000 ; cdnInd++)
        {
            String cdnRule = configData.getProperty("cdn-" + Integer.toString(cdnInd));
            if (cdnRule == null)
                break; // No more CDN rules

            try
            {
                String[] ruleParts = cdnRule.split(",");

                if (ruleParts.length != 4 && ruleParts.length != 5)
                {
                    System.out.println("Illegal propertyl: cdn-" + cdnInd + " " + cdnRule);
                    continue;
                }

                String srcHost = ruleParts[0];
                int minSize = Integer.parseInt(ruleParts[1]) * 1024;
                int maxSize = Integer.parseInt(ruleParts[2]) * 1024;
                String cdnHost = ruleParts[3];
                String cdnSslHost = ruleParts[3];
                if (ruleParts.length == 5)
                    cdnSslHost = ruleParts[4];
                configData.addCdn(srcHost, minSize, maxSize, cdnHost, cdnSslHost);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal propertyl: cdn-" + cdnInd + " " + cdnRule);
            }
        }

        String removeFromHtml = configData.getProperty("remove.from.html");
        if (removeFromHtml != null)
            configData.removeFromHtml = removeFromHtml.getBytes();
        else
            configData.removeFromHtml = null;

        String removeFromCss = configData.getProperty("remove.from.css");
        if (removeFromCss != null)
            configData.removeFromCss = removeFromCss.getBytes();
        else
            configData.removeFromCss = null;

        String sslLocalhost = configData.getProperty("ssl.localhost");
        configData.isSslLocalhost = !"false".equalsIgnoreCase(sslLocalhost); // Default is true

        String fixedMaxAge = configData.getProperty("fixed.maxage");
        configData.isFixedMaxAge = "true".equalsIgnoreCase(fixedMaxAge);

        String minMaxAge = configData.getProperty("min.maxage");
        configData.minStateLessMaxAge = 7*60; // Default
        if (minMaxAge != null)
        {
            try
            {
                configData.minStateLessMaxAge = Long.parseLong(minMaxAge);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: min.maxage " + minMaxAge);
            }
        }

        String maxMaxAge = configData.getProperty("max.maxage");
        configData.maxServerSideMaxAge = 0; // Default value 0 means unlimited
        if (maxMaxAge != null)
        {
            try
            {
                configData.maxServerSideMaxAge = Long.parseLong(maxMaxAge);
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: max.maxage " + maxMaxAge);
            }
        }

        String variants = configData.getProperty("variants");
        configData.minStateLessVariant = 0; // Default
        configData.maxStateLessVariant = 4; // Default
        configData.minStateFullVariant = 0; // Default
        configData.maxStateFullVariant = 4; // Default
        if (variants != null)
        {
            try
            {
                String[] offsets = variants.split(",");
                if (offsets.length == 2)
                {
                    int minStateLess = Integer.parseInt(offsets[0]);
                    int maxStateLess = Integer.parseInt(offsets[1]);
                    configData.minStateLessVariant = minStateLess;
                    configData.maxStateLessVariant = maxStateLess;
                }
                if (offsets.length == 4)
                {
                    int minStateLess = Integer.parseInt(offsets[0]);
                    int maxStateLess = Integer.parseInt(offsets[1]);
                    int minStateFull = Integer.parseInt(offsets[2]);
                    int maxStateFull = Integer.parseInt(offsets[3]);
                    configData.minStateLessVariant = minStateLess;
                    configData.maxStateLessVariant = maxStateLess;
                    configData.minStateFullVariant = minStateFull;
                    configData.maxStateFullVariant = maxStateFull;
                }
            }
            catch (NumberFormatException e)
            {
                System.out.println("Illegal property: variants " + variants);
            }
        }

        String isVersioning = configData.getProperty("versioning");
        configData.isVersioning = "true".equalsIgnoreCase(isVersioning);

        String isBaseTargetParent = configData.getProperty("base.target.parent");
        configData.isBaseTargetParent = !"false".equalsIgnoreCase(isBaseTargetParent); // Default is true

        configData.queryEncoding = configData.getProperty("query.encoding");
        if (configData.queryEncoding == null)
            configData.queryEncoding = "ISO-8859-1";

        String ieMinContentFirst = configData.getProperty("ie.min.content.first");
        if (ieMinContentFirst != null)
        {
            try {
                configData.ieMinContentFirst = Integer.parseInt(ieMinContentFirst);
            }
            catch (NumberFormatException e)
            {
                configData.ieMinContentFirst = ConfigData.IE_MIN_CONTENT_FIRST_DEFAULT;
                System.out.println("Illegal property: ie.min.content.first " + ieMinContentFirst);
            }
        }
        else
        {
           configData.ieMinContentFirst = ConfigData.IE_MIN_CONTENT_FIRST_DEFAULT; 
        }

        if (isGaliel)
        {
            // HtmlSpeed server is in multi domains support mode:
            configData.maxCacheTotalSize = 1024 * 1024 * 50; // Default is 50 Megabytes
            String maxCacheMega = configData.getProperty("max.cache.mega");
            if (maxCacheMega != null)
            {
                try
                {
                    configData.maxCacheTotalSize = 1024 * 1024 * Integer.parseInt(maxCacheMega);
                }
                catch (NumberFormatException e)
                {
                    System.out.println("Illegal property: max.cache.mega " + ieMinContentFirst);
                }
            }
            configData.lowerCacheTotalSize = configData.maxCacheTotalSize * 75 / 100;
        }
        else
        {
            // Otherwise a single domain is accelarated thus max-cache is 60% of heap.
        }

    }

    /**
     * Called by HtmlSpeedServlet.init to load all configuration files into memory.
     */
    public static void loadAllConfigFiles() throws ServletException
    {
        loadHostInfo();

        if (!isGaliel)
        {
            _configs = null;
            loadContentFirstPatterns(CONFIG_DIR, File.separator + "content-first.txt", _configData);
            loadDeferPatterns(CONFIG_DIR, File.separator + "defer.txt", _configData);
            loadStateFullPatterns(CONFIG_DIR, File.separator + "state-full.txt", _configData);
            loadStateLessPatterns(CONFIG_DIR, File.separator + "state-less.txt", _configData);
            loadNoInlinePatterns(CONFIG_DIR, File.separator + "no-inline.txt", _configData);
            System.out.println(loadProperties(CONFIG_DIR, File.separator + "properties.txt", _configData).replace("<br/>", "\n"));
            loadAutoRefreshed(File.separator + "auto-refreshed.txt");
            return;
        }

        // Loading global-properties:
        System.out.println(ConfigUtils.loadProperties(CONFIG_DIR, File.separator + "properties.txt", _configData).replace("<br/>", "\n"));

        _configs = new HashMap<String, ConfigData>(1000);

        // Scanning directory-tree:
        scanDir(new File(CONFIG_DIR + File.separator + "domains"), 0, true);
    }

    /**
     * Loads configuration-files for domain specified by relPath.
     *
     * Locks entire configuration for read, and updated domain for write.
     *
     * @param relPath path to domain-dir relative to CONFIG_DIR/domains
     * @param cleanCache when true cache of reloaded domain is cleaned
     * @throws ServletException 
     */
    public static void load(String relPath, boolean cleanCache) throws ServletException
    {
        int iLastPathSep = relPath.lastIndexOf("/");
        String domain = relPath.substring(iLastPathSep + 1);
        File dir = new File(CONFIG_DIR + File.separator + "domains" +
                                    (iLastPathSep < 0 ? "" : File.separator + relPath.substring(0, iLastPathSep)));
        File file = new File(dir.getAbsolutePath() + File.separator + domain);
        String path = file.getAbsolutePath();
        ConfigData configData = _configs.get(domain);
        if (configData == null)
        {
            new ConfigData();
        }
        else
        {
            if (configData.otherDomains != null)
            {
                for (String otherDomain : configData.otherDomains)
                {
                    _configs.remove(otherDomain);
                }
            }
        }

        configData.lock.writeLock().lock();
        try
        {
            configData.clean();
            if (cleanCache)
            {
                CacheStructure cache = configData.cache;
                cache.rsrcs.clear();
                cache.stateFullRsrcs.clear();
                cache.rsrcsList.clear();
            }
            configData.dirPath = dir.getPath();
            loadOtherDomains(path, File.separator + "other-domains.txt", configData);
            loadContentFirstPatterns(path, File.separator + "content-first.txt", configData);
            loadDeferPatterns(path, File.separator + "defer.txt", configData);
            loadStateFullPatterns(path, File.separator + "state-full.txt", configData);
            loadStateLessPatterns(path, File.separator + "state-less.txt", configData);
            loadNoInlinePatterns(path, File.separator + "no-inline.txt", configData);
            loadProperties(path, File.separator + "properties.txt", configData);
            configData.domain = domain;

            _configs.put(domain, configData);

            if (configData.otherDomains != null)
            {
                for (String otherDomain : configData.otherDomains)
                {
                    _configs.put(otherDomain, configData);
                }
            }
        }
        finally
        {
            configData.lock.writeLock().unlock();
        }
    }

    /**
     * Recursively scans dir and loads all configuration files in all found domain-dirs.
     *
     * A domain-dir is a directory having a '.' character in its name.
     *
     * @param dir scaned directory (not a domain-dir)
     * @param level 0 when dir is CONFIG_DIR, 1 when directly nested under it, ...
     * @param isInWrittenToRoute true when we are in the written-to route
     */
    private static void scanDir(File dir, int level, boolean isInWrittenToRoute) throws ServletException
    {
        for (File file : dir.listFiles())
        {
            if (file.isDirectory())
            {
                if (file.getName().contains("."))
                {
                    // Directory is a domain:
                    String path = file.getAbsolutePath();
                    String domain = file.getName();
                    ConfigData configData = new ConfigData();
                    configData.dirPath = dir.getPath();
                    loadOtherDomains(path, File.separator + "other-domains.txt", configData);
                    loadContentFirstPatterns(path, File.separator + "content-first.txt", configData);
                    loadDeferPatterns(path, File.separator + "defer.txt", configData);
                    loadStateFullPatterns(path, File.separator + "state-full.txt", configData);
                    loadStateLessPatterns(path, File.separator + "state-less.txt", configData);
                    loadNoInlinePatterns(path, File.separator + "no-inline.txt", configData);
                    loadProperties(path, File.separator + "properties.txt", configData);
                    configData.domain = domain;
                    _configs.put(domain, configData);
                    if (configData.otherDomains != null)
                    {
                        for (String otherDomain : configData.otherDomains)
                        {
                            _configs.put(otherDomain, configData);
                        }
                    }
                }
                else
                {
                    // Non-domain directory:
                    if (isInWrittenToRoute && level < _configDirs.length)
                    {
                        try
                        {
                            int num = Integer.parseInt(file.getName());
                            if (num > _configDirs[level])
                            {
                                _configDirs[level] = num;
                                for (int l = level + 1; l < _configDirs.length ; l++)
                                {
                                    _configDirs[l] = 0; // Need to search maximums in nested levels
                                }
                                scanDir(file, level+1, true);
                                continue;
                            }
                        }
                        catch (NumberFormatException e)
                        {
                        }
                    }
                    scanDir(file, level+1, false);
                }
            }
        }
    }

    /**
     * Periodicaly checks if any of the configuration-files
     * have changed and loads the changed configuration-files.
     */
    private static class BackThread extends Thread
    {
        public BackThread(String threadName)
        {
            super(threadName);
        }

        @Override
        public void run()
        {
            while (true)
            {
                try
                {
                    sleep(30000);
                }
                catch (InterruptedException exc)
                {
                }

                if (isGaliel)
                    continue; // When isGaliel we have distributed configuration.

                boolean isLicenseModified = false;
                long licenseLastModified = new File(LICENSE_FILE_PATH).lastModified();
                if (_licenseLastModified != licenseLastModified)
                {
                    _licenseLastModified = licenseLastModified;
                    isLicenseModified = true;
                }

                boolean isHostInfoModified = false;
                long hostInfoLastModified = new File(HOST_INFO_FILE_PATH).lastModified();
                if (_hostInfoLastModified != hostInfoLastModified)
                {
                    _hostInfoLastModified = hostInfoLastModified;
                    isHostInfoModified = true;
                }

                boolean isPropertiesModified = false;
                long propertiesLastModified = new File(_propertiesFilePath).lastModified();
                if (_propertiesLastModified != propertiesLastModified)
                {
                    _propertiesLastModified = propertiesLastModified;
                    isPropertiesModified = true;
                }

                boolean isContentFirstModified = false;
                long contentFirstLastModified = new File(_contentFirstFilePath).lastModified();
                if (_contentFirstLastModified != contentFirstLastModified)
                {
                    _contentFirstLastModified = contentFirstLastModified;
                    isContentFirstModified = true;
                }

                boolean isDeferModified = false;
                long deferLastModified = new File(_deferFilePath).lastModified();
                if (_deferLastModified != deferLastModified)
                {
                    _deferLastModified = deferLastModified;
                    isDeferModified = true;
                }

                boolean isStateFullModified = false;
                long stateFullLastModified = new File(_stateFullFilePath).lastModified();
                if (_stateFullLastModified != stateFullLastModified)
                {
                    _stateFullLastModified = stateFullLastModified;
                    isStateFullModified = true;
                }

                boolean isStateLessModified = false;
                long stateLessLastModified = new File(_stateLessFilePath).lastModified();
                if (_stateLessLastModified != stateLessLastModified)
                {
                    _stateLessLastModified = stateLessLastModified;
                    isStateLessModified = true;
                }

                boolean isNoInlineModified = false;
                long noInlineLastModified = new File(_noInlineFilePath).lastModified();
                if (_noInlineLastModified != noInlineLastModified)
                {
                    _noInlineLastModified = noInlineLastModified;
                    isNoInlineModified = true;
                }

                boolean isAutoRefreshedModified = false;
                long autoRefreshedLastModified = new File(_autoRefreshedFilePath).lastModified();
                if (_autoRefreshedLastModified != autoRefreshedLastModified)
                {
                    _autoRefreshedLastModified = autoRefreshedLastModified;
                    isAutoRefreshedModified = true;
                }

                if (isLicenseModified || isHostInfoModified || isPropertiesModified || isContentFirstModified ||
                        isDeferModified || isStateFullModified || isStateLessModified || isNoInlineModified ||
                        isAutoRefreshedModified)
                {
                    // At least 1 configuration-file has changed:
                    configLock.writeLock().lock();
                    try
                    {
                        if (isDeferModified || isStateFullModified || isStateLessModified || isNoInlineModified)
                        {
                            CacheStructure cache = _configData.cache;
                            cache.rsrcs.clear();
                            cache.stateFullRsrcs.clear();
                            cache.rsrcsList.clear();
                        }

                        if (isLicenseModified)
                            loadLicense();

                        if (isHostInfoModified)
                        {
                            try{ System.out.println(loadHostInfo()); } catch (ServletException exc){}
                        }

                        if (isPropertiesModified)
                        {
                            try
                            {
                                System.out.println(
                                        loadProperties(CONFIG_DIR, _propertiesFileName, _configData).replace("<br/>", "\n"));
                            }
                            catch (ServletException exc){}
                        }

                        if (isContentFirstModified)
                            System.out.println(
                                    loadContentFirstPatterns(CONFIG_DIR, _contentFirstFileName, _configData).replace("<br/>", "\n"));

                        if (isDeferModified)
                            System.out.println(
                                    loadDeferPatterns(CONFIG_DIR, _deferFileName, _configData).replace("<br/>", "\n"));

                        if (isStateFullModified)
                            System.out.println(
                                    loadStateFullPatterns(CONFIG_DIR, _stateFullFileName, _configData).replace("<br/>", "\n"));

                        if (isStateLessModified)
                            System.out.println(
                                    loadStateLessPatterns(CONFIG_DIR, _stateLessFileName, _configData).replace("<br/>", "\n"));

                        if (isNoInlineModified)
                            System.out.println(
                                    loadNoInlinePatterns(CONFIG_DIR, _noInlineFileName, _configData).replace("<br/>", "\n"));

                        if (isAutoRefreshedModified)
                            loadAutoRefreshed(_autoRefreshedFileName);
                    }
                    finally
                    {
                        configLock.writeLock().unlock();
                    }
                }
            }
        }
    }

    private static final byte[] SERIALIZED_PUBLIC_KEY2 = new byte[] {
                1,1,0,-64,115,3,-124,-14,5,-37,-103,-67,40,38,-20,-12,-14,-39,66,36,-127,115,-127,-66,
                -102,-98,85,-46,62,-109,114,4,-110,-116,-92,98,-119,-13,106,-58,123,-123,-84,46,-53,
                119,-107,90,6,-54,124,-69,1,110,-69,11,70,-126,123,-118,64,4,-95,60,-57,-28,-49,124,
                -21,-32,70,-43,73,38,72,-64,-1,24,76,41,-37,38,86,-38,-115,90,51,97,52,-56,69,83,-89,
                42,20,-113,113,-18,24,115,-128,-42,63,-41,-122,94,124,-51,-84,19,78,75,-31,-47,38,
                -126,-78,68,35,-86,49,95,107,-39,48,94,-84,-107,-9,-71,124,-112,-112,-113,3,-86,95,
                -107,18,6,-17,67,91,72,15,-34,-62,39,20,82,-41,-118,-79,-16,-70,-13,120,-35,-54,-38,
                39,-116,-14,-72,89,127,-32,86,-47,-104,-119,-4,-36,27,108,-73,27,-126,-10,110,-54
                };
    private static final byte[] SERIALIZED_PUBLIC_KEY1 = new byte[] {
                -84,-19,0,5,115,114,0,20,106,97,118,97,46,115,101,99,117,114,105,116,121,46,75,
                101,121,82,101,112,-67,-7,79,-77,-120,-102,-91,67,2,0,4,76,0,9,97,108,103,111,114,
                105,116,104,109,116,0,18,76,106,97,118,97,47,108,97,110,103,47,83,116,114,105,
                110,103,59,91,0,7,101,110,99,111,100,101,100,116,0,2,91,66,76,0,6,102,111,114,109,
                97,116,113,0,126,0,1,76,0,4,116,121,112,101,116,0,27,76,106,97,118,97,47,115,101,
                99,117,114,105,116,121,47,75,101,121,82,101,112,36,84,121,112,101,59,120,112,116,
                0,3,82,83,65,117,114,0,2,91,66,-84,-13,23,-8,6,8,84,-32,2,0,0,120,112,0,0,1,38,48,
                -126,1,34,48,13,6,9,42,-122,72,-122,-9,13,1,1,1,5,0,3,-126,1,15,0,48,-126,1,10,2,-126
                };
    private static final byte[] SERIALIZED_PUBLIC_KEY3 = new byte[] {
                88,7,-120,-84,-62,-47,-46,16,67,23,-6,102,-74,-83,-41,68,-61,26,-93,3,9,-127,-121,-90,
                27,-75,32,40,125,-74,-80,-30,36,-29,-34,-32,-87,-78,122,-31,27,73,29,-111,-26,111,96,
                -99,1,-110,-122,40,39,73,-127,-42,28,25,90,68,18,-34,-114,106,-120,9,-51,-116,-71,
                109,-54,-76,-115,-7,118,127,50,-103,2,3,1,0,1,116,0,5,88,46,53,48,57,126,114,0,25,
                106,97,118,97,46,115,101,99,117,114,105,116,121,46,75,101,121,82,101,112,36,84,
                121,112,101,0,0,0,0,0,0,0,0,18,0,0,120,114,0,14,106,97,118,97,46,108,97,110,103,46,
                69,110,117,109,0,0,0,0,0,0,0,0,18,0,0,120,112,116,0,6,80,85,66,76,73,67
                };

}
