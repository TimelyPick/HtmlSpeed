/*
 *  Copyright 2001 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 */

/*
 * ImageAnalyzer.java
 *
 * Created on 22 Jan 2012
 */

package com.htmlspeed.server;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * ImageAnalyzer
 *
 *    Analyzes images an returns their: mime-type, width, and height.
 *
 *    The actual width and height of an image is inserted into IMG elements
 *    of HTML pages when the attributes width, and height are not specified.
 *
 * '$Id: ImageAnalyzer.java,v 1.3 2012/02/29 17:32:54 eldad Exp $'
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.3 $$Date: 2012/02/29 17:32:54 $
 */
public class ImageAnalyzer
{
    private int _height; // Height in pixels of analyzed image.
    private int _width; // Width in pixel of anzlyzed image.
    private String _mime; // Mime-type of image: "image/gif", "image/jpg", ...

    private ImageAnalyzer()
    {
    }

    /**
     * Analyzes the specified image-file.
     *
     * @param file a file containing an image.
     * @throws IOException when can't read file or when
     *                  file doesn't contain an image of supported type.
     */
    public ImageAnalyzer(File file) throws IOException
    {
            InputStream is = new FileInputStream(file);
            try
            {
                    analyze(is);
            }
            finally
            {
                    is.close();
            }
    }

    /**
     * Analyzes the specified stream (assumed to contain an image).
     *
     * @param is an input-stream containing an image.
     * @throws IOException when is doesn't contain an image of supported type.
     */
    public ImageAnalyzer(InputStream is) throws IOException
    {
            analyze(is);
    }

    /**
     * Analyzes the specified byte-array (assumed to contain an image).
     *
     * @param bytes a byte-array containing an image.
     * @throws IOException when bytes doesn't contain an image of supported type.
     */
    public ImageAnalyzer(byte[] bytes) throws IOException
    {
            InputStream is = new ByteArrayInputStream(bytes);
            try
            {
                    analyze(is);
            }
            finally
            {
                    is.close();
            }
    }

    /**
     * @return the height of the image
     */
    public int getHeight()
    {
            return _height;
    }

    /**
     * @return the width of the image
     */
    public int getWidth()
    {
            return _width;
    }

    /**
     * @return the mime-type of the image ("image/jpg", "image/png", ...)
     */
    public String getMime()
    {
            return _mime;
    }

    @Override
    public String toString() {
            return "Mime: " + _mime + "\t Width : " + _width + "\t Height : " + _height;
    }

    /**
     * Analyzes the input-stream to find its mime-type, width, and height.
     *
     * @param is an input-stream containing the analyzed image.
     * @throws IOException when is doesn't contain an image of supported type.
     */
    private void analyze(InputStream is) throws IOException
    {
            int c1 = is.read();
            int c2 = is.read();
            int c3 = is.read();

            _mime = null;
            _width = _height = -1;

            if (c1 == 'G' && c2 == 'I' && c3 == 'F')
            { // GIF
                    is.skip(3);
                    _width = readInt(is, 2, false);
                    _height = readInt(is, 2, false);
                    _mime = "image/gif";
            }
            else if (c1 == 0xFF && c2 == 0xD8)
            { // JPG
                    while (c3 == 255)
                    {
                            int marker = is.read();
                            int len = readInt(is, 2, true);
                            if (marker == 192 || marker == 193 || marker == 194)
                            {
                                    is.skip(1);
                                    _height = readInt(is, 2, true);
                                    _width = readInt(is, 2, true);
                                    _mime = "image/jpeg";
                                    break;
                            }
                            is.skip(len - 2);
                            c3 = is.read();
                    }
            }
            else if (c1 == 137 && c2 == 80 && c3 == 78)
            { // PNG
                    is.skip(15);
                    _width = readInt(is,2,true);
                    is.skip(2);
                    _height = readInt(is,2,true);
                    _mime = "image/png";
            }
            else if (c1 == 66 && c2 == 77)
            { // BMP
                    is.skip(15);
                    _width = readInt(is,2,false);
                    is.skip(2);
                    _height = readInt(is,2,false);
                    _mime = "image/bmp";
            }
            else
            {
                    int c4 = is.read();
                    if ((c1 == 'M' && c2 == 'M' && c3 == 0 && c4 == 42)
                                    || (c1 == 'I' && c2 == 'I' && c3 == 42 && c4 == 0))
                    { //TIFF
                            boolean bigEndian = c1 == 'M';
                            int ifd = 0;
                            int entries;
                            ifd = readInt(is, 4, bigEndian);
                            is.skip(ifd - 8);
                            entries = readInt(is, 2, bigEndian);
                            for (int i = 1; i <= entries; i++)
                            {
                                    int tag = readInt(is,2,bigEndian);
                                    int fieldType = readInt(is, 2, bigEndian);
                                    long count = readInt(is, 4, bigEndian);
                                    int valOffset;
                                    if ((fieldType == 3 || fieldType == 8))
                                    {
                                            valOffset = readInt(is, 2, bigEndian);
                                            is.skip(2);
                                    }
                                    else
                                    {
                                            valOffset = readInt(is, 4, bigEndian);
                                    }
                                    if (tag == 256)
                                    {
                                            _width = valOffset;
                                    }
                                    else if (tag == 257)
                                    {
                                            _height = valOffset;
                                    }
                                    if (_width != -1 && _height != -1)
                                    {
                                            _mime = "image/tiff";
                                            break;
                                    }
                            }
                    }
            }
    }

    /**
     * @param is the input-stream containing the analyzed image.
     * @param noOfBytes size of read value in bytes (2 or 4)
     * @param bigEndian true when high-order byte is first in is.
     * @return the number of size noOfBytes bytes just read from is.
     * @throws IOException can occur when reading from is.
     */
    private int readInt(InputStream is, int noOfBytes, boolean bigEndian) throws IOException
    {
            int value = 0; // Returned value.

            int shiftLeftBy = bigEndian ? ((noOfBytes - 1) * 8) : 0;
            int shiftIncrement = bigEndian ? -8 : 8;

            for (int i = 0; i < noOfBytes ; i++)
            {
                    value |= is.read() << shiftLeftBy;
                    shiftLeftBy += shiftIncrement;
            }

            return value;
    }

}
