package no.nav.syfo.document.db

import java.sql.ResultSet
import no.nav.syfo.application.database.DatabaseInterface

class DocumentDb(private val database: DatabaseInterface) {
    fun insert(documentDAO: DocumentDAO): Long {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        INSERT INTO document(document_id,
                                             type,
                                             content,
                                             content_type,
                                             orgnumber,
                                             message_title,
                                             message_summary,
                                             link_id,
                                             status)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id;
                        """.trimIndent()
                ).use { preparedStatement ->
                    with(documentDAO) {
                        preparedStatement.setObject(1, documentId)
                        preparedStatement.setObject(2, type.name)
                        preparedStatement.setBytes(3, content)
                        preparedStatement.setString(4, contentType)
                        preparedStatement.setString(5, orgnumber)
                        preparedStatement.setString(6, messageTitle)
                        preparedStatement.setString(7, messageSummary)
                        preparedStatement.setObject(8, linkId)
                        preparedStatement.setObject(9, status, java.sql.Types.OTHER)
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

    fun update(documentDAO: DocumentDAO): Boolean {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        UPDATE document
                        SET message_id = ?,
                            status     = ?,
                            is_read    = ?
                        WHERE id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    with(documentDAO) {
                        require(id != null) { "Document ID cannot be null when updating a document." }
                        preparedStatement.setObject(1, messageId)
                        preparedStatement.setObject(2, status, java.sql.Types.OTHER)
                        preparedStatement.setBoolean(3, isRead)
                        preparedStatement.setLong(4, id)
                    }
                    preparedStatement.execute()
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
