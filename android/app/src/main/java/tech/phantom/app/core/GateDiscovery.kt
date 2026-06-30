package tech.phantom.app.core

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo

/**
 * Finds a Phantom gate on the local network via mDNS (NsdManager), reading the
 * gate URL + bearer token from the service's TXT record. Mirrors the
 * `_phantom._tcp` service advertised by phantom/pc/discovery.py.
 *
 * Callbacks fire on a binder thread — the caller should marshal to the UI
 * thread. Call [stop] when done (or after the first hit).
 */
class GateDiscovery(context: Context) {

    data class Found(val url: String, val token: String)

    private val nsd = context.applicationContext.getSystemService(Context.NSD_SERVICE) as NsdManager
    private var listener: NsdManager.DiscoveryListener? = null
    private var done = false

    fun start(onFound: (Found) -> Unit, onError: (String) -> Unit) {
        val l = object : NsdManager.DiscoveryListener {
            override fun onServiceFound(info: NsdServiceInfo) {
                @Suppress("DEPRECATION")
                nsd.resolveService(info, object : NsdManager.ResolveListener {
                    override fun onServiceResolved(resolved: NsdServiceInfo) {
                        if (done) return
                        val attrs = resolved.attributes ?: emptyMap()
                        val token = attrs["token"]?.toString(Charsets.UTF_8).orEmpty()
                        val url = attrs["url"]?.toString(Charsets.UTF_8)
                            ?: "http://${resolved.host?.hostAddress}:${resolved.port}"
                        if (token.isNotEmpty()) {
                            done = true
                            onFound(Found(url, token))
                            stop()
                        }
                    }

                    override fun onResolveFailed(info: NsdServiceInfo, errorCode: Int) {
                        onError("resolve failed ($errorCode)")
                    }
                })
            }

            override fun onServiceLost(info: NsdServiceInfo) {}
            override fun onDiscoveryStarted(serviceType: String) {}
            override fun onDiscoveryStopped(serviceType: String) {}
            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                onError("discovery failed ($errorCode)")
            }
            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {}
        }
        listener = l
        nsd.discoverServices("_phantom._tcp", NsdManager.PROTOCOL_DNS_SD, l)
    }

    fun stop() {
        listener?.let {
            try {
                nsd.stopServiceDiscovery(it)
            } catch (_: Exception) {
            }
        }
        listener = null
    }
}
