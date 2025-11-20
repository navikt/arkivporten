package no.nav.syfo.document.db

import java.sql.ResultSet
import java.util.UUID
import no.nav.syfo.application.database.DatabaseInterface
import no.nav.syfo.document.api.v1.DocumentType
import java.sql.Types

class DocumentDAO(private val database: DatabaseInterface) {
    fun insert(documentEntity: DocumentEntity): Long {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        INSERT INTO document(document_id,
                                             type,
                                             content,
                                             content_type,
                                             title,
                                             summary,
                                             link_id,
                                             status,
                                             dialog_id)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                        RETURNING id;
                        """.trimIndent()
                ).use { preparedStatement ->
                    with(documentEntity) {
                        preparedStatement.setObject(1, documentId)
                        preparedStatement.setObject(2, type, Types.OTHER)
                        preparedStatement.setBytes(3, content)
                        preparedStatement.setString(4, contentType)
                        preparedStatement.setString(5, title)
                        preparedStatement.setString(6, summary)
                        preparedStatement.setObject(7, linkId)
                        preparedStatement.setObject(8, status, Types.OTHER)
                        preparedStatement.setLong(9, dialog.id!!)
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

    fun update(documentEntity: DocumentEntity) {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        UPDATE document
                        SET dialog_id = ?,
                            status     = ?,
                            is_read    = ?
                        WHERE id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    with(documentEntity) {
                        require(id != null) { "Document ID cannot be null when updating a document." }
                        preparedStatement.setLong(1, dialog.id!!)
                        preparedStatement.setObject(2, status, Types.OTHER)
                        preparedStatement.setBoolean(3, isRead)
                        preparedStatement.setLong(4, id)
                    }
                    preparedStatement.execute()
                }
            if (documentEntity.dialog.dialogportenId != null) {
                connection.prepareStatement(
                    """
                        UPDATE dialogporten_dialog
                        SET dialog_id = ?
                        WHERE id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    with(documentEntity) {
                        preparedStatement.setObject(1, dialog.dialogportenId)
                        preparedStatement.setLong(2, dialog.id!!)
                    }
                    preparedStatement.execute()
                }
            }
            connection.commit()
        }
    }

    fun getById(id: Long): DocumentEntity? {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        SELECT doc.*, dialog.id as dialog_pk_id, dialog.title as dialog_title, dialog.summary as dialog_summary, 
                               dialog.dialog_id as dialog_uuid, dialog.fnr, dialog.org_number, dialog.created as dialog_created, 
                               dialog.updated as dialog_updated
                        FROM document doc
                        LEFT JOIN dialogporten_dialog dialog ON doc.dialog_id = dialog.id
                        WHERE doc.id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    preparedStatement.setLong(1, id)
                    val resultSet = preparedStatement.executeQuery()
                    if (resultSet.next()) {
                        resultSet.toDocumentEntity()
                    } else {
                        null
                    }
                }
        }
    }

    fun getByLinkId(linkId: UUID): DocumentEntity? {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        SELECT doc.*, dialog.id as dialog_pk_id, dialog.title as dialog_title, dialog.summary as dialog_summary, 
                               dialog.dialog_id as dialog_uuid, dialog.fnr, dialog.org_number, dialog.created as dialog_created, 
                               dialog.updated as dialog_updated
                        FROM document doc
                        LEFT JOIN dialogporten_dialog dialog ON doc.dialog_id = dialog.id
                        WHERE doc.link_id = ?
                        """.trimIndent()
                ).use { preparedStatement ->
                    preparedStatement.setObject(1, linkId)
                    val resultSet = preparedStatement.executeQuery()
                    if (resultSet.next()) {
                        resultSet.toDocumentEntity()
                    } else {
                        null
                    }
                }
        }
    }

    fun getDocumentsByStatus(status: DocumentStatus): List<DocumentEntity> {
        return database.connection.use { connection ->
            connection
                .prepareStatement(
                    """
                        SELECT doc.*, dialog.id as dialog_pk_id, dialog.title as dialog_title, dialog.summary as dialog_summary, 
                               dialog.dialog_id as dialog_uuid, dialog.fnr, dialog.org_number, dialog.created as dialog_created, 
                               dialog.updated as dialog_updated
                        FROM document doc
                        LEFT JOIN dialogporten_dialog dialog ON doc.dialog_id = dialog.id
                        WHERE doc.status = ?
                        order by doc.created
                        LIMIT 100
                        """.trimIndent()
                ).use { preparedStatement ->
                    preparedStatement.setObject(1, status, Types.OTHER)
                    val resultSet = preparedStatement.executeQuery()
                    val documents = mutableListOf<DocumentEntity>()
                    while (resultSet.next()) {
                        documents.add(resultSet.toDocumentEntity())
                    }
                    documents
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

fun ResultSet.toDocumentEntity(): DocumentEntity =
    DocumentEntity(
        id = getLong("id"),
        linkId = getObject("link_id") as UUID,
        documentId = getObject("document_id") as UUID,
        type = DocumentType.valueOf(getString("type")),
        content = getBytes("content"),
        contentType = getString("content_type"),
        title = getString("title"),
        summary = getString("summary"),
        status = DocumentStatus.valueOf(getString("status")),
        isRead = getBoolean("is_read"),
        dialog = DialogEntity(
            id = getLong("dialog_pk_id"),
            title = getString("dialog_title"),
            summary = getString("dialog_summary"),
            fnr = getString("fnr"),
            orgNumber = getString("org_number"),
            dialogportenId = getObject("dialog_uuid") as UUID?,
            created = getTimestamp("dialog_created")?.toInstant(),
            updated = getTimestamp("dialog_updated")?.toInstant(),
        ),
        created = getTimestamp("created")?.toInstant(),
    )

class DocumentGeneratedIDException(message: String) : RuntimeException(message)
