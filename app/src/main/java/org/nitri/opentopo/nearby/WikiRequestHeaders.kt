package org.nitri.opentopo.nearby

/**
 * Helper for Wikimedia API request headers.
 */
internal object WikiRequestHeaders {

    /**
     * Returns a policy-compliant User-Agent for Wikimedia API requests.
     *
     * See: https://meta.wikimedia.org/wiki/User-Agent_policy
     */
    fun userAgent(versionName: String): String {
        return "OpenTopoMapViewer/$versionName (https://github.com/Pygmalion69/OpenTopoMapViewer)"
    }
}
