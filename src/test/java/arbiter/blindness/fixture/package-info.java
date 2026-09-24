/**
 * Stand-in application code for proving the blindness rule, never used by the app.
 *
 * <p>{@code ui} plays the role of {@code arbiter.ui} and {@code service} of {@code arbiter.service}.
 * Each class under {@code ui} other than {@code BlindScreen} and the adjudicator's package leaks
 * exactly one way, so the test can tell which rule caught it.
 */
package arbiter.blindness.fixture;
