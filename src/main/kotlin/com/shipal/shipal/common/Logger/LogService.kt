package com.shipal.shipal.common.Logger

import org.springframework.stereotype.Component
import java.io.File
import java.io.FileWriter
import java.io.PrintWriter
import java.text.SimpleDateFormat
import java.util.Date

@Component
class LogService {
    private val lock = Any()
    private val baseDir = File(System.getProperty("user.dir"), "SystemLog")
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd")
    private val timeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS")

    fun logMessage(message: String){
        try
        {
            val now = Date()
            val year = SimpleDateFormat("yyyy").format(now)
            val month = SimpleDateFormat("MM").format(now)

            // SystemLog/yyyy/MM 폴더 생성
            val logDir = File(baseDir, "$year/$month")
            logDir.mkdirs()

            // 파일 이름: yyyy-MM-dd.txt
            val logFile = File(logDir, "${dateFormat.format(now)}.txt")

            synchronized(lock){
                PrintWriter(FileWriter(logFile, true)).use { writer ->
                    writer.println("[${timeFormat.format(now)}]\t$message")
                }
            }
        }
        catch (ex : Exception){}
    }

    fun consoleMessage(message: String){
        synchronized(lock){
            println("\u001B[32m[INFO] ${timeFormat.format(Date())}: $message\u001B[0m")
        }
    }

    fun consoleWarning(message: String) {
        synchronized(lock) {
            println("\u001B[31m[WARN] ${timeFormat.format(Date())}: $message\u001B[0m")
        }
    }
}