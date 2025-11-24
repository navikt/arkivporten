package no.nav.syfo.application.metric

import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry

const val METRICS_NS = "syfo-arkivporten"

val METRICS_REGISTRY = PrometheusMeterRegistry(PrometheusConfig.DEFAULT)
