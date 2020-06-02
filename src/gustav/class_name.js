goog.provide("gustav.class_name");


/**
 * The following annotation allows the google closure compiler to heavily
 * optimize and make the function call disappear, and the class name will be
 * uniquely optimized by a short name globally.
 *
 * This strategy does not compromise the cljs cache.
 *
 * @idGenerator {consistent}
 * @param {string} x The name to get an ID for.
 * @return {string} A short, unique ID that is consistent per input name.
 */

gustav.class_name.generator = function (x) {
  return x;
};
