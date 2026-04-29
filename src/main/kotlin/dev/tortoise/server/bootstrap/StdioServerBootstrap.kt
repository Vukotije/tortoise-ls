package dev.tortoise.server.bootstrap

import dev.tortoise.server.protocol.TortoiseLanguageServer
import org.eclipse.lsp4j.jsonrpc.Launcher
import org.eclipse.lsp4j.launch.LSPLauncher
import org.eclipse.lsp4j.services.LanguageClient

class StdioServerBootstrap : ServerBootstrap {
    override fun start() {
        val server = TortoiseLanguageServer()
        val launcher: Launcher<LanguageClient> = LSPLauncher.createServerLauncher(
            server,
            System.`in`,
            System.out,
        )

        server.connect(launcher.remoteProxy)
        launcher.startListening().get()
    }
}
