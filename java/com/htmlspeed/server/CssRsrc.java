/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 8 March 2012
 */
package com.htmlspeed.server;

/**
 * CssRsrc
 *
 * A stylesheet loaded from content-provider.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.17 $$Date: 2013/08/21 06:54:22 $
 */
public class CssRsrc extends Rsrc
{
    public boolean isInIEComment; // True when stylesheet is inside IE comment.
    public boolean isOptimized; // True after referenced images are inlined in the stylesheet.
    public boolean isInlinable; // True when stylesheet can be inlined in page.

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
        // Not inlining stylesheet files containing "@import" or "@font-face"
        //
        for (int i = 0 ; i < bytes.length - 7 ; i++)
        {
            if (bytes[i] == '@' &&
                    (bytes[i+1] == 'i' || bytes[i+1] == 'I') &&
                    (bytes[i+2] == 'm' || bytes[i+2] == 'M') &&
                    (bytes[i+3] == 'p' || bytes[i+3] == 'P') &&
                    (bytes[i+4] == 'o' || bytes[i+4] == 'O') &&
                    (bytes[i+5] == 'r' || bytes[i+5] == 'R') &&
                    (bytes[i+6] == 't' || bytes[i+6] == 'T'))
            {
                isInlinable = false;
                return;
            }

            if (bytes[i] == '@' &&
                    (bytes[i+1] == 'f' || bytes[i+1] == 'F') &&
                    (bytes[i+2] == 'o' || bytes[i+2] == 'O') &&
                    (bytes[i+3] == 'n' || bytes[i+3] == 'N') &&
                    (bytes[i+4] == 't' || bytes[i+4] == 'T') &&
                    (bytes[i+5] == '-') &&
                    (bytes[i+6] == 'f' || bytes[i+6] == 'F') &&
                    (bytes[i+7] == 'a' || bytes[i+7] == 'A') &&
                    (bytes[i+8] == 'c' || bytes[i+8] == 'C') &&
                    (bytes[i+9] == 'e' || bytes[i+9] == 'E'))
            {
                isInlinable = false;
                return;
            }

        }

        isInlinable = true;
    }

}
