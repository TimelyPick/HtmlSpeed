/**
 *  Copyright 2011 Galiel 3.14 Ltd. All rights reserved.
 *  Use is subject to license terms.
 *
 * HtmlAnalyzer.java
 *
 * Created on 28 Feb 2012
 */
package com.htmlspeed.server;

import java.util.HashMap;

/**
 * HtmlAnalyzer
 *
 * Analyzes html and style-sheet documents.
 * Returns the resoult in an efficient int-array.
 *
 * @author  Eldad Zamler
 * @version $Revision: 1.34 $$Date: 2013/07/15 14:54:28 $
 */
public class HtmlAnalyzer
{
    public static final int DUPLICATES_INFO = 1; // For resources with check for duplicates.

    //
    // Output for <script ...> ... </script> selected by SCRIPT_KIND:
    //
    public static final int SCRIPT_START_FIRST = 2;
    public static final int SCRIPT_SRC_FIRST = 3;
    public static final int SCRIPT_SRC_LAST = 4;
    public static final int SCRIPT_TYPE_FIRST = 5;  // when specified should be: text/javascript
    public static final int SCRIPT_TYPE_LAST = 6;
    public static final int SCRIPT_START_LAST = 7;
    public static final int SCRIPT_END_FIRST = 8;
    public static final int SCRIPT_END_LAST = 9;
    public static final int SCRIPT_IS_DYNAMIC = 10; // 1 means true, 0 means false
    public static final int SCRIPT_IN_IE_COMMENT = 11;
    public static final int SCRIPT_INFO_LEN = 12;

    /**
     * Output for LINK tag selected by LINK_KIND:
     */
    public static final int LINK_FIRST = 2;
    public static final int LINK_LAST = 3;
    public static final int LINK_HREF_FIRST = 4; // url of css file
    public static final int LINK_HREF_LAST = 5;
    public static final int LINK_TYPE_FIRST = 6; // when specified should be: text/css
    public static final int LINK_TYPE_LAST = 7;
    public static final int LINK_REL_FIRST = 8; // should be: stylesheet
    public static final int LINK_REL_LAST = 9;
    public static final int LINK_MEDIA_FIRST = 10; // screen, all, ...
    public static final int LINK_MEDIA_LAST = 11;
    public static final int LINK_IN_IE_COMMENT = 12;
    public static final int LINK_INFO_LEN = 13;

    /**
     * Output for STYLE start-tag selected by STYLE_START_KIND:
     */
    public static final int STYLE_START_FIRST = 1;
    public static final int STYLE_START_LAST = 2;
    public static final int STYLE_START_INFO_LEN = 3;

    /**
     * Output for css-image selected by CSS_IMAGE_KIND:
     */
    public static final int CSS_IMG_FIRST = 2;
    public static final int CSS_IMG_LAST = 3;
    public static final int CSS_IMG_INFO_LEN = 4;

    /**
     * Output for STYLE end-tag selected by STYLE_END_KIND:
     */
    public static final int STYLE_END_FIRST = 1;
    public static final int STYLE_END_LAST = 2;
    public static final int STYLE_END_INFO_LEN = 3;

    /**
     * Output for IMG tag selected by IMG_KIND:
     */
    public static final int IMG_SRC_FIRST = 2;
    public static final int IMG_SRC_LAST = 3;
    public static final int IMG_INFO_LEN = 4;

    /**
     * Output for FRAME tag selected by FRAME_KIND:
     */
    public static final int FRAME_SRC_FIRST = 1;
    public static final int FRAME_SRC_LAST = 2;
    public static final int FRAME_INFO_LEN = 3;

    /**
     * Output for IFRAME tag selected by IFRAME_KIND:
     */
    public static final int IFRAME_FIRST = 1;
    public static final int IFRAME_LAST = 2;
    public static final int IFRAME_SRC_FIRST = 3;
    public static final int IFRAME_SRC_LAST = 4;
    public static final int IFRAME_INFO_LEN = 5;

    /**
     * Output for HEAD tag selected by HEAD_KIND:
     */
    public static final int HEAD_START_FIRST = 1;
    public static final int HEAD_START_LAST = 2;
    public static final int HEAD_INFO_LEN = 3;

    /**
     * Output for BODY tag selected by BODY_KIND:
     */
    public static final int BODY_START_FIRST = 1;
    public static final int BODY_START_LAST = 2;
    public static final int BODY_ONLOAD_FIRST = 3;
    public static final int BODY_ONLOAD_LAST = 4;
    public static final int BODY_INFO_LEN = 5;

    /**
     * Output for A tag selected by A_KIND:
     */
    public static final int A_START_FIRST = 1;
    public static final int A_START_LAST = 2;
    public static final int A_TARGET_FIRST = 3;
    public static final int A_TARGET_LAST = 4;
    public static final int A_HREF_FIRST = 5;
    public static final int A_HREF_LAST = 6;
    public static final int A_INFO_LEN = 7;

    /**
     * Output for INPUT tag (when type is 'image') selected by INPUT_KIND:
     */
    public static final int INPUT_FIRST = 2;
    public static final int INPUT_LAST = 3;
    public static final int INPUT_SRC_FIRST = 4;
    public static final int INPUT_SRC_LAST = 5;
    public static final int INPUT_INFO_LEN = 6;

    /**
     * Output for EVENT
     */
    public static final int EVENT_FIRST = 1;
    public static final int EVENT_LAST = 2;
    public static final int EVENT_INFO_LEN = 3;

    /**
     * Output for A tag selected by A_KIND:
     */
    public static final int FORM_START_FIRST = 1;
    public static final int FORM_START_LAST = 2;
    public static final int FORM_TARGET_FIRST = 3;
    public static final int FORM_TARGET_LAST = 4;
    public static final int FORM_INFO_LEN = 5;

    /**
     * Output for Base tag selected by BASE_KIND:
     */
    public static final int BASE_HREF_FIRST = 1;
    public static final int BASE_HREF_LAST = 2;
    public static final int BASE_INFO_LEN = 3;

