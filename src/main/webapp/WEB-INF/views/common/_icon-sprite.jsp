<%@ page contentType="text/html;charset=UTF-8" pageEncoding="UTF-8" %>
<%-- Stroke icon set, drawn from Lucide (MIT licensed, 24x24, stroke-width 2, round caps/joins).

     Vendored as an inline sprite rather than installed: this project has no npm/build step, so the
     packaged version is not usable, and a CDN script that rewrites the DOM after load would add an
     external dependency plus a flash of missing icons. Defined once per page and referenced with
     <use href="#i-name">, so the geometry is downloaded once and every icon inherits the current
     text colour and size from .icon in style.css.

     To add an icon: copy its <path>/<circle>/<line> children from lucide.dev into a new <symbol>
     with the same viewBox. Do not draw new shapes by hand — the point of a set is that every glyph
     shares one optical weight. --%>
<svg xmlns="http://www.w3.org/2000/svg" style="display:none" aria-hidden="true">
    <symbol id="i-dashboard" viewBox="0 0 24 24"><rect x="3" y="3" width="7" height="9" rx="1"/><rect x="14" y="3" width="7" height="5" rx="1"/><rect x="14" y="12" width="7" height="9" rx="1"/><rect x="3" y="16" width="7" height="5" rx="1"/></symbol>
    <symbol id="i-users" viewBox="0 0 24 24"><path d="M16 21v-2a4 4 0 0 0-4-4H6a4 4 0 0 0-4 4v2"/><circle cx="9" cy="7" r="4"/><path d="M22 21v-2a4 4 0 0 0-3-3.87"/><path d="M16 3.13a4 4 0 0 1 0 7.75"/></symbol>
    <symbol id="i-utensils" viewBox="0 0 24 24"><path d="M3 2v7c0 1.1.9 2 2 2h1a2 2 0 0 0 2-2V2"/><path d="M6 11v11"/><path d="M18 2v20"/><path d="M18 2a3 3 0 0 1 3 3v7h-3"/></symbol>
    <symbol id="i-folders" viewBox="0 0 24 24"><path d="M3 7a2 2 0 0 1 2-2h3l2 2h6a2 2 0 0 1 2 2v1"/><path d="M3 9h15a2 2 0 0 1 2 2l-1 7a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></symbol>
    <symbol id="i-trending-up" viewBox="0 0 24 24"><polyline points="22 7 13.5 15.5 8.5 10.5 2 17"/><polyline points="16 7 22 7 22 13"/></symbol>
    <symbol id="i-briefcase" viewBox="0 0 24 24"><rect x="2" y="7" width="20" height="14" rx="2"/><path d="M16 21V5a2 2 0 0 0-2-2h-4a2 2 0 0 0-2 2v16"/></symbol>
    <symbol id="i-clock" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><polyline points="12 7 12 12 15 14"/></symbol>
    <symbol id="i-store" viewBox="0 0 24 24"><path d="M3 9l1.5-5h15L21 9"/><path d="M4 9v11a1 1 0 0 0 1 1h14a1 1 0 0 0 1-1V9"/><path d="M3 9a3 3 0 0 0 6 0 3 3 0 0 0 6 0 3 3 0 0 0 6 0"/><path d="M9 21v-6h6v6"/></symbol>
    <symbol id="i-inbox-in" viewBox="0 0 24 24"><path d="M12 3v9"/><polyline points="8 9 12 13 16 9"/><path d="M3 13h5l1.5 3h5L16 13h5v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z"/></symbol>
    <symbol id="i-building" viewBox="0 0 24 24"><rect x="4" y="3" width="16" height="18" rx="1"/><path d="M8 7h2M14 7h2M8 11h2M14 11h2M8 15h2M14 15h2"/><path d="M10 21v-3h4v3"/></symbol>
    <symbol id="i-image" viewBox="0 0 24 24"><rect x="3" y="3" width="18" height="18" rx="2"/><circle cx="9" cy="9" r="2"/><path d="m21 15-4.5-4.5L7 20"/></symbol>
    <symbol id="i-wallet" viewBox="0 0 24 24"><path d="M19 7V5a2 2 0 0 0-2-2H5a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-2"/><path d="M21 12h-5a2 2 0 0 0 0 4h5z"/></symbol>
    <symbol id="i-key" viewBox="0 0 24 24"><circle cx="8" cy="15" r="4"/><path d="m10.5 12.5 8-8"/><path d="m17 4 3 3"/><path d="m14.5 6.5 2.5 2.5"/></symbol>
    <symbol id="i-search" viewBox="0 0 24 24"><circle cx="11" cy="11" r="7"/><path d="m21 21-4.3-4.3"/></symbol>
    <symbol id="i-heart" viewBox="0 0 24 24"><path d="M20.8 5.6a5.5 5.5 0 0 0-7.8 0L12 6.6l-1-1a5.5 5.5 0 0 0-7.8 7.8l1 1L12 22l7.8-7.6 1-1a5.5 5.5 0 0 0 0-7.8z"/></symbol>
    <symbol id="i-eye" viewBox="0 0 24 24"><path d="M2 12s3.6-7 10-7 10 7 10 7-3.6 7-10 7-10-7-10-7z"/><circle cx="12" cy="12" r="3"/></symbol>
    <symbol id="i-chevron-down" viewBox="0 0 24 24"><polyline points="6 9 12 15 18 9"/></symbol>
    <symbol id="i-x" viewBox="0 0 24 24"><path d="M18 6 6 18"/><path d="m6 6 12 12"/></symbol>
    <symbol id="i-check-circle" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><polyline points="8.5 12.5 11 15 15.5 9.5"/></symbol>
    <symbol id="i-ban" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="m5.6 5.6 12.8 12.8"/></symbol>
    <symbol id="i-gift" viewBox="0 0 24 24"><rect x="3" y="8" width="18" height="4" rx="1"/><path d="M5 12v8a1 1 0 0 0 1 1h12a1 1 0 0 0 1-1v-8"/><path d="M12 8v13"/><path d="M12 8 9.5 5.5a2 2 0 1 1 2.5-.5 2 2 0 1 1 2.5.5L12 8"/></symbol>
    <symbol id="i-id-card" viewBox="0 0 24 24"><rect x="2" y="5" width="20" height="14" rx="2"/><circle cx="8" cy="11" r="2"/><path d="M5 16c.6-1.5 1.8-2 3-2s2.4.5 3 2"/><path d="M14 10h5M14 14h4"/></symbol>
    <symbol id="i-arrow-left" viewBox="0 0 24 24"><path d="M19 12H5"/><polyline points="12 19 5 12 12 5"/></symbol>
    <symbol id="i-graduation-cap" viewBox="0 0 24 24"><path d="M21 9 12 5 3 9l9 4z"/><path d="M6 11v5c0 1.1 2.7 2 6 2s6-.9 6-2v-5"/></symbol>
    <symbol id="i-phone" viewBox="0 0 24 24"><path d="M22 16.9v3a2 2 0 0 1-2.2 2 19.8 19.8 0 0 1-8.6-3.1 19.5 19.5 0 0 1-6-6A19.8 19.8 0 0 1 2.1 4.2 2 2 0 0 1 4.1 2h3a2 2 0 0 1 2 1.7c.1 1 .4 1.9.7 2.8a2 2 0 0 1-.5 2.1L8.1 9.9a16 16 0 0 0 6 6l1.3-1.3a2 2 0 0 1 2.1-.4c.9.3 1.8.6 2.8.7a2 2 0 0 1 1.7 2z"/></symbol>
    <symbol id="i-package" viewBox="0 0 24 24"><path d="m12 3 8.5 4.5v9L12 21l-8.5-4.5v-9z"/><polyline points="3.5 7.5 12 12 20.5 7.5"/><path d="M12 12v9"/></symbol>
    <symbol id="i-bike" viewBox="0 0 24 24"><circle cx="5.5" cy="17.5" r="3.5"/><circle cx="18.5" cy="17.5" r="3.5"/><path d="M15 6a1 1 0 1 0 0-2 1 1 0 0 0 0 2"/><path d="M12 17.5 9 9l4-2 3 4h3"/></symbol>
    <symbol id="i-flame" viewBox="0 0 24 24"><path d="M12 3c2 3.5 5 5 5 9a5 5 0 0 1-10 0c0-2 1-3.5 2-5 .5 1 1 1.5 2 2 0-2.5-1-4 1-6z"/></symbol>
    <symbol id="i-alert-circle" viewBox="0 0 24 24"><circle cx="12" cy="12" r="9"/><path d="M12 7.5v5"/><path d="M12 16h.01"/></symbol>
</svg>
