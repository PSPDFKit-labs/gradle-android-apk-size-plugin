package com.vanniktech.android.apk.size

import groovy.transform.stc.ClosureParams
import groovy.transform.stc.SimpleType
import org.gradle.api.DefaultTask
import com.android.build.gradle.api.BaseVariantOutput
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

class ApkSizeTask extends DefaultTask {

    def BaseVariantOutput apkOutput
    @OutputFile File outputFile

    @TaskAction def sizeApk() {
        final File apk = apkOutput.outputFile
        final int apkSize = apk.length()

        def nativeLibrarySizes = [:]    // architecture / size pairs
        final File nativeLibraryDir = new File("${project.buildDir}/intermediates/bundles/${apkOutput.dirName}/jni")
        if (nativeLibraryDir.exists()) {
            nativeLibraryDir.eachDirRecurse { dir ->
                def size = 0
                dir.eachFileRecurse { file ->
                    size += file.length()
                }

                nativeLibrarySizes[dir.name] = size
            }
        }

        final def fileEnding = apk.name[-3..-1].toUpperCase(Locale.US)
        withStyledOutput { out ->
            out.warn("Total ${fileEnding} Size in ${apk.name} in bytes: ${apkSize} (${ApkSizeTools.convertBytesToMegaBytes(apkSize)} mb)")
            nativeLibrarySizes.each { name, size ->
                out.warn("Total ${name} Architecture Size in bytes: ${size} (${ApkSizeTools.convertBytesToMegaBytes(size)} mb)")
            }
        }

        if (outputFile != null) {
            outputFile.parentFile.mkdirs()
            outputFile.createNewFile()
            outputFile.withOutputStream { stream ->
                def appendableStream = new PrintStream(stream)
                appendableStream.println("name,bytes,kilobytes,megabytes")

                final String bytes = String.valueOf(apkSize)
                final String kiloBytes = ApkSizeTools.convertBytesToKiloBytes(apkSize)
                final String megaBytes = ApkSizeTools.convertBytesToMegaBytes(apkSize)

                appendableStream.println(fileEnding + "," + bytes + "," + kiloBytes + "," + megaBytes)
                nativeLibrarySizes.each { name, size ->
                    final String kbSize = ApkSizeTools.convertBytesToKiloBytes(size)
                    final String mbSize = ApkSizeTools.convertBytesToMegaBytes(size)
                    appendableStream.println(name + "," + size + "," + kbSize + "," + mbSize)
                }
            }
        }
    }

    def withStyledOutput(@ClosureParams(value = SimpleType, options = ['org.gradle.api.logging.Logger']) Closure closure) {
        closure(getLogger())
    }
}
