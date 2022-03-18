package com.example.uploadpictures

import android.content.Context
import android.net.Uri
import top.zibin.luban.Luban
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream

/**
 * @param context 上下文
 * @param outFilePath 输出文件路径,因为android10的关系,所以该路径只可为应用内沙盒路径
 */
fun Uri.copyAndConvert(context: Context, outFilePath: String): File {
    val pfd = context.contentResolver.openFileDescriptor(this, "r")//r代表读操作
    FileInputStream(pfd?.fileDescriptor).use { fis ->
        FileOutputStream(File(outFilePath)).use { fos ->
            fis.copyTo(fos)
        }
    }
    return Luban.with(context).load(outFilePath).get()[0]
}

