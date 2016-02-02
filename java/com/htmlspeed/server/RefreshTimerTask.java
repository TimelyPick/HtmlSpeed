/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * Created on 28 December 2012
 */
package com.htmlspeed.server;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;

/**
 * RefreshTimerTask
 *
 * Periodically refreshes state-less pages and loads once
 * state-full pages (that listed in config-file "refresh.txt").
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.1 $$Date: 2012/12/28 13:35:30 $
 */
public class RefreshTimerTask extends TimerTask
{
    /**
     * Full url of refreshed page.
     */
    private String _url;

    /**
     * Used thread-pool (allocated by ConfigUtils).
     */
    private ExecutorService _pool;

    /**
     * The single instance of HtmlSpeedServlet.
     * Initialized by HtmlSpeedServlet.init().
     */
    private static HtmlSpeedServlet _servlet;

    /**
     * Calls HtmlSpeedServlet.service from a thread taken from the pool.
     */
    private Refresher _refresher;

    /**
     * Called by HtmlSpeedServlet.init().
     *
     * @param servlet the single instance of HtmlSpeedServlet
     */
    public static void setServlet(HtmlSpeedServlet servlet)
    {
        _servlet = servlet;
    }

    /**
     * CONSTRUCTOR
     *
     * @param url full url of loaded/refreshed page
     * @param pool used thread-poll
     */
    public RefreshTimerTask(String url, ExecutorService pool)
    {
        _url = url;
        _pool = pool;
        _refresher = new Refresher(_url);
    }

    @Override
    public void run()
    {
        _pool.execute(_refresher);
    }

    /**
     * Its run() method calls _servlet.service(request, response)
     * for loading/refreshing a statefull/stateless page.
     */
    private static class Refresher implements Runnable
    {
        private String _url;

        public Refresher(String url)
        {
            _url = url;
        }

        @Override
        public void run()
        {
            try
            {
                RefreshServletRequest request = new RefreshServletRequest(_url);
                RefreshServletResponse response = new RefreshServletResponse();
                _servlet.service(request, response);
            }
            catch (Exception exc)
            {
                exc.printStackTrace();
            }
        }
    }
}
