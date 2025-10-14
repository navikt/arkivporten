package no.nav.syfo.dialogporten.domain

sealed class PatchOperation {
    abstract val op: String
    abstract val path: String
}

data class AddTransmissions(
    val value: List<Transmission>,
    override val op: String = "add",
    override val path: String = "/transmissions",
) : PatchOperation()

data class AddStatus(
    val value: DialogStatus,
    override val op: String = "add",
    override val path: String = "/status",
) : PatchOperation()

data class AddApiActions(
    val value: List<ApiAction>,
    override val op: String = "add",
    override val path: String = "/apiActions",
) : PatchOperation()
