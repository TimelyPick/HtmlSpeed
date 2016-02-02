/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 31 October 2012
 */
package com.htmlspeed.server;

import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import javax.crypto.Cipher;
import org.eclipse.jetty.client.Address;
import org.eclipse.jetty.client.HttpClient;
import org.eclipse.jetty.http.HttpSchemes;

/**
 * ExitNow.
 *
 * Requesting htmlspeed server to exit-now.
 *
 * Usage:
 *      exitNow domain uri {now|zero} {ukey|ckey|e|rkey} ...
 * 
 *  Used http-headers:
 *      ukey: Uri           (msg-part is appended to uri, as the value of key query-param)
 *      ckey: Cookie    (msg-part is passed as a cookie value of key cookie)
 *      e: Etag              (msg-part is the Etag value)
 *      rkey: Referer    (msg-part is the uri of the referer having the same domain)
 *
 *  Example:
 *      java.exe www.kuku.com /logo.png now uId cJSESSIONID
 *
 *  The exit-now msg:
 *      valid-domain length (1 byte)
 *      valid-domain
 *      current-time or zero (8 bytes)
 *
 *  The msg is encripted into a 256 bytes value and is passed to
 *  the server as an hexa string whos length is 512 characters.
 *  The msg is splited into as many parts as the used http-headers.
 *
 *  If now is specified in the cmd line then the current-time is used
 * when building the exit-now msg. Otherwise the value 0 is used.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.3 $$Date: 2013/01/11 08:33:17 $
  */
public class ExitNow
{
    public static void main(String[] args)
    {
        if (args.length < 4)
            return;

        String domain = args[0];
        byte[] domainBytes = domain.getBytes();

        String uri = args[1];

        boolean useNow = args[2].equalsIgnoreCase("now");

        boolean useUri = false;
        String uriKey = null;
        String uriMsg = null;

        boolean useCookie = false;
        String cookieKey = null;
        String cookieMsg = null;

        boolean useEtag = false;
        String etagMsg = null;

        boolean useReferer = false;
        String refererKey = null;
        String refererMsg = null;

        int partsCount = 0;

        for (int a = 3 ; a < args.length ; a++)
        {
            String arg = args[a];
            if (arg.startsWith("u"))
            {
                useUri = true;
                uriKey = arg.substring(1);
                partsCount++;
            }
            else if (arg.startsWith("c"))
            {
                useCookie = true;
                cookieKey = arg.substring(1);
                partsCount++;
            }
            else if (arg.equals("e"))
            {
                useEtag = true;
                partsCount++;
            }
            else if (arg.startsWith("r"))
            {
                useReferer = true;
                refererKey = arg.substring(1);
                partsCount++;
            }
        }

        if (partsCount < 1)
        {
            System.out.println("At least 1 mechanism must be specified");
            return;
        }

        byte[] origMsg = new byte[1 + domainBytes.length + 8];

        // Appending domainBytes.length to origMsg:
        int offset = 0;
        origMsg[offset++] = (byte)domainBytes.length;

        // Appending domainBytes to origMsg:
        System.arraycopy(domainBytes, 0, origMsg, offset, domainBytes.length);
        offset += domainBytes.length;

        // Appending the current-time to origMsg:
        long currentTime = useNow ? System.currentTimeMillis() : 0;
        writeLong(currentTime, origMsg, offset);
        offset += 8;

        // Encripting origMsg:
        byte[] encMsg;
        try
        {
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType()); 
            java.io.FileInputStream fis = new java.io.FileInputStream("C:/eldad/java/ssl/.keystore"); 
            ks.load(fis, "yaelarielbaraktamir".toCharArray()); 
            fis.close(); 
            KeyPair keyPair = getPrivateKey(ks, "mykey", "yaelarielbaraktamir");

            Cipher cipher = Cipher.getInstance("RSA");

            cipher.init(Cipher.ENCRYPT_MODE, keyPair.getPrivate());
            encMsg = cipher.doFinal(origMsg); // 256 bytes (size of key)
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
            return;
        }

        if (encMsg == null || encMsg.length != 256)
        {
            System.out.println("Length of encripted msg is not 256");
            return;
        }

