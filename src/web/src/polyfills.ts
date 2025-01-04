/**
 * Vessel Call Management System - Polyfills Configuration
 * Version: 1.0.0
 * 
 * This file includes polyfills required for Angular 16.x and cross-browser compatibility.
 * Browser Support:
 * - Chrome >= 112
 * - Firefox >= 112
 * - Edge >= 112
 * - Safari >= 16.4
 */

/***************************************************************************************************
 * Zone JS is required by default for Angular itself.
 * @version ~0.13.0
 */
import 'zone.js'; // Included with Angular CLI by default

/***************************************************************************************************
 * APPLICATION IMPORTS
 */

/**
 * Support for older browsers that might not have globalThis
 */
if (typeof globalThis === 'undefined') {
  (window as any).globalThis = window;
}

/**
 * IE11 requires the following for NgClass support on SVG elements
 * Uncomment if IE11 support is needed
 */
// import 'classlist.js';

/**
 * Web Animations `@angular/platform-browser/animations`
 * Only required if AnimationBuilder is used within the application 
 * Uncomment if needed
 */
// import 'web-animations-js';

/**
 * By default, zone.js will patch all possible macroTask and DomEvents
 * user can disable parts of macroTask/DomEvents patch by setting following flags
 */
(window as any).__Zone_disable_requestAnimationFrame = false;
(window as any).__Zone_disable_on_property = false;
(window as any).__zone_symbol__BLACK_LISTED_EVENTS = ['scroll', 'mousemove'];

/***************************************************************************************************
 * Zone JS LOAD_ERROR EVENT PATCH
 * This patch ensures proper handling of load_error events across browsers
 */
(window as any).__Zone_enable_cross_context_check = true;

/**
 * Polyfill ECMAScript features selectively based on browser support
 * Using differential loading strategy to optimize bundle size
 */
if (!Object.fromEntries) {
  Object.fromEntries = function<T = any>(entries: Iterable<readonly [PropertyKey, T]>): { [k: string]: T } {
    const obj: { [k: string]: T } = {};
    for (const [key, value] of entries) {
      obj[key as string] = value;
    }
    return obj;
  };
}

/**
 * Ensure consistent handling of WebSocket connections across browsers
 */
if (typeof WebSocket !== 'undefined') {
  (window as any).__zone_symbol__UNPATCHED_EVENTS = ['message'];
}

/**
 * Enable Service Worker support if available
 */
if ('serviceWorker' in navigator) {
  (window as any).__Zone_disable_requestAnimationFrame = false;
  (window as any).__Zone_disable_on_property = false;
  (window as any).__Zone_disable_XHR = false;
}

/**
 * Performance optimization: Disable zone.js patching for selected APIs
 * when not required by the application
 */
(window as any).__Zone_disable_timer = false;
(window as any).__Zone_disable_EventTarget = false;
(window as any).__Zone_disable_fs = true;