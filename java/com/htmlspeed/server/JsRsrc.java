/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

/**
 * JsRsrc
 *
 * A Java-Script loaded from content-provider.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.12 $$Date: 2013/08/21 06:55:20 $
 */
public class JsRsrc extends Rsrc
{
    public boolean isInIEComment; // True when JS is inside IE comment.

    public boolean isInlinable; // True when script can be inlined in page.

    /**
     * Initializing isInlinable by scaning origData.
     *
     * @param configData used configuration
     */
    public void checkInlinable(ConfigData configData)
    {
        if (origData == null)
        {
            isInlinable = false;
            return;
        }

        if (configData.isStateFull(url))
        {
            isInlinable = false;
            return;
        }

        if (isInIEComment)
        {
            isInlinable = false;
            return;
        }

        byte[] bytes = origData;

        if (bytes == null)
        {
            isInlinable = false;
            return;
        }

        //
        // Not inlining JS files containing "<!--" (it confuses the IE browser):
        //
        for (int i = 0 ; i < bytes.length - 4 ; i++)
        {
            if (bytes[i] == '<' &&
                    bytes[i+1] == '!' &&
                    bytes[i+2] == '-' &&
                    bytes[i+3] == '-')
            {
                isInlinable = false;
                return;
            }
        }

        //
        // Not inlining JS files containing "<SCRIPT" or "</script" even inside comments (it confuses the browser):
        //
        for (int i = 0 ; i < bytes.length - 7 ; i++)
        {
            if (bytes[i] == '<' &&
                    (bytes[i+1] == 's' || bytes[i+1] == 'S') &&
                    (bytes[i+2] == 'c' || bytes[i+2] == 'C') &&
                    (bytes[i+3] == 'r' || bytes[i+3] == 'R') &&
                    (bytes[i+4] == 'i' || bytes[i+4] == 'I') &&
                    (bytes[i+5] == 'p' || bytes[i+5] == 'P') &&
                    (bytes[i+6] == 't' || bytes[i+6] == 'T'))
            {
                isInlinable = false;
                return;
            }

            if (bytes[i] == '<' && bytes[i+1] == '/' &&
                    (bytes[i+2] == 's' || bytes[i+2] == 'S') &&
                    (bytes[i+3] == 'c' || bytes[i+3] == 'C') &&
                    (bytes[i+4] == 'r' || bytes[i+4] == 'R') &&
                    (bytes[i+5] == 'i' || bytes[i+5] == 'I') &&
                    (bytes[i+6] == 'p' || bytes[i+6] == 'P') &&
                    (bytes[i+7] == 't' || bytes[i+7] == 'T'))
            {
                isInlinable = false;
                return;
            }
        }

        //
        // Not inlining JS files containing hebrew characters (they confuse the browser):
        //
        for (int i = 3 ; i < bytes.length ; i++) // First 3 chars in file can be EF BB BF (marking utf-8)
        {
            if (bytes[i] < 0)
            {
                isInlinable = false;
                return;
            }
        }

        //
        // Not inlining JS files containing HTML documents:
        //
        int sp = 0;
        while (sp < bytes.length && bytes[sp] <= ' ')
            sp++;
        if (sp+5 < bytes.length && bytes[sp] == '<' &&
                (bytes[sp+1] == 'h' || bytes[sp+1] == 'H') &&
                (bytes[sp+2] == 't' || bytes[sp+2] == 'T') &&
                (bytes[sp+3] == 'm' || bytes[sp+3] == 'M') &&
                (bytes[sp+4] == 'l' || bytes[sp+4] == 'L') &&
                 bytes[sp+5] == '>')
        {
                isInlinable = false;
                return;
        }

        isInlinable = true;
    }

}
