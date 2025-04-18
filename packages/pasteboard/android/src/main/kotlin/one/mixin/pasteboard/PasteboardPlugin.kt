package one.mixin.pasteboard


import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import androidx.core.content.FileProvider
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel
import io.flutter.plugin.common.MethodChannel.MethodCallHandler
import io.flutter.plugin.common.MethodChannel.Result
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.util.UUID
import java.util.concurrent.Executors

/** PasteboardPlugin */
class PasteboardPlugin : FlutterPlugin, MethodCallHandler {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private lateinit var context: Context
    private lateinit var channel: MethodChannel

    private val executor = Executors.newSingleThreadExecutor()

    override fun onAttachedToEngine(flutterPluginBinding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(flutterPluginBinding.binaryMessenger, "pasteboard")
        channel.setMethodCallHandler(this)
        context = flutterPluginBinding.applicationContext
    }

    override fun onMethodCall(call: MethodCall, result: Result) {
        val manager = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val cr = context.contentResolver
        val first = manager.primaryClip?.getItemAt(0)
        when (call.method) {
            "image" -> {
                executor.run {
                    val uri = first?.uri ?: return@run result.success(null)
                    val mime = cr.getType(uri)
                    if (mime == null || !mime.startsWith("image")) return@run result.success(null)
                    val bytes =
                        cr.openInputStream(uri)?.readBytes() ?: return@run result.success(null)
                    result.success(bytes)
                }
            }

            "files" -> {
                manager.primaryClip?.run {
                    if (itemCount == 0) result.success(null)
                    val files: MutableList<String> = mutableListOf()
                    for (i in 0 until itemCount) {
                        getItemAt(i).uri?.let {
                            files.add(it.toString())
                        }
                    }
                    result.success(files)
                }
            }

            "html" -> result.success(first?.htmlText)
            "writeFiles" -> {
                val args = call.arguments<List<String>>() ?: return result.error(
                    "NoArgs",
                    "Missing Arguments",
                    null,
                )
                val clip: ClipData? = null
                for (i in args) {
                    val uri = Uri.parse(i)
                    clip ?: ClipData.newUri(cr, "files", uri)
                    clip?.addItem(ClipData.Item(uri))
                }
                clip?.let {
                    manager.setPrimaryClip(it)
                }
                result.success(null)
            }

            "writeImage" -> {
                val image = call.arguments<ByteArray>() ?: return result.error(
                    "NoArgs",
                    "Missing Arguments",
                    null,
                )

                val name = UUID.randomUUID().toString() + ".png"
                val file = File(context.cacheDir, name)

                executor.execute {
                    try {
                        val bitmap = BitmapFactory.decodeByteArray(image, 0, image.size)
                        val out = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)

                        FileOutputStream(file).use {
                            out.writeTo(it)
                        }

                        val uri = FileProvider.getUriForFile(
                            context, "${context.packageName}.provider", file
                        )
                        val clip = ClipData.newUri(cr, "image.png", uri)
                        manager.setPrimaryClip(clip)
                        result.success(null)
                    } catch (e: Exception) {
                        result.error("Error", "Failed to write image", e.message)
                    }
                }
            }

            else -> result.notImplemented()
        }
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
    }
}
