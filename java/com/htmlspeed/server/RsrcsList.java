/*
 *  Copyright 2001 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 */
package com.htmlspeed.server;

/**
* RsrcsList.java
 *
 * Created on 13 Feb 2012
 *
 * A doubly-linked-list of timed-buffers, ordered by last access time.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.9 $$Date: 2013/08/21 12:29:05 $
 */
public class RsrcsList
{
    /**
     * Minimum size of TimedBuffer considered to be large.
     */
    public static final int MIN_LARGE_BUFFER = 32*1024;

    /**
     * Minimum size of TimedBuffer considered to be medium-sized.
     */
    public static final int MIN_MEDIUM_BUFFER = 8*1024;

    /**
     * Minimum size of TimedBuffer considered to be small-sized.
     * Buffers smaller than that are considered tiny.
     */
    public static final int MIN_SMALL_BUFFER = 200;

    /**
     * Number of milli-seconds in one day
     */
    private static final long DAY_IN_MILLIS = 1000*3600*24;

    /**
     * The last time when not recently used resources have been removed.
     */
    private static long _lastUnusedRemovalTime = 0;

    /**
     * The least-recently-accest instance in the doubly-linked-list
     */
    public Rsrc head;

    /**
     * The most-recently-accest instance in the doubly-linked-list
     */
    public Rsrc tail;

    /**
     * Current number of timed-buffers in this list
     */
    public volatile int count = 0;

    /**
     * Total size in bytes of all timed-buffers in this list
     */
    private long _totalSize = 0;

    /**
     * The used configuration
     */
    private ConfigData _configData;

    /**
     * CONSTRUCTOR
     *
     * @param configData used configuration
     */
    public RsrcsList(ConfigData configData)
    {
        _configData = configData;
    }

    /**
     * Removes all element from list.
     */
    public synchronized void clear()
    {
        head = null;
        tail = null;
        count = 0;
        _totalSize = 0;
    }

    /**
     * Adds buff to linked-list (after tail)
     *
     * @param buff the added timed-buffer
     */
    public synchronized void add(Rsrc rsrc)
    {
        int rsrcLen = rsrc.getTotalTimedBuffersSize();

        makeRoom(rsrcLen);

        if (tail == null)
        {
            head = rsrc;
            tail = rsrc;
            rsrc.setPrev(null);
            rsrc.setNext(null);
        }
        else
        {
            rsrc.setPrev(tail);
            tail.setNext(rsrc);
            rsrc.setNext(null);
            tail = rsrc;
        }

        ++count;

        _totalSize += rsrcLen;
    }

    public synchronized void remove(Rsrc rsrc)
    {
        
        if (rsrc.getPrev() != null)
            rsrc.getPrev().setNext(rsrc.getNext());
        if (rsrc.getNext() != null)
            rsrc.getNext().setPrev(rsrc.getPrev());
        if (head == rsrc)
            head = rsrc.getNext();
        if (tail == rsrc)
            tail = rsrc.getPrev();

        rsrc.setNext(null);
        rsrc.setPrev(null);

        --count;

        _totalSize -= rsrc.getTotalTimedBuffersSize();
    }

    /**
     * Moved rsrc to end of linked-list.
     *
     * @param rsrc moved rsrc
     */
    public synchronized void moveToEnd(Rsrc rsrc)
    {
        if (tail == rsrc)
            return;

        // Removing buff from linked-list:
        if (rsrc.getPrev() != null)
            rsrc.getPrev().setNext(rsrc.getNext());
        if (rsrc.getNext() != null)
            rsrc.getNext().setPrev(rsrc.getPrev());
        if (head == rsrc)
            head = rsrc.getNext();
        if (tail == rsrc)
            tail = rsrc.getPrev();

        rsrc.setNext(null);
        rsrc.setPrev(null);

        // Adding buff to linked-list:
        if (tail == null)
        {
            head = rsrc;
            tail = rsrc;
            rsrc.setPrev(null);
            rsrc.setNext(null);
        }
        else
        {
            rsrc.setPrev(tail);
            tail.setNext(rsrc);
            rsrc.setNext(null);
            tail = rsrc;
        }
    }