    /**
     * Kinds of seuquences of entries in the output-int-array:
     */
    public static final int SCRIPT_KIND = 0;
    public static final int LINK_KIND = 1;
    public static final int STYLE_START_KIND = 2;
    public static final int CSS_IMG_KIND = 3;
    public static final int STYLE_END_KIND = 4;
    public static final int IMG_KIND = 5;
    public static final int FRAME_KIND = 6;
    public static final int IFRAME_KIND = 7;
    public static final int HEAD_KIND = 8;
    public static final int BODY_KIND = 9;
    public static final int A_KIND = 10;
    public static final int INPUT_KIND = 11;
    public static final int EVENT_KIND = 12;
    public static final int FORM_KIND = 13;
    public static final int BASE_KIND = 14;
    public static final int EOF_KIND = (-1); // Last element in output-int-array.

    /**
     * INFO_LENS[i] is the number of element
     * in the output-int-array for the i'th kind.
     */
    public static final int[] INFO_LENS = {
            SCRIPT_INFO_LEN,
            LINK_INFO_LEN,
            STYLE_START_INFO_LEN,
            CSS_IMG_INFO_LEN,
            STYLE_END_INFO_LEN,
            IMG_INFO_LEN,
            FRAME_INFO_LEN,
            IFRAME_INFO_LEN,
            HEAD_INFO_LEN,
            BODY_INFO_LEN,
            A_INFO_LEN,
            INPUT_INFO_LEN,
            EVENT_INFO_LEN,
            FORM_INFO_LEN,
            BASE_INFO_LEN
            };

    private static final char[] IMG = new char[]{'<', 'i', 'm', 'g'};
    private static final char[] LINK = new char[]{'<', 'l', 'i', 'n', 'k'};
    private static final char[] REL_ATTR = new char[]{'r', 'e', 'l'};
    private static final char[] MEDIA_ATTR = new char[]{'m', 'e', 'd', 'i', 'a'};
    private static final char[] SRC_ATTR = new char[]{'s', 'r', 'c'};
    private static final char[] STYLE_ATTR = new char[]{'s', 't', 'y', 'l', 'e'};
    private static final char[] TYPE_ATTR = new char[]{'t', 'y', 'p', 'e'};
    private static final char[] TARGET_ATTR = new char[]{'t', 'a', 'r', 'g', 'e', 't'};
    private static final char[] STYLE_START = new char[]{'<', 's', 't', 'y', 'l', 'e'};
    private static final char[] STYLE_END = new char[]{'<', '/', 's', 't', 'y', 'l', 'e'};
    private static final char[] SCRIPT_START = new char[]{'<', 's', 'c', 'r', 'i', 'p', 't'};
    private static final char[] SCRIPT_END = new char[]{'<', '/', 's', 'c', 'r', 'i', 'p', 't'};
    private static final char[] HTML_COMMENT = new char[] {'<', '!', '-', '-'};
    private static final char[] BGD = new char[] {'b','a','c','k','g','r','o','u','n','d',':'};
    private static final char[] BGD_IMG = new char[] {'b','a','c','k','g','r','o','u','n','d','-', ' ', 'i','m','a','g','e',':'};
    private static final char[] URL = new char[] {'u','r','l'};
    private static final char[] FRAME = new char[] {'<', 'f', 'r', 'a', 'm', 'e'};
    private static final char[] IFRAME = new char[] {'<', 'i', 'f', 'r', 'a', 'm','e'};
    private static final char[] HEAD = new char[] {'<', 'h', 'e', 'a', 'd'};
    private static final char[] BODY = new char[] {'<', 'b', 'o', 'd', 'y'};
    private static final char[] BASE = new char[] {'<', 'b', 'a', 's', 'e'};
    private static final char[] META = new char[] {'<', 'm', 'e', 't', 'a'};
    private static final char[] ONLOAD_ATTR = new char[]{'o', 'n', 'l', 'o', 'a', 'd'};
    private static final char[] ONCLICK_ATTR = new char[]{'o', 'n', 'c', 'l', 'i', 'c','k'};
    private static final char[] ONMOUSE_ATTR = new char[]{'o', 'n', 'm', 'o', 'u', 's','e'};
    private static final char[] JAVA_SCRIPT = new char[] {'j', 'a', 'v', 'a', 's', 'c', 'r', 'i', 'p', 't', ':'};
    private static final char[] HREF_ATTR = new char[]{'h', 'r', 'e', 'f'};
    private static final char[] HTTP_EQUIV_ATTR = new char[]{'h','t','t','p','-','e','q','u','i','v'};
    private static final char[] CONTENT_TYPE = new char[]{'c','o','n','t','e','n','t','-','t','y','p','e'};
    private static final char[] CONTENT_ATTR = new char[]{'c','o','n','t','e','n','t'};
    private static final char[] HREF_EVENT1_ATTR = new char[]{'h', 'r', 'e', 'f', '=', '"', 'j', 'a','v', 'a', 's', 'c', 'r', 'i', 'p', 't', ':'};
    private static final char[] HREF_EVENT2_ATTR = new char[]{'h', 'r', 'e', 'f', '=', '\'', 'j', 'a','v', 'a', 's', 'c', 'r', 'i', 'p', 't', ':'};
    private static final char[] START_IE_COMMENT = new char[] {'<', '!', '-', '-', '[', 'i', 'f', ' '};
    private static final char[] END_IE_COMMENT = new char[] {'<', '!', '[', 'e', 'n', 'd', 'i', 'f', ']', '-', '-', '>'};
    private static final char[] INPUT = new char[]{'<', 'i', 'n', 'p','u','t'};
    private static final char[] IMAGE_TYPE = new char[] {'i','m','a','g','e'};
    private static final char[] FORM = new char[] {'<', 'f', 'o', 'r', 'm'};

    private boolean _inHtmlComment = false;

    private boolean _inIEComment = false;

    private boolean _inTag = false;

    private boolean _inStyle = false;
    private boolean _inStyleStart = false;
    private int _styleStartFirst = (-1);
    private boolean _inStyleEnd = false;
    private int _styleEndFirst = (-1);

    /**
     * True when in body of script (element between start and end tags)
     */
    private boolean _inScript = false;

