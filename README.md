# HtmlSpeed

Html-Speed is a **reverse proxy** written in **Java**.<br>
It is used for **accelerating performance of websites** by<br>
improving **page load speed** and **reducing load on webservers**.<br>
This is achieved by applying **front-end optimizations** to both<br>
static and dynamic content and by **optimizing the caching of resources**.<br>
For a list of features and deployment options, see the
[introductory blog post](https://www.timelypick.com/blog/2016/02/htmlspeed-first-release).<br><br>


<img src="https://cloud.githubusercontent.com/assets/6310421/12713473/9b31590c-c8d7-11e5-9ec5-065b62b34331.png" height="150" alt="html-speed-logo">

## HtmlSpeed Front-End optimization<br>software for faster website load

Did you know?

According to Google:

* More than 80% of end-user response time is spent on the frontend
* 500 MS load time slower = 20% drop in traffic
* Fast and optimized pages lead to higher visitor engagement, retention, and conversions.

The importance of fast websites load is even more critical when visitors are browsing from mobile devices, because mobile devices have slower connections with higher latency.

Perceiving website load speed as a “Slow load”, affects the actual end-user experience and the website’s performance.

Google, Yahoo, and Amazon – all agree that a slow website has a negative impact on the site performance, and that speed matters.

### By accelerating a website you:

* Increase number of page views
* Make visitors more engaged
* Gain more returning visitors
* Increase conversion rate
* Decrease bounce rate
* Improve search engine results and ranking

### HtmlSpeed:

| <img style="border-style: none;" alt="load" src="https://cloud.githubusercontent.com/assets/6310421/12713971/84ab7070-c8da-11e5-8c1d-66f0a5b21c60.jpg" style="float:left;" width="55" height="55"> | <img style="border-style: none;" alt="load" src="https://cloud.githubusercontent.com/assets/6310421/12713974/88fa7d4c-c8da-11e5-94f3-7e0698c962ad.png" style="float:left;" width="55" height="55"> | <img style="border-style: none;" alt="load" src="https://cloud.githubusercontent.com/assets/6310421/12713976/8db9edc2-c8da-11e5-83d1-8f95cc446daf.png" style="float:left;" width="55" height="55"> |
| --- | --- | --- |
| Improves page<br>load speed by<br>tens of percents | Increases mobile<br>browsing speed<br>by tens of percents| Reduces load<br>on servers by<br>tens of percents |

### Why preferring HtmlSpeed over other front end accelerators to speed up websites?

* Automatic on-the-fly acceleration
* Web site contents are fully preserved
* The integrity of a website is never compromised
* Website speed acceleration for domain with unlimited number of web pages / surfers / bandwidth / servers / requests
* Fully scalable – unlimited grow
* Supports all common web servers
* No dependence on our infrastructure
* No need to reveal website security certificate
* Easy to uninstall
* HtmlSpeed is based on a self developed parser and has unique techniques that you won’t find in other front-end optimizations
* Supports all major browsers:

  <img style="border-style: none;" alt="chrome-logo" src="https://cloud.githubusercontent.com/assets/6310421/12713931/3e2469f4-c8da-11e5-9434-693431bfc98c.png" style="float:left;" width="55" height="55"><img style="border-style: none;" alt="ie-logo" src="https://cloud.githubusercontent.com/assets/6310421/12713936/46459342-c8da-11e5-9836-eb61d6ee223c.png" style="float:left;" width="55" height="55"><img style="border-style: none;" alt="safari-logo" src="https://cloud.githubusercontent.com/assets/6310421/12713947/5718f6f0-c8da-11e5-90df-faeb53297118.png" style="float:left;" width="55" height="55"><img style="border-style: none;" alt="opera-logo" src="https://cloud.githubusercontent.com/assets/6310421/12713952/5af0b59c-c8da-11e5-9a46-993e11550579.png" style="float:left;" width="55" height="55"><img style="border-style: none;" alt="firefox-logo" src="https://cloud.githubusercontent.com/assets/6310421/12713954/5f0b0d08-c8da-11e5-82a9-c226c41ade9d.png" style="float:left;" width="55" height="55">

### What does HtmlSpeed accelerates?

  HtmlSpeed automatically shortens web page load time for:

  * Stateful and stateless web pages
  * Secured (SSL) and non secured web pages. (Http and Https)
  * “Get” and “Post” methods
  * All local web server resources and part of external resources.

### How HtmlSpeed accelerates website’s speed?

HtmlSpeed exclusively implements three front end optimizations (FEO) techniques:

* Gradually declining inline optimization – dramatically decreasing the<br>number of requests while preserving bandwidth usage
* Content first optimization – Start page rendering before Java scripts (JS)
* Unmodified state-full pages detection – Status 304 is returned to browser

HtmlSpeed also improves website performance by implementing some common optimization techniques:

* Image compression – JPEG files
* Cache optimization – filename versioning (CSS, JavaScript, images)
* Gzip compression – except for images

## Configuration-files:

	All configuration-files are stored in the directory htmlspeed.

	HtmlSpeed servers check once each 30 seconds if any configuration file has changed,
	by using the last-update time in the file-system. They handle changes on the fly.

	When you run more than one HtmlSpeed server, and you decide to change any
	configuration-file, you should do so in all HtmlSpeed server machines.

### license.dat

	This configuration file contains the license controling which domains/sub-domains are allowed
	to be service by HtmlSpeed. You can't modify this file. When you need to change/extend the
	license, you will have to generate another file. The license mecahnism has been developed for
	protecting HtmlSpeed software from beeing used to accelarate unauthorized domains.

	When your domain www.yyy.com is services by HtmlSpeed servers, and web-pages in that domain
	use images, java-scripts, etc' from domain www.zzz.com, then you don't need license for domain
	www.zzz.com. License for www.zzz.com is only required when domain www.zzz.com is serviced
	by HtmlSpeed servers.

	When the issued license specifies "others" as a licensed domain in the list of authorized domains
	then http-requests directed to unlicensed domains are routed (but not optimized) using the routing-
	info for domain "others" in hostinfo.txt (see bellow).

### hostinfo.txt

	This configuration file contains exactly a single logical line (that can be splitted into several lines).

	It is used for mapping each domain/sub-domain that is serviced by the current HtmlSpeed server
	to one or more ip-addresses of original web-servers (private ip-addresses are prefered), and
	optionaly specify the weight of each server.

	A domain starting with '.' represents a default for all sub-domains of the domain that follows.
	For example ".aaa.com" will match "www.aaa.com" and "bbb.aaa.com" but not "aaa.com".

	The domain "others" is used for defining routing-information for unlicensed domains.
	HtmlSpeed routes requests for unlicensed domains (without optimizing them) only when
	license.dat lists others as an authorized domain (otherwise error 500 is returned).

  ```
  [withfirstplus/]www.yyy.com,ipAddress1[-w1],ipAddress2[-w2],zzz.yyy.com,ipAddress3[-w3],www.sss.com,ipAddress4[-w4][/]

	[-wNNN]
		Optional original-server weight (its relative computing strenth).
		When supplied, must be an integer in the range 1..127.
		Defaulted to 1, when not supplied.

	ipAddressNNN
		Format: ip-address[:htmlPort:sslPort]
		Private IP addresses are prefered over public IP addresses.
		When no ports are specified, HtmlSpeed uses ports 80 and 443.
		A colon ':' preceeds each specified port.
		HtmlSpeed server balances the load between original-servers, using weighted round-robin algorithm.
		When session sticky-cookies are used, then HtmlSpeed server routes requests belonging to a session
		to the original-server who created the session.

	domains/sub-domains:
		A domain starting with '.' represent all subdomains of the domain/sub-domain that follows.
		Domains starting with '.' should be listed after sub-domains that are routed to other ip-addresses
		(thus they specify default mapping for remaining sub-domains).

		The domains ttt.com and www.ttt.com should separately be listed in hostinfo.txt

	[/]
		Only when the line end with '/' then content-first optimization will be applied to
		web-pages listed in content-first.txt (see: below).

	[withfirstplus/]
		When jumping from a web-page to another web-page in the same site, HtmlSpeed servers
		don't inline the largest images, style-sheets, java-scripts. That's because usualy style-sheets
		and java-scripts are shared between pages in the site, and when browsing the site these files
		are cached from visiting previous pages in the web-site. This behaviour preserves bandwidth.

		When specified, the withfirstplus option tells HtmlSpeed server that when jumping from a web-
		page to another web-page in the same site to use first-plus visit, which means that only largest
		java-script and style-sheets are not inlined but larger images are inlined. Otherwise the larger
		images will not be inlined. This option should be specified, when large images are not shared
		between web-pages of the web-site.
  ```

	When a running HtmlSpeed server detects that the file hostinfo.txt has changed, it adapts to the changes.
	The in-memory cache is not cleared, thus you can freely change the hostinfo.txt (without experiencing
	temporary performance degradation).

### state-full.txt

	This configuration file selects files (resources) that are forced to be state-full.
	A state-full resource is not cached by HtmlSpeed server (because each browser
	may receive specialized content, or because the resource returns a session-cookie).
	State-full resource are never inlined in their containers, because they must separately
	be fetched by the browser.

	HtmlSpeed automaticaly detects state-full resources, using http-headers returned
	by the original web-server (such as Cache-Control and Last-Modified headers). All
	resources that can only be cached for less than htmlspeed.min.maxage seconds
	are automaticaly state-full.

	In the state-full.txt configuration file you can force files (resources) to be state-full,
	to prevent them from being inlined or cached by HtmlSpeed servers. Usualy this is
	not required. Thus, the existance of this configuration-file is optional.

	File-format (by example):
		11
		html
		/Img/1.jpg
		*k.jpg
		*imgs*
		http://www.other.com/*.jpg

	The first line contains a version number (you should increase this number when changing the file).
	Each other line selects resources that are forced to be state-full. Up to two '*' can be used, for selecting
	group of resources (example: *k.jpg means all resoources that end with k.jpg). Resources beginning
	with '/' are resources from current web-site. Resources from other sites begins with "http://" or "https://".

	When a web-page is defined to be statefull, then no optimizations are applied to the page.

	A row whos content is "html" declares all html pages (having content-type "text/html") to
	be statefull (not cached by HtmlSpeed). Html pages that are not selected by other rows in
	state-full.txt and are statefull only because of the "html" declaration are fully optimize.

	When a running HtmlSpeed server detects that state-full.txt has changed it clears its memory cache,
	and adapts to the changes. This may cause some performance-degradation for a short time, until
	the in-memory cache is rebuilt.

### state-less.txt

	This configuration file selects files (resources) that are forced to be state-less.
	State-less resources are cached by HtmlSpeed server and can be inlined in their
	containers.

	HtmlSpeed automaticaly detects state-less resources, using http-headers returned by
	the original web-server (such as Cache-Control and Last-Modified headers).

	In the state-less.txt configuration file you can force files (resources) to be state-less,
	to enable them to be inlined and cached by HtmlSpeed servers. Usualy this is not
	required. Thus, the existance of this configuration-file is optional.

	Some web-sites erroneously return "private" in the Cache-Control response headers
	of all fetched images/css/java-scripts. This causes HtmlSpeed to make all resources
	state-full and thus no optimizations are allowed. In the state-less.txt configuration file
	you can force images, css, java-scripts that should be state-less to be state-less.
	This enables HtmlSpeed to optimize web-pages containing these resources.

	HtmlSpeed servers never forces resources that generate session-cookies and those
	that are explicitely configured to be state-full to become state-less, even when they
	are selected by state-less.txt.

	A row whos content is "html" declares all html pages (having content-type "text/html")
	to be stateless (cached by HtmlSpeed).

	When a running HtmlSpeed server detects that state-less.txt has changed it clears its memory cache,
	and adapts to the changes. This may cause some performance-degradation for a short time, until
	the in-memory cache is rebuilt.

	File-format: same as state-full.txt file-format.	

### content-first.txt

	This configuration file selects web-pages that are content-first optimized.

	When a content-first optimized web-page is displayed, an iframe occupying the
	entire window is opened above the page. In the iframe the page is rendered
	without executing the Java-Scripts. Thus the Java-Scripts don't	delay the
	display of text and images in the page. In the meantime bellow the displayed
	iframe the web-page is fully prepared (its java-script are executed). When the
	DOM of the full page is ready, then the iframe is removed and the full page is
	revealed. This improves the user-experience because the content of the web-page 
	is displayed as soon as possible, and the commercials are displayed when ready.

	The configuration file usualy list only web-pages and java-script resources.
	The web-pages must begin with "http://" or '*'.

	Content-first optimization may only be applied to top-level web-pages. It is not
	allowed for web-pages that are originaly displayed inside iframes.

	When content-first optimized web-pages uses java-scripts that modify the "location"
	of the web-page (navigating to other pages via java-script instead of hyper-links),
	then these java-scripts should also be listed in the content-first.txt file.

	Using content-first optimization will improve speed only when long-running java-scripts
	in the web-page delay the display of text/images for too long.

	Content-first optimization can't be used when the web-page contains java-scripts
	that act differently when the page is displayed inside an iframe, because content-
	first optimization do put the web-page inside an iframe !!!!

	This file is only relevant when content-first optimization is enabled by hostinfo.txt.

	When a running HtmlSpeed server detects that content-first.txt has changed it clears its memory cache,
	and adapts to the changes. This may cause some performance-degradation for a short time, until
	the in-memory cache is rebuilt.

	File-format: Same as state-full.txt file-format with the exception that only full
		    url should be spedified for web-pages (starting with "http://" or '*').

### no-inline.txt

	This configuration file selects images that should not be inlined in their containing style-sheets.

	This configuration file is used when many images are referenced by a style-sheet but only a
	small number of background images are actualy used. Inlining these images causes a waste
	of bandwidth, because most of the inlined images will not be used by the browser. On most
	web-sites there is no such problem. Thus, the existance of this configuration-file is optional.

	File-format: same as state-full.txt file-format.

### auto-refreshed.txt

	Contains a list of state-less pages that are periodicaly refreshed by HtmlSpeed
	Example:
```
	    60  http://www.kuku.com/
	    120 http://www.kuku.com/news
```

  In the example, the home-page of website ```www.kuku.com``` is auto-refreshed
  each 60 seconds. The /news page is refreshed each 120 seconds.

## properties.txt

	Each line in the properties file contains: 
				property-name    property-value

	The value is separated from the name by at least one blank or tab.

	remove.from.html
		When specified, each occurence of the string is removed from all html pages.
		Used mainly for removing port number of original webserver (example: 8081).

	debug
		When true (default), debug information is written to log (standard-output).

	min.huge
		Minimum size in bytes of huge files (default 64K bytes). Huge files are never inlined.

	min.large
		Minimum size in bytes of large files (default 32K bytes). Large files are only inlined in first-vist.

	min.medium
		Minimum size in bytes of medium files (default 8K bytes). Medium files are inlined in first
		and second visits.

	min.small
		Minimum size in bytes of small files (default 200 bytes). Small files are inlined in first
		second and third visits.

	redirects
		When 0 (default), http status 301(Moved Permanently) are routed back to browser.
		When 1, HtmlSpeed server fetches the resource from redirected to location (used
		by some websites for redirecting resources to cdn without mapping a subdomain
		(not using DNS).

	delay.iframe
		When false (default), HtmlSpeed replaces content-first pages by loaded iframe
		as soon as possible. When true, replacement by iframe is delayed to enable
		javascripts (that clears the page before building it) to be executed in the background.

	ssl.localhost
		When true (the default) HtmlSpeed invokes services on original webserver using
		https, when https is used by the browser and they are both on same machine.

	ssl.protocols
		Selects the SSL protocol/s that is/are used by the web-server that host the original web-site.
		In the above example the protocol is TLS version 1.0. The protocol that is used by the
		original web-server can be found by surfing to a secured section in the original web-site,
		using chrome, and clicking on the locker-icon in the address bar.

	ssl.trustall
		When false (default), content server is only trusted by HtmlSpeed when having a valid certificate
		that is signed by a trusted certificate-authority. When true, then content server is always trusted.

	session.cookies
		A comma separated list of names of session-cookies that are used by the original web-server
		for identifying sessions with browsers. HtmlSpeed uses these cookies when routing incoming browser
		requests to the specific original-server who created the session with the requesting browser. Additionaly,
		resouces generating these cookies (session starters) are forced to be state-full (can't be inlined).

	proxy.headers
		When true then HtmlSpeed adds X-Forwarded-For and X-Real-IP headers to http-requests (when missing).

	fixed.maxage
		When false (default), the value of max-age header that is returned to the browser
		is relative (current-time minus load-from-website time is subtracted from it).
		When true, the value of returned max-age is fixed as long as the resource is fresh.

	min.maxage
		The minumum value of max-age of state-less resources (default is 420 meaning 7 minutes).
		When max-age is smaller the resource is assumed to be state-full, unless forced to be state-
		less by state-less.txt. When a resource having smaller max-age is forced to be state-less its
		max-age header changes to htmlspeed.min.maxage.

	max.maxage
		The maximum number of seconds that a state-less resource is allowed to be cached by
		HtmlSpeed servers. The default is 0 which means that the max-age returned by original
		server when resource has been loaded limits the maximum allowed caching time.

	page.maxage
		When >= 0 then page.maxage forces an upper limit on the value of returned max-age for
		state-less (cached) pages.

	variants
		Limiting inline-degree (visits) for state-less and state-full pages.
		The value is 2 or 4 single-digit numbers separated by commas:
			min-state-less,max-state-less[,min-state-full,max-state-full]
		Each value is in the range: 0..4
		0: first-visit, 1: first-plus-visit, 2: second-visit, 3: third-visit, 4: forth-visit.
		When min and max visit numbers for state-full pages are not supplied
		they are defaulted to 0,4 (no limit is forced).

		When state-less pages are routed through a CDN min-state-less
		should eq	ual max-state-less (0: means aggressive inline, and 4:
		means no-inline). When min-state-less and max-state-less differ,
		the key "private" is added to the "Cache-Control" header of the
		response.

		Inline-degree can also be limited for state-full pages to preserve
		bandwidth. Usualy when resources are served by CDN then 2'nd
		visit will be used, so that larger resources will not be inlined but
		will be fetched from CDN.

	versioning
		When true (default is false) automatic file-versioning optimization is enabled.
		Important: file-versioning should not be enabled when some but not all machines
		in the farm run HtmlSpeed server, because files that are referenced by versioned
		file-names can't be served by the original web-servers.

	ie.min.content.first
		The smallest IE browser version for whom content-first optimizations are applied (default is 9).

	base.target.parent
		When true (default is true) the head section of content-first pages contains
		<base target='_parent'>

	max.threads
		Maximum number of threads (default is 2000) that HtmlSpeed can use for processing
		responses from the original web-servers (host).

	max.connections
		Maximum number of connections (default is 2000) that HtmlSpeed is allowed to use
		for accessing the original web-servers (host).

	connect.timeout
		Maximum number of milliseconds (default is ) that HtmlSpeed waits for connections
		with original web-server (host) to be established.

	timeout
		Maximum number of milliseconds (default is 30000) that HtmlSpeed waits for
		receiving responses from the original web-servers (host).

	idle.timeout
		The number of milliseconds (default is 300000) before idle connections with
		the original web-server (host) are closed.

	cdn-1
	cdn-2
	...
		Format:
			src-domain,min-size,max-size,cdn-domain[,cdn-ssl-domain]
		min-size and max-size are in Kbytes.
		The CDN rule defines that each resource from domain src-domain whos size is
		between min-size and max-size should be served using the domain cdn-domain
		when using http and cdn-ssl-domain when using https (that is assumed to be
		routed through a CDN). When cdn-ssl-domain is not supplied the same domain
		is used for both http and https.

	hosts.map
		When specified contains: srcHost1,dstHost1,srcHost2,dstHost2, ... , srcHostn,dstHostn
		HtmlSpeed routes http requests from srcHosti to dstHosti
		Mainly used for debuggin purposes.

	jpeg.min
		Minimum size in K-bytes of jpeg images to be optimized
		(when specified then jpeg.quality should also be specified).

	jpeg.quality
		Quality of optimized jpeg images range:  0.0 .. 1.0
		Usualy 0.8 is good enough.

	mobile.home
		When true (default is false) then the home-page is state-full for mobile devices including iPad)

	replace.src
		The string whos first-occurence in html pages is to be replaced by a near dst. The first-occurence
		of src is replaced by: replace.before (when specified), followed by replacing-dst exculding its first
		replace.dst.skip characters (when replace.dst.prefix and replace.dst.suffix are specified), followed
		by replace.after (when specified).

	replace.dst.prefix
		The start (prefix) of the replacing string (when null no replace-dst is used)

	replace.dst.suffix
		The end (suffix) of the replacing string

	replace.dst.skip
		The number of characters from start of dest string that are skipped when
		replacing the src string (default is 0).

	replace.before
		The string to insert before the replaced string (optional)

	replace.after
		The string to insert after the replaced string (optional)

	filen.suffix
	filen.replace.xxx
		Same as replace.xxx for the file whos url ends with filen.suffix (the file can be
		any file not just html page (note: replace.xxx are only applied to html-pages).

## Example

	Download the file HtmlSpeedExample1.zip from release 1.1
	Unzip it directly under c: (on Windows operating system).
	The zip-file contains a ready-to-run Jetty webserver and
	all configuration files that are needed	to enhance the
	website www.buzzfeed.com (just an example).

	HtmlSpeed is a Java web-application. It can be deployed to the open-source and freeware Java web-server
	named Jetty. The Jetty web-server is part of the open-source eclipse project (see: http://eclipse.org/jetty)
	that is sponsered by IBM. Eclipse is largly used by the Java community.

	To route the domain www.buzzfeed.com to your localhost, add the
	following line to the file C:\Windows\System32\drivers\etc\hosts:
		127.0.0.1		www.buzzfeed.com
	After that open a cmd window and type: ipconfig /flushdns
	After that restart the browser.
	This will cause your local browser to access the HtmlSpeed server that is running on localhost.
	
	To start the HtmlSpeed server:

		cd c:\jetty-8.1.5
		java -Xmx3g -Dorg.apache.jasper.compiler.disablejsr199=true -jar start.jar

	-Xmx3g
		Means that 3 Giga-bytes is the MaXimum heap-size (memory) that can be allocated by the web-server.
		This parameter should be set to be 1 giga-byte less than the amount of phisical memory. For example
		when the server has 4 giga-bytes of phisical memory then 3 giga-bytes should be allocated to the
		web-server. Larger memory will enable HtmlSpeed to cache more resources in its memory.

	-Dorg.apache.jasper.compiler.disablejsr199=true
		To enable serving JSP files.

	-Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n
		Only when you want to debug the webserver.

	-jar start.jar
		The standard way to start the Jetty web-server.

	The deployed HtmlSpeed web-application is located at:
		C:\jetty-8.1.5\webapps\HtmlSpeedServlet.war.

	When a page is accessed through HtmlSpeed for the first time, it is not
	accelerated. Thus you should clear the browser cache and browse again to
	buzzfeed to experience a performance boost.

	Jetty can also be executed on Linux.

	To stop HtmlSpeed you have to "kill" the java process running the web-server.

## License

HtmlSpeed was originally written by [Eldad Zamler](https://www.timelypick.com/play-solitaire) and is
licensed under the [MIT license](LICENSE).
