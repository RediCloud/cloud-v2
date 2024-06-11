package dev.redicloud.repository.template.file

import dev.redicloud.api.database.IDatabaseConnection
import dev.redicloud.api.template.file.ICloudFileTemplate
import dev.redicloud.api.template.file.ICloudFileTemplateRepository
import dev.redicloud.database.DatabaseConnection
import dev.redicloud.packets.PacketManager
import dev.redicloud.repository.cache.CachedDatabaseBucketRepository
import dev.redicloud.repository.node.NodeRepository
import dev.redicloud.api.service.ServiceType
import java.util.*
import kotlin.time.Duration.Companion.minutes

abstract class AbstractFileTemplateRepository(
    private val databaseConnection: IDatabaseConnection,
    private val nodeRepository: NodeRepository,
    packetManager: PacketManager
) : CachedDatabaseBucketRepository<ICloudFileTemplate, FileTemplate>(
    databaseConnection,
    "file-template",
    ICloudFileTemplate::class,
    FileTemplate::class,
    5.minutes,
    packetManager,
    ServiceType.NODE,
), ICloudFileTemplateRepository {

    override suspend fun getTemplate(uniqueId: UUID): FileTemplate? {
        return get(uniqueId.toString())
    }

    override suspend fun getTemplate(displayName: String): FileTemplate? {
        return getTemplates().firstOrNull { it.displayName.lowercase() == displayName.lowercase() }
    }

    override suspend fun getTemplate(name: String, prefix: String): FileTemplate? {
        return getTemplates().firstOrNull { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    override suspend fun existsTemplate(uniqueId: UUID, prefix: String): Boolean {
        return exists(uniqueId.toString())
    }

    override suspend fun existsTemplate(displayName: String): Boolean {
        return getTemplates().any { it.displayName.lowercase() == displayName.lowercase() }
    }

    override suspend fun existsTemplate(name: String, prefix: String): Boolean {
        return getTemplates().any { it.name.lowercase() == name.lowercase() && it.prefix.lowercase() == prefix.lowercase() }
    }

    override suspend fun deleteTemplate(uniqueId: UUID): Boolean {
        val templates = getTemplates()
        val template =
            templates.firstOrNull { it.uniqueId == uniqueId } ?: throw Exception("Template $uniqueId not found!")
        templates.filter { it.inherited.contains(template.uniqueId) }.forEach {
            it.inherited.remove(template.uniqueId)
            updateTemplate(it)
        }
        val state = delete(uniqueId.toString())
        if (template.folder.exists() && template.folder.isDirectory) {
            template.folder.deleteRecursively()
            if (template.prefixFolder.listFiles()?.isEmpty() == true) template.prefixFolder.deleteRecursively()
            nodeRepository.getConnectedNodes().forEach {
                if (it.serviceId == databaseConnection.serviceId) return@forEach
                pushTemplates(it.serviceId)
            }
        }
        return state
    }

    override suspend fun createTemplate(template: ICloudFileTemplate): FileTemplate {
        set(template.uniqueId.toString(), template as FileTemplate)
        if (!template.folder.exists()) template.folder.mkdirs()
        nodeRepository.getConnectedNodes().forEach {
            if (it.serviceId == databaseConnection.serviceId) return@forEach
            pushTemplates(it.serviceId)
        }
        return template
    }

    override suspend fun getTemplates(): List<FileTemplate> = getAll()

    override suspend fun collectTemplates(
        vararg templates: ICloudFileTemplate
    ): List<FileTemplate> {
        val collectedTemplates = mutableListOf<FileTemplate>()
        templates.forEach { fileTemplate ->
            val template = getTemplate(fileTemplate.uniqueId)
                ?: throw Exception("Template ${fileTemplate.uniqueId} not found!")
            collectedTemplates.add(template)
            collectedTemplates.addAll(
                collectTemplates(
                    *template.inherited.mapNotNull { getTemplate(it) }.toTypedArray()
                )
            )
        }
        return collectedTemplates
    }

}