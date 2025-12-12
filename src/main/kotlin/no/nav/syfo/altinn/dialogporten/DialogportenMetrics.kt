package no.nav.syfo.altinn.dialogporten

import io.micrometer.core.instrument.Counter
import no.nav.syfo.application.metric.METRICS_NS
import no.nav.syfo.application.metric.METRICS_REGISTRY

const val DIALOGPORTEN_DIALOGS_CREATED = "${METRICS_NS}_dialogporten_dialogs_created"
val COUNT_DIALOGPORTEN_DIALOGS_CREATED: Counter = Counter.builder(DIALOGPORTEN_DIALOGS_CREATED)
    .description("Counts the number of dialogs created in dialogporten")
    .register(METRICS_REGISTRY)

const val DIALOGPORTEN_TRANSMISSIONS_CREATED = "${METRICS_NS}_dialogporten_transmissions_created"
val COUNT_DIALOGPORTEN_TRANSMISSIONS_CREATED: Counter = Counter.builder(DIALOGPORTEN_TRANSMISSIONS_CREATED)
    .description("Counts the number of tranmissions created in dialogporten")
    .register(METRICS_REGISTRY)
