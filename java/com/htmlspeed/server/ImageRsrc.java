package com.htmlspeed.server;

public class ImageRsrc extends Rsrc
{
    public boolean isStatic;

    public short width;
    public short height;

    public byte[] base64Data; // lazily created when image is inlined

    public boolean isOptimized; // TRUE when image has been optimized by HtmlSpeed

    /**
     * @return the total-size of all timed-buffers.
     */
    @Override
    public int getTotalTimedBuffersSize()
    {
        int total = super.getTotalTimedBuffersSize();
        total += (base64Data != null ? base64Data.length : 0);
        return total;
    }

}
