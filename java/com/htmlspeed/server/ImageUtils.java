/*
 * Copyright 1999-2002,2004 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.htmlspeed.server;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Iterator;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.MemoryCacheImageOutputStream;

/**
 * ImageUtils
 *
 * The method encode is copied from com.sun.org.apache.xerces.internal.impl.dv.util.Base64.
 *
 * @author Jeffrey Rodriguez
 * @author Sandy Gao
 * @version $Revision: 1.4 $$Date: 2013/08/21 07:18:15 $
 */
public class ImageUtils
{

    static private final int  BASELENGTH         = 128;
    static private final int  LOOKUPLENGTH       = 64;
    static private final int  TWENTYFOURBITGROUP = 24;
    static private final int  EIGHTBIT           = 8;
    static private final int  SIXTEENBIT         = 16;
    static private final int  SIGN               = -128;
    static private final char PAD                = '=';
    static private final boolean fDebug          = false;
    static final private byte [] base64Alphabet        = new byte[BASELENGTH];
    static final private char [] lookUpBase64Alphabet  = new char[LOOKUPLENGTH];

    static {

        for (int i = 0; i < BASELENGTH; ++i) {
            base64Alphabet[i] = -1;
        }
        for (int i = 'Z'; i >= 'A'; i--) {
            base64Alphabet[i] = (byte) (i-'A');
        }
        for (int i = 'z'; i>= 'a'; i--) {
            base64Alphabet[i] = (byte) ( i-'a' + 26);
        }

        for (int i = '9'; i >= '0'; i--) {
            base64Alphabet[i] = (byte) (i-'0' + 52);
        }

        base64Alphabet['+']  = 62;
        base64Alphabet['/']  = 63;

        for (int i = 0; i<=25; i++)
            lookUpBase64Alphabet[i] = (char)('A'+i);

        for (int i = 26,  j = 0; i<=51; i++, j++)
            lookUpBase64Alphabet[i] = (char)('a'+ j);

        for (int i = 52,  j = 0; i<=61; i++, j++)
            lookUpBase64Alphabet[i] = (char)('0' + j);
        lookUpBase64Alphabet[62] = (char)'+';
        lookUpBase64Alphabet[63] = (char)'/';

    }

   /**
     * Encodes hex octects into Base64
     *
     * @param binaryData Array containing binaryData
     * @return Encoded Base64 array
     */
    public static String encode(byte[] binaryData) {

        if (binaryData == null)
            return null;

        int      lengthDataBits    = binaryData.length*EIGHTBIT;
        if (lengthDataBits == 0) {
            return "";
        }

        int      fewerThan24bits   = lengthDataBits%TWENTYFOURBITGROUP;
        int      numberTriplets    = lengthDataBits/TWENTYFOURBITGROUP;
        int      numberQuartet     = fewerThan24bits != 0 ? numberTriplets+1 : numberTriplets;
        char     encodedData[]     = null;

        encodedData = new char[numberQuartet*4];

        byte k=0, l=0, b1=0,b2=0,b3=0;

        int encodedIndex = 0;
        int dataIndex   = 0;
        if (fDebug) {
            System.out.println("number of triplets = " + numberTriplets );
        }

        for (int i=0; i<numberTriplets; i++) {
            b1 = binaryData[dataIndex++];
            b2 = binaryData[dataIndex++];
            b3 = binaryData[dataIndex++];

            if (fDebug) {
                System.out.println( "b1= " + b1 +", b2= " + b2 + ", b3= " + b3 );
            }

            l  = (byte)(b2 & 0x0f);
            k  = (byte)(b1 & 0x03);

            byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);

            byte val2 = ((b2 & SIGN)==0)?(byte)(b2>>4):(byte)((b2)>>4^0xf0);
            byte val3 = ((b3 & SIGN)==0)?(byte)(b3>>6):(byte)((b3)>>6^0xfc);

            if (fDebug) {
                System.out.println( "val2 = " + val2 );
                System.out.println( "k4   = " + (k<<4));
                System.out.println( "vak  = " + (val2 | (k<<4)));
            }

            encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ val2 | ( k<<4 )];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ (l <<2 ) | val3 ];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ b3 & 0x3f ];
        }

        // form integral number of 6-bit groups
        if (fewerThan24bits == EIGHTBIT) {
            b1 = binaryData[dataIndex];
            k = (byte) ( b1 &0x03 );
            if (fDebug) {
                System.out.println("b1=" + b1);
                System.out.println("b1<<2 = " + (b1>>2) );
            }
            byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ k<<4 ];
            encodedData[encodedIndex++] = PAD;
            encodedData[encodedIndex++] = PAD;
        } else if (fewerThan24bits == SIXTEENBIT) {
            b1 = binaryData[dataIndex];
            b2 = binaryData[dataIndex +1 ];
            l = ( byte ) ( b2 &0x0f );
            k = ( byte ) ( b1 &0x03 );

            byte val1 = ((b1 & SIGN)==0)?(byte)(b1>>2):(byte)((b1)>>2^0xc0);
            byte val2 = ((b2 & SIGN)==0)?(byte)(b2>>4):(byte)((b2)>>4^0xf0);

            encodedData[encodedIndex++] = lookUpBase64Alphabet[ val1 ];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ val2 | ( k<<4 )];
            encodedData[encodedIndex++] = lookUpBase64Alphabet[ l<<2 ];
            encodedData[encodedIndex++] = PAD;
        }

        return new String(encodedData);
    }

    /**
     * @param srcJpeg source jpeg to be optimized
     * @param configData used configuration
     * @return optimized jpeg with quality reduced to ConfigUtils.jpegQuality
     */
    public static byte[] optimizedJpeg(byte[] srcJpeg, ConfigData configData)
    {
        try
        {
            Iterator iter = ImageIO.getImageWritersByFormatName("jpeg");
            ImageWriter imgWriter = (ImageWriter)iter.next();
            ImageWriteParam imgWriterParam = imgWriter.getDefaultWriteParam();
            imgWriterParam.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
            imgWriterParam.setCompressionQuality(configData.jpegQuality);

            ByteArrayOutputStream baos = new ByteArrayOutputStream(srcJpeg.length);
            MemoryCacheImageOutputStream mcios = new MemoryCacheImageOutputStream(baos);
            imgWriter.setOutput(mcios);

            ByteArrayInputStream bais = new ByteArrayInputStream(srcJpeg);
            BufferedImage origBufImage = ImageIO.read(bais);
            IIOImage origIioImage = new IIOImage(origBufImage, null, null);
            imgWriter.write(null, origIioImage, imgWriterParam);
            imgWriter.dispose();
            byte[] dstJpeg = baos.toByteArray();
            mcios.close();
            baos.close();
            return dstJpeg;
            
        }
        catch (IOException exc)
        {
            return srcJpeg;
        }
    }
}