    /**
     * When total size of all currently cached timed-buffer plus sizeOfNewTimedBuffer
     * is greater than maxCacheTotalSize, enough timed-buffers are freed to make room.
     *
     * When the last timed-buffer of any resource is freed, the resource itself is removed
     * from the cache-structure.
     *
     * When makeRoom needs to free timed-buffers,
     * it uses the following iterative algorithm:
     *      In each iteration (there are maximum 4 iterations), only the least recently accessed
     *      25% of the buffers are scaned (or all the buffer when there are less than 1000 buffers).
     *      Buffers are freed until usage of heap-size drops bellow lowerCacheTotalSize. First
     *      only scaned large resources are freed.Than only medium-sized resources are freed, and
     *      finally small rsrcs are freed.
     *
     * @param newTimedBufferSize the size of the new buffer that is going to be cached
     */
    private void makeRoom(int sizeOfNewTimedBuffer)
    {
        final int BUFFS_IN_ITERATION = count / 4;

        int iterNum = 0; // Curr iteration number.

        int unusedBuffsCount = 0;
        int unusedBuffsSize = 0;
        int largeBuffsCount = 0;
        int largeBuffsSize = 0;
        int mediumBuffsCount = 0;
        int mediumBuffsSize = 0;
        int smallBuffsCount = 0;
        int smallBuffsSize = 0;
        int tinyBuffsCount = 0;
        int tinyBuffsSize = 0;

        // Once a day removing not recently accessed buffers:
        long currentTime = System.currentTimeMillis();
        if (currentTime - _lastUnusedRemovalTime > DAY_IN_MILLIS)
        {
            _lastUnusedRemovalTime = currentTime;

            Rsrc rsrc = head;
            while (rsrc != null)
            {
                Rsrc nextRsrc = rsrc.getNext();
                if (!rsrc.isRecentlyUsed(currentTime))
                {
                    int rsrcSize = rsrc.getTotalTimedBuffersSize();
                    unusedBuffsCount++;
                    unusedBuffsSize += rsrcSize;
                    ServiceUtils.removeFromCache(rsrc.getVariant(), rsrc.getUrl(), _configData);
                    String versionUrl = rsrc.getVersionUrl();
                    if (versionUrl != null)
                        ServiceUtils.removeFromCache(rsrc.getVariant(), versionUrl, _configData);
                }
                 rsrc = nextRsrc;
            }
        }

        //
        // If used-heap grow above maxCacheTotalSize then we free
        // enough buffers so that used-heap drops bellow lowerCacheTotalSize:
        //
        if (_totalSize + sizeOfNewTimedBuffer > _configData.maxCacheTotalSize)
        {
            while (_totalSize + sizeOfNewTimedBuffer  > _configData.lowerCacheTotalSize)
            {
                iterNum++;

                final int scanSize = (count < BUFFS_IN_ITERATION ? count : BUFFS_IN_ITERATION);

                // The timed-buffer following the last scaned buffer in current iteration (null when all buffers are scaned):
                Rsrc lastRsrc = head;

                for (int i = 1 ; i < scanSize && lastRsrc != null ; i++)
                    lastRsrc = lastRsrc.getNext();

                // Free large timed-buffers:
                for (Rsrc currRsrc = head, nextRsrc = null ;
                        _totalSize + sizeOfNewTimedBuffer > _configData.lowerCacheTotalSize && currRsrc != lastRsrc ;
                        currRsrc = nextRsrc)
                {
                    nextRsrc = currRsrc.getNext();
                    int rsrcSize = currRsrc.getTotalTimedBuffersSize();
                    if (rsrcSize > MIN_LARGE_BUFFER && !currRsrc.isBeingLoaded())
                    {
                        largeBuffsCount++;
                        largeBuffsSize += rsrcSize;
                        ServiceUtils.removeFromCache(currRsrc.getVariant(), currRsrc.getUrl(), _configData);
                        String versionUrl = currRsrc.getVersionUrl();
                        if (versionUrl != null)
                            ServiceUtils.removeFromCache(currRsrc.getVariant(), versionUrl, _configData);
                    }
                }

                // Free medium-sized timed-buffers:
                for (Rsrc currRsrc = head, nextRsrc = null ;
                        _totalSize + sizeOfNewTimedBuffer > _configData.lowerCacheTotalSize && currRsrc != lastRsrc ;
                        currRsrc = nextRsrc)
                {
                    nextRsrc = currRsrc.getNext();
                    int rsrcSize = currRsrc.getTotalTimedBuffersSize();
                    if (rsrcSize > MIN_MEDIUM_BUFFER && !currRsrc.isBeingLoaded())
                    {
                        mediumBuffsCount++;
                        mediumBuffsSize += rsrcSize;
                        ServiceUtils.removeFromCache(currRsrc.getVariant(), currRsrc.getUrl(), _configData);
                        String versionUrl = currRsrc.getVersionUrl();
                        if (versionUrl != null)
                            ServiceUtils.removeFromCache(currRsrc.getVariant(), versionUrl, _configData);
                    }
                }

                // Free small-sized timed-buffers:
                for (Rsrc currRsrc = head, nextRsrc = null ;
                        _totalSize + sizeOfNewTimedBuffer > _configData.lowerCacheTotalSize && currRsrc != lastRsrc ;
                        currRsrc = nextRsrc)
                {
                    nextRsrc = currRsrc.getNext();
                    int rsrcSize = currRsrc.getTotalTimedBuffersSize();
                    if (rsrcSize > MIN_SMALL_BUFFER && !currRsrc.isBeingLoaded())
                    {
                        smallBuffsCount++;
                        smallBuffsSize += rsrcSize;
                        ServiceUtils.removeFromCache(currRsrc.getVariant(), currRsrc.getUrl(), _configData);
                        String versionUrl = currRsrc.getVersionUrl();
                        if (versionUrl != null)
                            ServiceUtils.removeFromCache(currRsrc.getVariant(), versionUrl, _configData);
                    }
                }

                // Free remaining timed-buffers:
                for (Rsrc currRsrc = head, nextRsrc = null ;
                        _totalSize + sizeOfNewTimedBuffer > _configData.lowerCacheTotalSize && currRsrc != lastRsrc ;
                        currRsrc = nextRsrc)
                {
                    nextRsrc = currRsrc.getNext();
                    if (!currRsrc.isBeingLoaded())
                    {
                        tinyBuffsCount++;
                        tinyBuffsSize += currRsrc.getTotalTimedBuffersSize();
                        ServiceUtils.removeFromCache(currRsrc.getVariant(), currRsrc.getUrl(), _configData);
                        String versionUrl = currRsrc.getVersionUrl();
                        if (versionUrl != null)
                            ServiceUtils.removeFromCache(currRsrc.getVariant(), versionUrl, _configData);
                    }
                }
            }
        }

        /*
        if (unusedBuffsCount + smallBuffsCount + mediumBuffsCount + largeBuffsCount + tinyBuffsCount > 0)
        {
            StringBuilder sb = new StringBuilder(128);
            sb.append("MakeRoom: unused-resources: ").append(unusedBuffsCount);
            sb.append(" (").append(unusedBuffsSize/(1024*1024)).append(" Mbytes), ");
            sb.append(", large-resources: ").append(largeBuffsCount);
            sb.append(" (").append(largeBuffsSize/(1024*1024)).append(" Mbytes), ");
            sb.append(", medium-resources: ").append(mediumBuffsCount);
            sb.append(" (").append(mediumBuffsSize/(1024*1024)).append(" Mbytes), ");
            sb.append(", small-resources: ").append(smallBuffsCount);
            sb.append(" (").append(smallBuffsSize/(1024*1024)).append(" Mbytes).\n");
            sb.append(", tiny-resources: ").append(tinyBuffsCount);
            sb.append(" (").append(tinyBuffsSize/(1024*1024)).append(" Mbytes).\n");
        }
        */
    }

}