        byte[] hexBytes = new byte[512];
        int h = 0;
        for (byte b : encMsg)
        {
            int hex = (b & 0x000000f0) >>> 4;
            hexBytes[h++] = hex < 10 ? (byte)('0' + hex) : (byte)('a' + hex-10);

            hex = b & 0x0000000f;
            hexBytes[h++] = hex < 10 ? (byte)('0' + hex) : (byte)('a' + hex-10);
        }

        String hexMsg = new String(hexBytes);

        String[] hexMsgParts = new String[partsCount];
        if (partsCount == 1)
        {
            hexMsgParts[0] = hexMsg;
        }
        else if (partsCount == 2)
        {
            hexMsgParts[0] = hexMsg.substring(0, 256);
            hexMsgParts[1] = hexMsg.substring(256);
        }
        else if (partsCount == 3)
        {
            hexMsgParts[0] = hexMsg.substring(0, 128);
            hexMsgParts[1] = hexMsg.substring(128, 256);
            hexMsgParts[2] = hexMsg.substring(256);
        }
        else
        {
            hexMsgParts[0] = hexMsg.substring(0, 128);
            hexMsgParts[1] = hexMsg.substring(128, 256);
            hexMsgParts[2] = hexMsg.substring(256, 128*3);
            hexMsgParts[3] = hexMsg.substring(128*3);
        }

        int nextPart = 0;
        if (useUri)
            uriMsg = uri + '?' + uriKey + '='  + hexMsgParts[nextPart++];
        else
            uriMsg = uri;

        if (useCookie)
            cookieMsg = cookieKey + '=' + hexMsgParts[nextPart++];

        if (useEtag)
            etagMsg = hexMsgParts[nextPart++];

        if (useReferer)
            refererMsg = "http://" + domain + "/jquery.js?" + refererKey + '=' + hexMsgParts[nextPart++];

        // Invoking http-request on domain:
        try
        {
            HttpClient httpClient = new HttpClient();
            httpClient.start();
            httpClient.setConnectorType(HttpClient.CONNECTOR_SELECT_CHANNEL);
            httpClient.setTimeout(30000);

            LoadLock loadLock = new LoadLock();
            loadLock.setCount(1);

            HtmlSpeedHttpExchange exchange = new HtmlSpeedHttpExchange(loadLock, null);
            exchange.setScheme(HttpSchemes.HTTP);
            exchange.setMethod("GET");
            exchange.setAddress(Address.from(domain + ":80"));
            exchange.addRequestHeader("Host", domain);
            exchange.setRequestURI(uriMsg);

            if (useCookie)
                exchange.addRequestHeader("Cookie", cookieMsg);
            if (useEtag)
                exchange.addRequestHeader("If-None-Match", etagMsg);
            if (useReferer)
                exchange.addRequestHeader("Referer", refererMsg);

            httpClient.send(exchange);

            loadLock.waitUntilCountIs0();
        }
        catch (Exception exc)
        {
            exc.printStackTrace();
        }
    }

    private static final void writeLong(long v, byte[] writeBuffer, int offset)
    {
        writeBuffer[offset + 0] = (byte)(v >>> 56);
        writeBuffer[offset + 1] = (byte)(v >>> 48);
        writeBuffer[offset + 2] = (byte)(v >>> 40);
        writeBuffer[offset + 3] = (byte)(v >>> 32);
        writeBuffer[offset + 4] = (byte)(v >>> 24);
        writeBuffer[offset + 5] = (byte)(v >>> 16);
        writeBuffer[offset + 6] = (byte)(v >>>  8);
        writeBuffer[offset + 7] = (byte)(v >>>  0);
    }

    public static KeyPair getPrivateKey(KeyStore keystore, String alias, String password)
    {
        try
        {
            // Get private key
            Key key = keystore.getKey(alias, password.toCharArray());

            if (key instanceof PrivateKey)
            {
                // Get certificate of public key
                java.security.cert.Certificate cert = keystore.getCertificate(alias);

                // Get public key
                PublicKey publicKey = cert.getPublicKey();

                // Return a key pair
                return new KeyPair(publicKey, (PrivateKey)key);
            }
        }
        catch (UnrecoverableKeyException e) {}
        catch (NoSuchAlgorithmException e) {}
        catch (KeyStoreException e) {}

        return null;
    }

}