    /**
     * True when in script-start tag
     */
    private boolean _inScriptStart = false;

    private int _scriptStartFirst = (-1);
    private int _scriptStartLast = (-1);

    /**
     * True when in script-end tag
     */
    private boolean _inScriptEnd = false;

    private int _scriptEndFirst = (-1);

    private boolean _inImg = false;

    private boolean _inInput = false;
    private int _inputFirst = -1;

    private boolean _inFrame;
    private boolean _inIframe;
    private int _iframeFirst = (-1);

    private boolean _inHeadStart;
    private boolean _inBodyStart;
    private boolean _inBase;
    private boolean _inMeta;
    private boolean _inAStart;
    private boolean _inFormStart;

    private int _headStartFirst = -1;
    private int _bodyStartFirst = -1;
    private int _aStartFirst = -1;
    private int _formStartFirst = -1;

    private boolean _inSrcAttr = false;
    private int _srcAttrFirst = -1;
    private int _srcAttrLast = -1;

    private boolean _inTypeAttr = false;
    private int _typeAttrFirst = -1;
    private int _typeAttrLast = -1;

    private boolean _inRelAttr = false;
    private int _relAttrFirst = -1;
    private int _relAttrLast = -1;

    private boolean _inMediaAttr = false;
    private int _mediaAttrFirst = -1;
    private int _mediaAttrLast = -1;

    private boolean _inStyleAttr = false;
    private int _styleAttrFirst = -1;
    private int _styleAttrLast = -1;

    private boolean _inOnloadAttr = false;
    private int _onloadAttrFirst = -1;
    private int _onloadAttrLast = -1;

    private boolean _inEventAttr = false;
    private int _eventAttrFirst = -1;
    private int _eventAttrLast = -1;

    private boolean _inTargetAttr = false;
    private int _targetAttrFirst = -1;
    private int _targetAttrLast = -1;

    private boolean _inHrefAttr = false;
    private int _hrefAttrFirst = -1;
    private int _hrefAttrLast = -1;

    private boolean _inHttpEquivAttr = false;
    private int _httpEquivAttrFirst = -1;
    private int _httpEquivAttrLast = -1;

    private boolean _inContentAttr = false;
    private int _contentAttrFirst = -1;
    private int _contentAttrLast = -1;

    private boolean _inAttrWithoutQuotes = false;
    private boolean _inQuotes = false;
    private byte _quotesChar = 0;

    private boolean _inCssBgd = false;

    private boolean _inLink = false;
    private int _linkFirst = -1;

    /**
     * _out[0 .. _outLen - 1] is the result of analysis/
     */
    private int[] _out = new int[2000];
    private int _outLen = 0;

    private String _base = null;

    /**
     * Value of meta tag when http-equiv is Content-Type, example:
     * <meta http-equiv='Content-Type' content='text/html; charset=windows-1255' />
     */
    private String _metaContentType = null;

    /**
     * @param in checked document
     * @return true when input contains html content (not a java-script)
     */
    public static boolean isHtml(byte[] in)
    {
        // Verifying that input contains html document and not java-script:
        int len = in.length;
        if (len > 1024)
            len = 1024;
        int ind = 0;

        // First 3 bytes may be in hex: EF, BB, BF
        if (len > 3 && in[ind] < 0)
        {
            ind++;
            if (in[ind] < 0)
            {
                ind++;
                if (in[ind] < 0)
                    ind++;
            }
        }

        while (ind < len && in[ind] <= ' ')
            ind++;

        if (ind >= len-1)
            return false; // input is java-script

        // Skipping top comment:
        if (in[ind] == '<' && in[ind+1] == '!')
        {
            while (ind < len && in[ind] != '>')
                ind++;

            ind++;

            while (ind < len && in[ind] <= ' ')
                ind++;
        }

        if (ind >= len || in[ind] != '<')
            return false; // input is java-script

        return true;
    }

    /**
     * Constructor
     */
    public HtmlAnalyzer()
    {
    }

