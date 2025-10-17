package no.nav.syfo.plugins

import io.ktor.server.application.Application
import io.ktor.server.application.install
import no.nav.syfo.ereg.EregService
import no.nav.syfo.ereg.client.EregClient
import no.nav.syfo.ereg.client.FakeEregClient
import no.nav.syfo.altinntilganger.AltinnTilgangerService
import no.nav.syfo.altinntilganger.client.AltinnTilgangerClient
import no.nav.syfo.altinntilganger.client.FakeAltinnTilgangerClient
import no.nav.syfo.application.ApplicationState
import no.nav.syfo.application.Environment
import no.nav.syfo.application.LocalEnvironment
import no.nav.syfo.application.NaisEnvironment
import no.nav.syfo.application.database.Database
import no.nav.syfo.application.database.DatabaseConfig
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.application.isLocalEnv
import no.nav.syfo.application.leaderelection.LeaderElection
import no.nav.syfo.document.service.ValidationService
import no.nav.syfo.document.db.DocumentDAO
import no.nav.syfo.dialogporten.client.DialogportenClient
import no.nav.syfo.dialogporten.task.SendDialogTask
import no.nav.syfo.texas.client.TexasHttpClient
import no.nav.syfo.util.httpClientDefault
import org.koin.core.scope.Scope
import org.koin.dsl.module
import org.koin.ktor.plugin.Koin
import org.koin.logger.slf4jLogger

fun Application.configureDependencies() {
    install(Koin) {
        slf4jLogger()

        modules(
            applicationStateModule(),
            environmentModule(isLocalEnv()),
            httpClient(),
            databaseModule(),
            servicesModule()
        )
    }
}

private fun applicationStateModule() = module { single { ApplicationState() } }

private fun environmentModule(isLocalEnv: Boolean) = module {
    single {
        if (isLocalEnv) LocalEnvironment()
        else NaisEnvironment()
    }
}

private fun httpClient() = module {
    single {
        httpClientDefault()
    }
}

private fun databaseModule() = module {
    single<DatabaseInterface> {
        Database(
            DatabaseConfig(
                jdbcUrl = env().database.jdbcUrl(),
                username = env().database.username,
                password = env().database.password,
            )
        )
    }
    single {
        DocumentDAO(get())
    }
}

private fun servicesModule() = module {
    single { TexasHttpClient(client = get(), environment = env().texas) }
    single { DialogportenClient("https://platform.tt02.altinn.no", get(), get()) }
    single {
        if (isLocalEnv()) FakeEregClient() else EregClient(
            eregBaseUrl = env().clientProperties.eregBaseUrl,
        )
    }
    single {
        EregService(
            eregClient = get()
        )
    }
    single {
        if (isLocalEnv()) FakeAltinnTilgangerClient() else AltinnTilgangerClient(
            texasClient = get(),
            httpClient = get(),
            baseUrl = env().clientProperties.altinnTilgangerBaseUrl,
        )
    }

    single { AltinnTilgangerService(get()) }

    single { EregService(get()) }
    single { ValidationService(get(), get()) }
    single { LeaderElection(get(), env().clientProperties.electorPath) }
    single { SendDialogTask(get(), get(), get()) }
}

private fun Scope.env() = get<Environment>()
