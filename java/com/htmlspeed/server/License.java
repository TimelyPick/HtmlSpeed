/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 22 October 2012
 */
package com.htmlspeed.server;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Scanner;
import javax.crypto.Cipher;

/**
 * License
 *
 * A program for generating licenses for using htmlspeed servers.
 *
 * A license issued to domain galiel314.com is used for testing puproses (valid for 2 weeks).
 *
 * Any other licence is issued to a comma separated list of domains.
 * The domains "xxx" and "www.xxx" are separately listed when both
 * need to be licensed. A license issued to ".xxx" is valid for any sub-
 * domain of "xxx" (including "www.xxx" but excluding "xxx").
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.4 $$Date: 2013/01/11 09:39:56 $
 */
public class License
{

    private Cipher _cipher = null; // Used at both sides
    private KeyPair _keyPair = null; // At our side
    RSAPublicKey _pubKey = null; // At htmlspeed server side

    public KeyPair getPrivateKey(KeyStore keystore, String alias, String password)
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

    public void encodeAndDecodeLicense()
    {
        try
        {
            System.out.print("Enter licensed domains: ");
            Scanner inScan = new Scanner(System.in);
            String origMsg = inScan.nextLine();
            if (origMsg == null || origMsg.length() == 0)
                return;
            System.out.print("Enter output directory: ");
            String outDir = inScan.nextLine();
            if (outDir == null || outDir.length() == 0)
                return;

            if (origMsg.equals( "galiel314.com"))
                origMsg = origMsg + ' ' + DateUtils.formatDate(new Date());

            // Our server:
            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType()); 
            java.io.FileInputStream fis = new java.io.FileInputStream("C:/eldad/java/ssl/.keystore"); 
            ks.load(fis, "yaelarielbaraktamir".toCharArray()); 
            fis.close(); 
            _keyPair = getPrivateKey(ks, "mykey", "yaelarielbaraktamir");

            // Both:
            _cipher = Cipher.getInstance("RSA");

            _cipher.init(Cipher.ENCRYPT_MODE, _keyPair.getPrivate());
            byte[] encMsg = _cipher.doFinal(origMsg.getBytes()); // 256 bytes (size of key)

            final String LICENSE_PATH = outDir + "\\license.dat";
            File file = new File(LICENSE_PATH);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(encMsg);
            fos.close();
            
            // htmlspeed server:
            InputStream inStream = new FileInputStream("C:/eldad/java/ssl/certificate.cer");
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            X509Certificate cert = (X509Certificate)cf.generateCertificate(inStream);
            inStream.close();
            _pubKey = (RSAPublicKey) cert.getPublicKey();

            _cipher.init(Cipher.DECRYPT_MODE, _pubKey);
            byte[] decMsg = _cipher.doFinal(encMsg);
            System.out.println("Created file " + LICENSE_PATH + " for domains: ");
            System.out.println(new String(decMsg));

            final String LICENSED_FOR_PATH = outDir + "\\licensed-for.txt";
            File licensedForFile = new File(LICENSED_FOR_PATH);
            FileOutputStream licensedForStream = new FileOutputStream(licensedForFile);
            licensedForStream.write(origMsg.getBytes());
            licensedForStream.close();
            System.out.println("Created file: " + LICENSED_FOR_PATH);
            
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }
    }
    public static void main(String[] args)
    {
        new License().encodeAndDecodeLicense();
    }
}
