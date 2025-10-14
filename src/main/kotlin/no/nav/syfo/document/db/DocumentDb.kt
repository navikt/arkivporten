package no.nav.syfo.document.db

import java.sql.ResultSet
import java.util.UUID
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.document.api.v1.DocumentType

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

    fun getById(id: Long): DocumentDAO? {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        SELECT *
                        FROM document
                        WHERE id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    preparedStatement.setLong(1, id)
                    val resultSet = preparedStatement.executeQuery()
                    if (resultSet.next()) {
                        resultSet.toDocumentDAO()
                    } else {
                        null
                    }
                }
        }
    }

    fun getByLinkId(linkId: UUID): DocumentDAO? {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        SELECT *
                        FROM document
                        WHERE link_id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    preparedStatement.setObject(1, linkId)
                    val resultSet = preparedStatement.executeQuery()
                    if (resultSet.next()) {
                        resultSet.toDocumentDAO()
                    } else {
                        null
                    }
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

fun ResultSet.toDocumentDAO(): DocumentDAO =
    DocumentDAO(
        id = getLong("id"),
        linkId = getObject("link_id") as UUID,
        documentId = getObject("document_id") as UUID,
        type = DocumentType.valueOf(getString("type")),
        content = getBytes("content"),
        contentType = getString("content_type"),
        orgnumber = getString("orgnumber"),
        messageTitle = getString("message_title"),
        messageSummary = getString("message_summary"),
        status = DocumentStatus.valueOf(getString("status")),
        isRead = getBoolean("is_read"),
        messageId = getObject("message_id") as UUID?,
        created = getTimestamp("created")?.toLocalDateTime(),
    )

class DocumentGeneratedIDException(message: String) : RuntimeException(message)
