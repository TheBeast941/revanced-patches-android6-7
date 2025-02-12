package app.revanced.shared.util.resources

import app.revanced.patcher.data.DomFileEditor
import app.revanced.patcher.data.ResourceContext
import app.revanced.shared.util.FileCopyCompat
import java.nio.file.Files
import java.nio.file.StandardCopyOption

internal object ResourceUtils {
    /**
     * Copy resources from the current class loader to the resource directory.
     * @param sourceResourceDirectory The source resource directory name.
     * @param resources The resources to copy.
     */
    internal fun ResourceContext.copyResources(sourceResourceDirectory: String, vararg resources: ResourceGroup) {
        val classLoader = ResourceUtils.javaClass.classLoader
        val targetResourceDirectory = this["res"]

        for (resourceGroup in resources) {
            resourceGroup.resources.forEach { resource ->
                val resourceFile = "${resourceGroup.resourceDirectoryName}/$resource"
                try {
                    Files.copy(
                        classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")!!,
                        targetResourceDirectory.resolve(resourceFile).toPath(),
                        StandardCopyOption.REPLACE_EXISTING
                    )
                } catch (e: NoSuchMethodError) {
                    FileCopyCompat.copy(
                        classLoader.getResourceAsStream("$sourceResourceDirectory/$resourceFile")!!,
                        targetResourceDirectory.resolve(resourceFile)
                    )
                }
            }
        }
    }

    /**
     * Resource names mapped to their corresponding resource data.
     * @param resourceDirectoryName The name of the directory of the resource.
     * @param resources A list of resource names.
     */
    internal class ResourceGroup(val resourceDirectoryName: String, vararg val resources: String)

    /**
     * Copy resources from the current class loader to the resource directory.
     * @param resourceDirectory The directory of the resource.
     * @param targetResource The target resource.
     * @param elementTag The element to copy.
     */
    internal fun ResourceContext.copyXmlNode(resourceDirectory: String, targetResource: String, elementTag: String) {
        val stringsResourceInputStream = ResourceUtils.javaClass.classLoader.getResourceAsStream("$resourceDirectory/$targetResource")!!

        // Copy nodes from the resources node to the real resource node
        elementTag.copyXmlNode(
            this.xmlEditor[stringsResourceInputStream],
            this.xmlEditor["res/$targetResource"]
        ).close()
    }

    /**
     * Copies the specified node of the source [DomFileEditor] to the target [DomFileEditor].
     * @param source the source [DomFileEditor].
     * @param target the target [DomFileEditor]-
     * @return AutoCloseable that closes the target [DomFileEditor]s.
     */
    internal fun String.copyXmlNode(source: DomFileEditor, target: DomFileEditor): AutoCloseable {
        val hostNodes = source.file.getElementsByTagName(this).item(0).childNodes

        val destinationResourceFile = target.file
        val destinationNode = destinationResourceFile.getElementsByTagName(this).item(0)

        for (index in 0 until hostNodes.length) {
            val node = hostNodes.item(index).cloneNode(true)
            destinationResourceFile.adoptNode(node)
            destinationNode.appendChild(node)
        }

        return AutoCloseable {
            source.close()
            target.close()
        }
    }
}