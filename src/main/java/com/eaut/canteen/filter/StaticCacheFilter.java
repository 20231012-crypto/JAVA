package com.eaut.canteen.filter;

import java.io.IOException;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.annotation.WebFilter;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Tells browsers they may keep the CSS, JavaScript and images rather than re-checking them.
 *
 * <p>Tomcat already sends an ETag, so a repeat visit was never re-downloading these files — but it
 * was still making a request for each one and waiting for a 304. From Vietnam that is roughly 300ms
 * per file before the browser knows it can use what it already has, and the catalog page pulls in a
 * stylesheet, three scripts and every dish photo. With an explicit max-age the browser skips the
 * conversation entirely.
 *
 * <p>The awkward part of caching is invalidation, and this app has no asset fingerprinting: the
 * stylesheet is always /assets/css/style.css, so a long max-age would leave visitors on the old one
 * after a deploy with no way to tell them. Hence the split below — assets that change when we
 * deploy get an hour, and uploaded media, which is addressed by an id that changes when the content
 * does, gets a week.
 *
 * <p>Deliberately not applied to pages. Every HTML response here depends on who is logged in, what
 * is in their cart and what the kitchen has just run out of; caching any of it would show one
 * student another's page.
 */
@WebFilter({"/assets/*", "/images/*", "/banner-image"})
public class StaticCacheFilter implements Filter {

    /**
     * One hour for build assets. Long enough that a student browsing the menu fetches style.css
     * once for the whole session, short enough that a fix reaches everyone the same morning
     * without a hard refresh.
     */
    private static final String BUILD_ASSET_CACHE = "public, max-age=3600";

    /**
     * A week for uploaded media. /images/<filename> and /banner-image?id=<n> name a specific
     * picture, and replacing one produces a different filename or a different row, so a stale copy
     * cannot be served under the identity of a new one.
     */
    private static final String UPLOADED_MEDIA_CACHE = "public, max-age=604800";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;

        String path = req.getServletPath();
        boolean uploaded = path.startsWith("/images") || path.equals("/banner-image");
        resp.setHeader("Cache-Control", uploaded ? UPLOADED_MEDIA_CACHE : BUILD_ASSET_CACHE);

        chain.doFilter(request, response);
    }
}
