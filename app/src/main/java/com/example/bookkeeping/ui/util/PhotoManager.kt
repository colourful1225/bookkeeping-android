package com.example.bookkeeping.ui.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import androidx.core.content.FileProvider
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 照片管理工具类
 */
object PhotoManager {
    
    /**
     * 生成临时照片文件 URI（用于拍照）
     */
    fun createPhotoUri(context: Context): Pair<Uri, File> {
        val storageDir = context.cacheDir
        val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val imageFile = File.createTempFile("PHOTO_$timeStamp", ".jpg", storageDir)
        
        val photoUri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.fileprovider",
            imageFile
        )
        
        return Pair(photoUri, imageFile)
    }
    
    /**
     * 从相册选择照片后，保存到应用缓存目录
     */
    fun savePhotoFromUri(context: Context, sourceUri: Uri): Uri? {
        return try {
            val inputStream = context.contentResolver.openInputStream(sourceUri) ?: return null
            val storageDir = context.cacheDir
            val timeStamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
            val imageFile = File.createTempFile("PHOTO_$timeStamp", ".jpg", storageDir)
            
            inputStream.use { input ->
                imageFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            
            Uri.fromFile(imageFile)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * 删除照片文件
     */
    fun deletePhoto(uri: Uri?) {
        if (uri == null) return
        try {
            uri.path?.let { File(it).delete() }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
    
    /**
     * 检查照片是否存在
     */
    fun photoExists(uri: Uri?): Boolean {
        if (uri == null) return false
        return try {
            uri.path?.let { File(it).exists() } ?: false
        } catch (e: Exception) {
            false
        }
    }
}
