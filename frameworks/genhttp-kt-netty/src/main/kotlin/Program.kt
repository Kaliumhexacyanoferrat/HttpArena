package genhttp

import genhttp.infrastructure.Certificates

import org.genhttp.api.infrastructure.CertificateProvider
import org.genhttp.engine.netty.Host
import org.genhttp.modules.compression.compression

import org.genhttp.engine.netty.EngineOptions
import org.genhttp.engine.netty.EventLoopOptions
import org.genhttp.engine.netty.TcpTransportOptions
import org.genhttp.engine.netty.Transport

import kotlin.time.Duration.Companion.seconds

import org.slf4j.helpers.NOPLoggerFactory

import java.io.File

import kotlin.system.exitProcess

suspend fun main() {
    val certPath = System.getenv("TLS_CERT") ?: "/certs/server.crt"
    val keyPath = System.getenv("TLS_KEY") ?: "/certs/server.key"
    val hasCert = File(certPath).exists() && File(keyPath).exists()

    val app = Project.create()

    val cpuCount = Runtime.getRuntime().availableProcessors()

    val options = EngineOptions(
      eventLoop = EventLoopOptions(count = cpuCount, transport = Transport.IoUring),
      tcp = TcpTransportOptions(idleTimeout = 30.seconds, sendTimeout = 30.seconds)
    )

    val host = Host.create(options = options)
        .handler(app)
        .compression()
        .logging(NOPLoggerFactory(), false)

    host.bind(null, 8080)

    if (hasCert) {
        val certificateProvider = CertificateProvider.from(Certificates.fromPem(certPath, keyPath))

        host.bind(null, 8081, certificateProvider)
        host.bind(null, 8443, certificateProvider)
    }

    exitProcess(host.run())
}
