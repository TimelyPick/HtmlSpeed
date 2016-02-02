package com.htmlspeed.server;

public class LoadLock
{
    private int _count;

    private boolean _isError = false;

    public void waitUntilCountIs0() throws InterruptedException
    {
        synchronized (this)
        {
            while (_count > 0)
                wait();
        }
    }

    public int getCount()
    {
        synchronized (this)
        {
            return _count;
        }
    }

    public void setCount(int count)
    {
        synchronized (this)
        {
            _count = count;
        }
    }

    public void incCount()
    {
        synchronized (this)
        {
            ++_count;
        }
    }

    public void decCount()
    {
        synchronized (this)
        {
            --_count;
            if (_count <= 0)
                    notify();
        }
    }

    public boolean isError()
    {
        return _isError;
    }

    public void setError(boolean isError)
    {
        _isError = isError;
    }

}
