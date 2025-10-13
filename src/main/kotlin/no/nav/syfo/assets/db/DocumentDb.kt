package no.nav.syfo.assets.db

import java.sql.ResultSet
import no.nav.syfo.application.database.DatabaseInterface

class DocumentDb(private val database: DatabaseInterface) {
    fun insert(documentDAO: DocumentDAO): Long {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                       INSERT INTO document(
                       document_id, orgnumber, type, content, asset_status)
                       VALUES (?, ?, ?, ?, ?) RETURNING id;
                    """.trimIndent()
                ).use { preparedStatement ->
                    with(documentDAO) {
                        preparedStatement.setObject(1, documentId)
                        preparedStatement.setString(2, orgnumber)
                        preparedStatement.setString(3, type.name)
                        preparedStatement.setBytes(4, content)
                        preparedStatement.setObject(5, status, java.sql.Types.OTHER)
                    }
                    preparedStatement.execute()

                    runCatching { preparedStatement.resultSet.getGeneratedId("id") }.getOrElse {
                        connection.rollback()
                        throw it
                    }
                }.also {
                    connection.commit()
                }
        }
    }
}

private fun ResultSet.getGeneratedId(idColumnLabel: String): Long = this.use {
    val id = if (this.next()) {
        this.getObject(idColumnLabel) as? Long
    } else {
        null
    }

    return id ?: throw DocumentGeneratedIDException(
        "Could not get the generated id."
    )
}

class DocumentGeneratedIDException(message: String) : RuntimeException(message)