    /**
     * Analyzes the input style-sheet or html-page (context.in),
     * and returns the analysis result as array of integers.
     *
     * @param context service-context
     * @return result of analysis of style-sheet or html-page
     */
    public int[] analyze(ServiceContext context)
    {
        byte[] in = context.orig; // The analyzed style-sheet or html-page

        if (!context.isHtml && !context.isCss)
            return new int[0];

        if (context.isHtml)
        {
            // Verifying that input contains html document and not java-script:
            if (!isHtml(in))
                return new int[0]; // input is java-script
        }

        _inStyle = context.isCss;

        boolean withATags = context.isServiceWithIframe;
        boolean withFormTags = context.isServiceWithIframe;

        HashMap<String, Integer> urlToOffset = new HashMap<String, Integer>(256);

        int i = 0;
        while (i < in.length)
        {
            switch (in[i])
            {
                case '"':
                case '\'':
                    if (_inHtmlComment || _inScript)
                    {
                    }
                    else if(_inQuotes)
                    {
                        if(_quotesChar == in[i] &&
                                    ((in[i+1] <= ' ' && in[i+1] >= 0) || in[i+1] == '/'  || in[i+1] == '>' || in[i+1] == ')' || in[i+1] == '\'' || in[i+1] == '"' ||
                                        _inEventAttr))
                        {
                            // End of quotes:
                            if (!_inAttrWithoutQuotes)
                            {
                                if (_inSrcAttr)
                                {
                                    _inSrcAttr = false;
                                    _srcAttrLast = i;
                                    if (_inImg)
                                        addImg(in, urlToOffset);
                                    else if (_inInput)
                                        addInput(in, i, urlToOffset);
                                }
                                else if(_inStyleAttr)
                                {
                                    _inStyleAttr = false;
                                    _styleAttrLast = i;
                                }
                                else if(_inTypeAttr)
                                {
                                    _inTypeAttr = false;
                                    _typeAttrLast = i;
                                }
                                else if(_inRelAttr)
                                {
                                    _inRelAttr = false;
                                    _relAttrLast = i;
                                }
                                else if(_inMediaAttr)
                                {
                                    _inMediaAttr = false;
                                    _mediaAttrLast = i;
                                }
                                else if(_inOnloadAttr)
                                {
                                    _inOnloadAttr = false;
                                    _onloadAttrLast = i;
                                }
                                else if (_inEventAttr)
                                {
                                    _inEventAttr = false;
                                    _eventAttrLast = i;
                                    addEvent();
                                }
                                else if (_inTargetAttr)
                                {
                                    _inTargetAttr = false;
                                    _targetAttrLast = i;
                                    if (_inAStart)
                                        addA(in, -1);
                                }
                                else if(_inHrefAttr)
                                {
                                    _inHrefAttr = false;
                                    _hrefAttrLast = i;
                                }
                                else if (_inHttpEquivAttr)
                                {
                                    _inHttpEquivAttr = false;
                                    _httpEquivAttrLast = i;
                                }
                                else if (_inContentAttr)
                                {
                                    _inContentAttr = false;
                                    _contentAttrLast = i;
                                }
                            }
                            _inQuotes = false;
                        }
                    }
                    else if ((_inTag && (in[i-1]=='=' || in[i-1]<=' ')))
                    {
                        _inQuotes = true;
                        _quotesChar = in[i];
                    }
                    break;

                case '<':
                    if (isMatching(in, i, START_IE_COMMENT))
                    {
                        _inIEComment = true;
                    }
                    else if(isMatching(in, i, END_IE_COMMENT))
                    {
                        _inIEComment = false;
                    }
                    if (_inHtmlComment ||  _inQuotes || (_inScript && !isMatching(in, i, SCRIPT_END)))
                    {
                    }
                    else
                    {
                        if (!_inIEComment && isMatching(in, i, HTML_COMMENT))
                        {
                            _inHtmlComment = true;
                            i += HTML_COMMENT.length;
                            continue;
                        }
                        else
                        {
                            _inTag = true;
                            _inStyleAttr = false;

                            if (isMatching(in, i , STYLE_START))
                            {
                                _inStyleStart = true;
                                _styleStartFirst = i;
                                i += STYLE_START.length;
                                continue;
                            }

                            if(isMatching(in, i, STYLE_END))
                            {
                                _inStyle = false;
                                _inStyleEnd = true;
                                _styleEndFirst = i;
                                i += STYLE_END.length;
                                continue;
                            }

                            if(isMatching(in, i, SCRIPT_START))
                            {
                                _inScriptStart = true;
                                _scriptStartFirst = i;
                                _scriptStartLast = (-1);
                                _inSrcAttr = false;
                                _inTypeAttr = false;
                                i += SCRIPT_START.length;
                                continue;
                            }

                            if(isMatching(in, i, SCRIPT_END))
                            {
                                _inScriptEnd = true;
                                _scriptEndFirst = i;
                                i += SCRIPT_END.length;
                                continue;
                            }

                            if(isMatching(in, i, IMG))
                            {
                                _inImg = true;
                                _inSrcAttr = false;
                                i += IMG.length;
                                continue;
                            }

                            if(isMatching(in, i, LINK))
                            {
                                _inLink = true;
                                _linkFirst = i;
                                _inHrefAttr = false;
                                _inTypeAttr = false;
                                _inRelAttr = false;
                                _inMediaAttr = false;
                                i += LINK.length;
                                continue;
                            }

                            if(isMatching(in, i, INPUT))
                            {
                                _inInput = true;
                                _inputFirst = i;
                                _inTypeAttr = false;
                                _inSrcAttr = false;
                                _typeAttrFirst = (-1);
                                _srcAttrFirst = (-1);
                                i += INPUT.length;
                                continue;
                            }

                            if(isMatching(in, i, FRAME))
                            {
                                _inFrame = true;
                                _inSrcAttr = false;
                                i += FRAME.length;
                                continue;
                            }

                            if(isMatching(in, i, IFRAME))
                            {
                                _inIframe = true;
                                _iframeFirst = i;
                                _inSrcAttr = false;
                                i += IFRAME.length;
                                continue;
                            }

                            if(isMatching(in, i, HEAD))
                            {
                                _inHeadStart = true;
                                _headStartFirst = i;
                                i += HEAD.length;
                                continue;
                            }

                            if(isMatching(in, i, BODY))
                            {
                                _inBodyStart = true;
                                _bodyStartFirst = i;
                                _inOnloadAttr = false;
                                i += BODY.length;
                                continue;
                            }

                            if(isMatching(in, i, BASE))
                            {
                                _inBase = true;
                                _inHrefAttr = false;
                                i += BASE.length;
                                continue;
                            }

                            if(isMatching(in, i, META))
                            {
                                _inMeta = true;
                                _inHttpEquivAttr = false;
                                _inContentAttr = false;
                                i += META.length;
                                continue;
                            }

                            if(withATags && i+2 < in.length && (in[i+1] == 'a' || in[i+1] == 'A') && in[i+2] == ' ')
                            {
                                _inAStart = true;
                                _aStartFirst = i;
                                _inTargetAttr = false;
                                _targetAttrFirst = (-1);
                                _targetAttrLast = (-1);
                                i += 3;
                                continue;
                            }

                            if(withFormTags && isMatching(in, i, FORM))
                            {
                                _inFormStart = true;
                                _formStartFirst = i;
                                _inTargetAttr = false;
                                i += 3;
                                continue;
                            }
                        }
                    }
                    break;

                case '>':
                    if (_inHtmlComment && in[i-1] == '-' && in[i-2] == '-')
                    {
                        _inHtmlComment = false;
                    }
                    else if (_inHtmlComment || _inQuotes ||
                            (_inScript && !isMatching(in, i - SCRIPT_END.length, SCRIPT_END)))
                    {
                    }
                    else if(_inTag)
                    {
                        _inTag = false;
                        _inStyleAttr = false;

                        if (_inSrcAttr)
                        {
                            _srcAttrLast = i;
                            _inSrcAttr = false;
                            if (_inImg)
                                addImg(in, urlToOffset);
                            else if (_inInput)
                                addInput(in, i, urlToOffset);
                        }
                        else if (_inEventAttr)
                        {
                            _eventAttrLast = i;
                            _inEventAttr = false;
                            addEvent();
                        }

                        if(_inLink)
                        {
                            if (_inHrefAttr)
                            {
                                _hrefAttrLast = i;
                                _inHrefAttr = false;
                            }
                            if (_inTypeAttr)
                            {
                                _typeAttrLast = i;
                                _inTypeAttr = false;
                            }
                            if (_inRelAttr)
                            {
                                _relAttrLast = i;
                                _inRelAttr = false;
                            }
                            if (_inMediaAttr)
                            {
                                _mediaAttrLast = i;
                                _inMediaAttr = false;
                            }
                            ensureCapacity(LINK_INFO_LEN);
                            _out[_outLen] = LINK_KIND;
                            _out[_outLen + 1] = checkDuplicates(in, _out, urlToOffset, _hrefAttrFirst, _hrefAttrLast,  _outLen + 1);
                            _out[_outLen + LINK_FIRST] = _linkFirst;
                            _out[_outLen + LINK_LAST] = i + 1;
                            _out[_outLen + LINK_HREF_FIRST] = _hrefAttrFirst;
                            _out[_outLen + LINK_HREF_LAST] = _hrefAttrLast;
                            _out[_outLen + LINK_TYPE_FIRST] = _typeAttrFirst;
                            _out[_outLen + LINK_TYPE_LAST] = _typeAttrLast;
                            _out[_outLen + LINK_REL_FIRST] = _relAttrFirst;
                            _out[_outLen + LINK_REL_LAST] = _relAttrLast;
                            _out[_outLen + LINK_MEDIA_FIRST] = _mediaAttrFirst;
                            _out[_outLen + LINK_MEDIA_LAST] = _mediaAttrLast;
                            _out[_outLen + LINK_IN_IE_COMMENT] = (_inIEComment ? 1 : 0);
                            _outLen += LINK_INFO_LEN;
                            _inLink = false;
                            _linkFirst = (-1);
                            _hrefAttrFirst = (-1);
                            _hrefAttrLast = (-1);
                            _typeAttrFirst = (-1);
                            _typeAttrLast = (-1);
                            _relAttrFirst = (-1);
                            _relAttrLast = (-1);
                            _mediaAttrFirst = (-1);
                            _mediaAttrLast = (-1);
                        }

                        else if(_inInput)
                        {
                            _inInput = false;
                            _inputFirst = (-1);
                            _typeAttrFirst = (-1);
                            _typeAttrLast = (-1);
                            _srcAttrFirst = (-1);
                            _srcAttrLast = (-1);
                        }

                        else if(_inFrame)
                        {
                            ensureCapacity(FRAME_INFO_LEN);
                            _out[_outLen] = FRAME_KIND;
                            _out[_outLen + FRAME_SRC_FIRST] = _srcAttrFirst;
                            _out[_outLen + FRAME_SRC_LAST] = _srcAttrLast;
                            _outLen += FRAME_INFO_LEN;
                            _inFrame = false;
                            _srcAttrFirst = (-1);
                            _srcAttrLast = (-1);
                        }

                        else if(_inIframe)
                        {
                            ensureCapacity(IFRAME_INFO_LEN);
                            _out[_outLen] = IFRAME_KIND;
                            _out[_outLen + IFRAME_FIRST] = _iframeFirst;
                            _out[_outLen + IFRAME_LAST] = i + 1;
                            _out[_outLen + IFRAME_SRC_FIRST] = _srcAttrFirst;
                            _out[_outLen + IFRAME_SRC_LAST] = _srcAttrLast;
                            _outLen += IFRAME_INFO_LEN;
                            _inIframe = false;
                            _iframeFirst = (-1);
                            _srcAttrFirst = (-1);
                            _srcAttrLast = (-1);
                        }

                        else if(_inHeadStart)
                        {
                            ensureCapacity(HEAD_INFO_LEN);
                            _out[_outLen] = HEAD_KIND;
                            _out[_outLen + HEAD_START_FIRST] = _headStartFirst;
                            _out[_outLen + HEAD_START_LAST] = i + 1;
                            _outLen += HEAD_INFO_LEN;
                            _inHeadStart = false;
                        }

                        else if(_inBodyStart)
                        {
                            if (_inOnloadAttr)
                            {
                                _onloadAttrLast = i;
                                _inOnloadAttr = false;
                            }
                            ensureCapacity(BODY_INFO_LEN);
                            _out[_outLen] = BODY_KIND;
                            _out[_outLen + BODY_START_FIRST] = _bodyStartFirst;
                            _out[_outLen + BODY_START_LAST] = i + 1;
                            _out[_outLen + BODY_ONLOAD_FIRST] = _onloadAttrFirst;
                            _out[_outLen + BODY_ONLOAD_LAST] = _onloadAttrLast;
                            _outLen += BODY_INFO_LEN;
                            _inBodyStart = false;
                            _onloadAttrFirst = (-1);
                            _onloadAttrLast = (-1);
                        }

                        else if(_inBase)
                        {
                            if (_inHrefAttr)
                            {
                                _hrefAttrLast = i;
                                _inHrefAttr = false;
                            }
                            if (_hrefAttrFirst > 0)
                                _base = new String(in, _hrefAttrFirst, _hrefAttrLast - _hrefAttrFirst);
                            _out[_outLen] = BASE_KIND;
                            _out[_outLen + BASE_HREF_FIRST] = _hrefAttrFirst;
                            _out[_outLen + BASE_HREF_LAST] = _hrefAttrLast;
                            _outLen += BASE_INFO_LEN;
                            _inBase = false;
                            _hrefAttrFirst = (-1);
                            _hrefAttrLast = (-1);
                        }

                        else if(_inMeta)
                        {
                            if (_inHttpEquivAttr)
                            {
                                _httpEquivAttrLast = i;
                                _inHttpEquivAttr = false;
                            }
                            if (_inContentAttr)
                            {
                                _contentAttrLast = i;
                                _inContentAttr = false;
                            }
                            if (_httpEquivAttrFirst > 0 && _contentAttrFirst > 0 &&
                                    isMatching(in, _httpEquivAttrFirst, CONTENT_TYPE))
                                _metaContentType = new String(in, _contentAttrFirst, _contentAttrLast - _contentAttrFirst);
                            _inMeta = false;
                            _httpEquivAttrFirst = (-1);
                            _httpEquivAttrLast = (-1);
                            _contentAttrFirst = (-1);
                            _contentAttrLast = (-1);
                        }

                        else if(_inAStart)
                        {
                            if (_inHrefAttr)
                            {
                                _hrefAttrLast = i;
                                _inHrefAttr = false;
                            }
                            addA(in, i+1);
                        }

                        else if(_inFormStart)
                        {
                            if (_inTargetAttr)
                            {
                                _targetAttrLast = i;
                                _inTargetAttr = false;
                            }
                            ensureCapacity(FORM_INFO_LEN);
                            _out[_outLen] = FORM_KIND;
                            _out[_outLen + FORM_START_FIRST] = _formStartFirst;
                            _out[_outLen + FORM_START_LAST] = i + 1;
                            _out[_outLen + FORM_TARGET_FIRST] = _targetAttrFirst;
                            _out[_outLen + FORM_TARGET_LAST] = _targetAttrLast;
                            _outLen += FORM_INFO_LEN;
                            _inFormStart = false;
                            _targetAttrFirst = (-1);
                            _targetAttrLast = (-1);
                        }

                        else if(_inStyleStart)
                        {
                            _inStyleStart = false;
                            _inStyle = true;
                            ensureCapacity(STYLE_START_INFO_LEN);
                            _out[_outLen] = STYLE_START_KIND;
                            _out[_outLen + STYLE_START_FIRST ] = _styleStartFirst;
                            _out[_outLen + STYLE_START_LAST] = i + 1;
                            _outLen += STYLE_START_INFO_LEN;
                            _styleStartFirst = (-1);
                        }
                        else if(_inStyleEnd)
                        {
                            _inStyleEnd = false;
                            _inStyle = false;
                            ensureCapacity(STYLE_END_INFO_LEN);
                            _out[_outLen] = STYLE_END_KIND;
                            _out[_outLen + STYLE_END_FIRST ] = _styleEndFirst;
                            _out[_outLen + STYLE_END_LAST] = i + 1;
                            _outLen += STYLE_END_INFO_LEN;
                            _styleEndFirst = (-1);
                        }
                        else if(_inScriptStart)
                        {
                            _inScript = true;
                            _inScriptStart = false;
                            _scriptStartLast = i + 1;

                            if (_inTypeAttr)
                            {
                                _typeAttrLast = i;
                                _inTypeAttr = false;
                            }
                        }
                        else if(_inScriptEnd)
                        {
                            ensureCapacity(SCRIPT_INFO_LEN);
                            _out[_outLen] = SCRIPT_KIND;
                            _out[_outLen + 1] = checkDuplicates(in, _out, urlToOffset, _srcAttrFirst, _srcAttrLast,  _outLen + 1);
                            _out[_outLen + SCRIPT_START_FIRST ] = _scriptStartFirst;
                            _out[_outLen + SCRIPT_SRC_FIRST ] = _srcAttrFirst;
                            _out[_outLen + SCRIPT_SRC_LAST] = _srcAttrLast;
                            _out[_outLen + SCRIPT_TYPE_FIRST ] = _typeAttrFirst;
                            _out[_outLen + SCRIPT_TYPE_LAST] = _typeAttrLast;
                            _out[_outLen + SCRIPT_START_LAST] = _scriptStartLast;
                            _out[_outLen + SCRIPT_END_FIRST ] = _scriptEndFirst;
                            _out[_outLen + SCRIPT_END_LAST] = i + 1;
                            _out[_outLen + SCRIPT_IS_DYNAMIC] = 0;
                            _out[_outLen + SCRIPT_IN_IE_COMMENT] = (_inIEComment ? 1 : 0);
                            _outLen += SCRIPT_INFO_LEN;
                            _inScript = false;
                            _inScriptEnd = false;
                            _scriptStartFirst = (-1);
                            _srcAttrFirst = (-1);
                            _srcAttrLast = (-1);
                            _typeAttrFirst = (-1);
                            _typeAttrLast = (-1);
                            _scriptStartLast = (-1);
                            _scriptEndFirst = (-1);
                        }
                    }
                    break;

                case 's':
                case 'S':
                    if (_inHtmlComment)
                    {
                    }
                    else if (!_inQuotes && !_inImg && _inTag && !_inStyleAttr && isMatching(in, i , STYLE_ATTR))
                    {
                        _inStyleAttr = true;
                        i += STYLE_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _styleAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    else if(!_inQuotes && (_inImg || _inInput || _inIframe || _inScriptStart) && !_inSrcAttr && isMatching(in, i, SRC_ATTR))
                    {
                        _inSrcAttr = true;
                        i += SRC_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _srcAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 'r':
                case 'R':
                    if (_inHtmlComment)
                    {
                    }
                    else if(!_inQuotes && _inLink && !_inRelAttr && isMatching(in, i, REL_ATTR))
                    {
                        _inRelAttr = true;
                        i += REL_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _relAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 'm':
                case 'M':
                    if (_inHtmlComment)
                    {
                    }
                    else if(!_inQuotes && _inLink && !_inMediaAttr && isMatching(in, i, MEDIA_ATTR))
                    {
                        _inMediaAttr = true;
                        i += MEDIA_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _mediaAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 't':
                case 'T':
                    if (_inHtmlComment)
                    {
                    }
                    else if(!_inQuotes && (_inScriptStart || _inInput) && !_inTypeAttr && isMatching(in, i, TYPE_ATTR))
                    {
                        _inTypeAttr = true;
                        i += TYPE_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _typeAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    else if(!_inQuotes && (_inAStart || _inFormStart) && !_inTargetAttr && isMatching(in, i, TARGET_ATTR))
                    {
                        _inTargetAttr = true;
                        i += TARGET_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _targetAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 'o':
                case 'O':
                    if (_inHtmlComment)
                    {
                    }
                    else if (!_inQuotes && _inTag && !_inEventAttr && context.isServiceWithIframe &&
                                            (isMatching(in, i, ONCLICK_ATTR) || isMatching(in, i, ONMOUSE_ATTR)))
                    {
                        _inEventAttr = true;
                        i += ONCLICK_ATTR.length;
                        while (i < in.length && (in[i] != '='))
                            i++;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _eventAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        if (isMatching(in, _eventAttrFirst, JAVA_SCRIPT))
                            _eventAttrFirst += JAVA_SCRIPT.length;
                        continue;
                    }
                    else if (!_inQuotes && _inBodyStart && !_inOnloadAttr && isMatching(in, i, ONLOAD_ATTR))
                    {
                        _inOnloadAttr = true;
                        i += ONLOAD_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _onloadAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 'h':
                case 'H':
                    if (_inHtmlComment)
                    {
                    }
                    else if (!_inQuotes && _inTag && !_inEventAttr && context.isServiceWithIframe &&
                                            (isMatching(in, i, HREF_EVENT1_ATTR) || isMatching(in, i, HREF_EVENT2_ATTR)))
                    {
                        _inEventAttr = true;
                        i += HREF_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _eventAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        if (isMatching(in, _eventAttrFirst, JAVA_SCRIPT))
                            _eventAttrFirst += JAVA_SCRIPT.length;
                        continue;
                    }
                    else if(!_inQuotes && (_inBase || _inLink || _inAStart) && !_inHrefAttr && isMatching(in, i, HREF_ATTR))
                    {
                        _inHrefAttr = true;
                        i += HREF_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _hrefAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    else if(!_inQuotes && _inMeta && !_inHttpEquivAttr && isMatching(in, i, HTTP_EQUIV_ATTR))
                    {
                        _inHttpEquivAttr = true;
                        i += HTTP_EQUIV_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _httpEquivAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case 'c':
                case 'C':
                    if (_inHtmlComment)
                    {
                    }
                    else if(!_inQuotes && _inMeta && !_inContentAttr && isMatching(in, i, CONTENT_ATTR))
                    {
                        _inContentAttr = true;
                        i += CONTENT_ATTR.length;
                        while (i < in.length && (in[i] == '=' || in[i] <= ' '))
                            i++;
                        _inAttrWithoutQuotes = (in[i] != '"' && in[i] != '\'');
                        _contentAttrFirst = (_inAttrWithoutQuotes ? i : i + 1);
                        continue;
                    }
                    break;

                case ' ':
                    if (_inHtmlComment)
                    {
                    }
                    else if(!_inQuotes && _inAttrWithoutQuotes)
                    {
                        _inAttrWithoutQuotes = false;

                        if (_inSrcAttr)
                        {
                            _inSrcAttr = false;
                            _srcAttrLast = i;
                            if (_inImg)
                                addImg(in, urlToOffset);
                            else if (_inInput)
                                addInput(in, i, urlToOffset);
                        }
                        else if(_inStyleAttr)
                        {
                            _inStyleAttr = false;
                            _styleAttrLast = i;
                        }
                        else if(_inTypeAttr)
                        {
                            _inTypeAttr = false;
                            _typeAttrLast = i;
                        }
                        else if(_inOnloadAttr)
                        {
                            _inOnloadAttr = false;
                            _onloadAttrLast = i;
                        }
                        else if (_inEventAttr)
                        {
                            _inEventAttr = false;
                            _eventAttrLast = i;
                            addEvent();
                        }
                        else if(_inHrefAttr)
                        {
                            _inHrefAttr = false;
                            _hrefAttrLast = i;
                        }
                        else if (_inHttpEquivAttr)
                        {
                            _inHttpEquivAttr = false;
                            _httpEquivAttrLast = i;
                        }
                        else if (_inContentAttr)
                        {
                            _inContentAttr = false;
                            _contentAttrLast = i;
                        }
                    }
                    break;

                case 'b':
                case 'B':
                    if (_inHtmlComment)
                    {
                    }
                    else if(_inStyle || _inStyleAttr)
                    {
                        int matchLen;
                        if (isMatching(in, i, BGD))
                        {
                            _inCssBgd = true;
                            i += BGD.length;
                            continue;
                        }
                        else if((matchLen = getMatchLen(in, i, BGD_IMG)) >= 0)
                        {
                            _inCssBgd = true;
                            i += matchLen;
                            continue;
                        }
                    }
                    break;

                case 'u':
                case 'U':
                    if (_inHtmlComment)
                    {
                    }
                    else if ((_inStyle || _inStyleAttr) && _inCssBgd && isMatching(in, i, URL))
                    {
                        i += URL.length;
                        while (i < in.length && (in[i] == '(' || in[i] <= ' ' || in[i] == '"' || in[i] == '\''))
                            i++;
                        int first = i;
                        int last = first;
                        while (last < in.length && in[last] != ')' && in[last] != '"' && in[last] != '\'')
                            last++;
                        if (last < in.length)
                        {
                            ensureCapacity(CSS_IMG_INFO_LEN);
                            _out[_outLen] = CSS_IMG_KIND;
                            _out[_outLen + 1] = checkDuplicates(in, _out, urlToOffset, first, last,  _outLen + 1);
                            _out[_outLen + CSS_IMG_FIRST] = first;
                            _out[_outLen + CSS_IMG_LAST] = last;
                            _outLen += CSS_IMG_INFO_LEN;
                            i = last;
                            continue;
                        }
                    }
                    break;

                case ';':
                case '}':
                    _inCssBgd = false;
                    break;
            }

            i++;
        }

        ensureCapacity(1);
        _out[_outLen] = EOF_KIND;
        return _out;
    }

    /**
     * @return href of base tag (null when no base tag in document).
     */
    public String getBase()
    {
        return _base;
    }

    /**
     * @return value of meta tag when http-equiv is Content-Type (null when not found).
     */
    public String getMetaContentType()
    {
        return _metaContentType;
    }

    /**
     * duplicates-info:
     *      0:  when url is unique in input-buffer
     *      negative: number of encountered duplicates (in first duplicate)
     *      positive: offset in output-array of first encountered duplicate (in other duplicates)
     *
     * @param in analyzed input-buffer
     * @param out output-buffer (result of analysis of input-buffer)
     * @param urlToOutOffset maps encountered urls (in bytes) to offsets in output-array
     * @param inFirst start of url in input-buffer
     * @param inLast end of url in input-buffer
     * @param outOffset offset in output-array containing duplicates-info
     * @return the duplicates-info to assign to be assigned to output-array at offset outOffset
     */
    private int checkDuplicates(
                                    byte[] in,
                                    int[] out,
                                    HashMap<String, Integer> urlToOutOffset,
                                    int inFirst,
                                    int inLast,
                                    int outOffset)
    {
        if (inFirst < 0 || inLast <= inFirst)
            return 0;

        int urlLen = inLast - inFirst;
        String url;
        try
        {
            url =  new String(in, inFirst, urlLen, "UTF-8");
        }
        catch (Exception exc)
        {
            return 0;
        }

        Integer firstOutOffset = urlToOutOffset.get(url);

        if (firstOutOffset == null)
        {
            // url not previously encountered:
            urlToOutOffset.put(url, outOffset);
            return 0;
        }

        out[firstOutOffset]--;
        return firstOutOffset;
    }

    private boolean isMatching(byte[] bytes, int offset, char[] value)
    {
        return getMatchLen(bytes, offset, value) >= 0;
    }

    private int getMatchLen(byte[] bytes, int offset, char[] value)
    {
        if (bytes.length <= offset + value.length)
            return -1;

        int o = offset;

        for (int i = 0 ; i < value.length && o < bytes.length ; i++)
        {
            if (value[i] == ' ')
            {
                // Skipping zero, one, or more white spaces in bytes:
                while (o < bytes.length && bytes[o] <= ' ')
                    o++;
                continue;
            }

            byte b = bytes[o++];
            if (b != value[i] &&
                    ( b < 'A' || 'Z' < b || b - 'A' + 'a' != value[i]))
                return -1;
        }

        if (o >= bytes.length)
            return -1;
        else
            return o - offset;
    }

    /**
     * Enlarges _out when not big enougth
     *
     * @param minFree number of going to be added elements
     */
    private void ensureCapacity(int minFree)
    {
        if (_outLen + minFree >= _out.length)
        {
            int[] tmp = new int[_out.length + 2000];
            System.arraycopy(_out, 0, tmp, 0, _out.length);
            _out = tmp;
        }
    }

    /**
     * Adds an onXxx event to _out.
     */
    private void addEvent()
    {
        if (_inTag)
        {
            ensureCapacity(EVENT_INFO_LEN);
            _out[_outLen] = EVENT_KIND;
            _out[_outLen + EVENT_FIRST] = _eventAttrFirst;
            _out[_outLen + EVENT_LAST] = _eventAttrLast;
            _outLen += EVENT_INFO_LEN;
            _inEventAttr = false;
            _eventAttrFirst = (-1);
            _eventAttrLast = (-1);
        }
    }

    /**
     * Adds an IMG to _out.
     *
     * @param in input byte-array
     * @param urlToOffset 
     */
    private void addImg(byte[] in, HashMap<String, Integer> urlToOffset)
    {
        if (_inImg)
        {
            ensureCapacity(IMG_INFO_LEN);
            _out[_outLen] = IMG_KIND;
            _out[_outLen + 1] = checkDuplicates(in, _out, urlToOffset, _srcAttrFirst, _srcAttrLast,  _outLen + 1);
            _out[_outLen + IMG_SRC_FIRST] = _srcAttrFirst;
            _out[_outLen + IMG_SRC_LAST] = _srcAttrLast;
            _outLen += IMG_INFO_LEN;
            _inImg = false;
            _srcAttrFirst = (-1);
            _srcAttrLast = (-1);
        }
    }

    /**
     * Adds an INPUT to _out.
     *
     * @param in input byte-array
     * @param i current index in 'in'
     * @param urlToOffset 
     */
    private void addInput(byte[] in, int i, HashMap<String, Integer> urlToOffset)
    {
        if (_inInput)
        {
            if (_inTypeAttr)
            {
                _typeAttrLast = i;
                _inTypeAttr = false;
            }
            if (_srcAttrFirst >= 0 && _typeAttrFirst >= 0 && isMatching(in, _typeAttrFirst+1, IMAGE_TYPE))
            {
                ensureCapacity(INPUT_INFO_LEN);
                _out[_outLen] = INPUT_KIND;
                _out[_outLen + 1] = checkDuplicates(in, _out, urlToOffset, _srcAttrFirst, _srcAttrLast,  _outLen + 1);
                _out[_outLen + INPUT_FIRST] = _inputFirst;
                _out[_outLen + INPUT_LAST] = i + 1;
                _out[_outLen + INPUT_SRC_FIRST] = _srcAttrFirst;
                _out[_outLen + INPUT_SRC_LAST] = _srcAttrLast;
                _outLen += INPUT_INFO_LEN;
            }
            _inInput = false;
            _inputFirst = (-1);
            _typeAttrFirst = (-1);
            _typeAttrLast = (-1);
            _srcAttrFirst = (-1);
            _srcAttrLast = (-1);
        }
    }

    /**
     * Adds an A to _out.
     *
     * @param in input byte-array
     * @param aStartLast when _targetAttrFirst < 0 then points to last char of <a...> element (-1 otherwise).
     */
    private void addA(byte[] in, int aStartLast)
    {
        ensureCapacity(A_INFO_LEN);
        _out[_outLen] = A_KIND;
        _out[_outLen + A_START_FIRST] = _aStartFirst;
        _out[_outLen + A_START_LAST] = aStartLast;
        _out[_outLen + A_TARGET_FIRST] = _targetAttrFirst;
        _out[_outLen + A_TARGET_LAST] = _targetAttrLast;
        _out[_outLen + A_HREF_FIRST] = _hrefAttrFirst;
        _out[_outLen + A_HREF_LAST] = _hrefAttrLast;
        _outLen += A_INFO_LEN;
        _inAStart = false;
        _targetAttrFirst = (-1);
        _targetAttrLast = (-1);
        _hrefAttrFirst = (-1);
        _hrefAttrLast = (-1);
    }
}
