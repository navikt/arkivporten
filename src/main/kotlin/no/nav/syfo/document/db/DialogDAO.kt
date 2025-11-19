package no.nav.syfo.document.db

import no.nav.syfo.application.database.DatabaseInterface
import java.sql.ResultSet

class DialogDAO(private val database: DatabaseInterface) {
    fun insertDialog(dialogEntity: DialogEntity): DialogEntity {
        val insertStatement =
            """
            INSERT INTO dialogporten_dialog (
                title,
                summary,
                fnr,
                org_number
            ) VALUES (?, ?, ?, ?)
            RETURNING id, title, summary, fnr, org_number, created, updated, dialog_id
            """.trimIndent()

        val connection = database.connection
        connection.use { conn ->
            val preparedStatement = conn.prepareStatement(insertStatement)
            preparedStatement.use { ps ->
                ps.setString(1, dialogEntity.title)
                ps.setString(2, dialogEntity.summary)
                ps.setString(3, dialogEntity.fnr)
                ps.setString(4, dialogEntity.orgNumber)
                ps.setObject(5, dialogEntity.dialogportenId)
                val resultSet = ps.executeQuery()
                if (resultSet.next()) {
                    return resultSet.toDialog()
                } else {
                    throw Exception("Inserting dialog failed, no rows returned.")
                }
            }
        }
    }

    fun getByFnrAndOrgNumber(fnr: String, orgNumber: String): DialogEntity? {
        val query =
            """
            SELECT *
            FROM dialogporten_dialog
            WHERE fnr = ? AND org_number = ?
            """.trimIndent()

        database.connection.use { conn ->
            val preparedStatement = conn.prepareStatement(query)
            preparedStatement.use { ps ->
                ps.setString(1, fnr)
                ps.setString(2, orgNumber)
                val resultSet = ps.executeQuery()
                if (resultSet.next()) {
                    return resultSet.toDialog()
                }
            }
        }
        return null
    }
}

fun ResultSet.toDialog(): DialogEntity =
    DialogEntity(
        id = getLong("id"),
        title = getString("title"),
        summary = getString("summary"),
        fnr = getString("fnr"),
        orgNumber = getString("org_number"),
        created = getTimestamp("created")?.toInstant(),
        updated = getTimestamp("updated")?.toInstant(),
        dialogportenId = getObject("dialog_id", java.util.UUID::class.java)
    )
