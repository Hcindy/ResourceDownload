import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.fasterxml.jackson.module.kotlin.readValue
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicLong

const val rootDirPath = "D:/下载/"

@Service
class DownloadServiceImpl : DownloadService {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun startFromLocal(downloadTasks: List<String>): DownloadTaskRes {
        logger.info("will start ${downloadTasks.size} task")
        val downloadTaskList = CopyOnWriteArrayList<DownloadTask>()
//        val downloadTaskMap = ConcurrentHashMap<String, DownloadTask>()
        val taskFutures = arrayListOf<CompletableFuture<Void>>()

        val executor = Executors.newFixedThreadPool(10)
        for (url in downloadTasks) {
            taskFutures.add(CompletableFuture.runAsync(Runnable {
                // pre
                val startTime = LocalDateTime.now()
                val formatter = DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                val urlSplit = url.split("?")
                val jsonpCallbackName = urlSplit[1].substring(urlSplit[1].lastIndexOf("=") + 1)
                val downloadTask = DownloadTask(urlSplit[0].substring(urlSplit[0].lastIndexOf("/") + 1),
                        url, startTime.format(formatter))
                logger.info("${downloadTask.id} pre ok")
                // get date from jsonp
                val dist = URL(url)
                val reader = BufferedReader(InputStreamReader(dist.openStream(), "UTF-8"))
                val jsonpDataBuilder = StringBuilder()
                var line = reader.readLine()
                while (line != null) {
                    jsonpDataBuilder.append(line)
                    line = reader.readLine()
                }
                reader.close()
                logger.info("${downloadTask.id} read ok")
                val jsonData = jsonpDataBuilder
                        .delete(0, jsonpCallbackName.length + 1)
                        .deleteCharAt(jsonpDataBuilder.length - 1)
                        .toString()
                // convert jsonData
                val personMap: Map<String, Any> = jacksonObjectMapper().readValue(jsonData)
                logger.info("${downloadTask.id} convert jsonData ok")
                // get name
                downloadTask.name = personMap["name"] as String
                logger.info("${downloadTask.id} name ${downloadTask.name} ok")
                // mkdir
                val subDir = File("$rootDirPath${downloadTask.name}")
                if (!subDir.exists()) {
                    subDir.mkdirs()
                }
                logger.info("${downloadTask.id} dir ok")
                // videoUrl
                var content = personMap["resources"] as String
                content = content.substring(content.indexOf("http"))
                val videoUrl = content.substring(0, content.indexOf("\""))
                logger.info("${downloadTask.id} videoUrl $videoUrl ok")
                // download pre
                var connection: HttpURLConnection? = null
                var bis: BufferedInputStream? = null
                var fos: FileOutputStream? = null
                // download video
                try {
                    val fileName = "$rootDirPath${downloadTask.name}/${downloadTask.name}.mp4"
                    if (!File(fileName).exists()) {
                        // build connection
                        connection = URL(videoUrl).openConnection() as HttpURLConnection
                        connection.connect()
                        // get net input stream
                        bis = BufferedInputStream(connection.inputStream)
                        // get file output stream
                        fos = FileOutputStream(fileName)
                        // save file
                        val buf = ByteArray(4096)
                        logger.info("${downloadTask.id} downloading ${downloadTask.name}.mp4")
                        var size = bis.read(buf)
                        while (size != -1) {
                            fos.write(buf, 0, size)
                            size = bis.read(buf)
                        }
                        downloadTask.video = true
                        logger.info("${downloadTask.id} download video ok")
                    } else {
                        downloadTask.video = true
                        logger.info("${downloadTask.id} exist video")
                    }
                } catch (e: Exception) {
                    logger.warn("${downloadTask.id} download video fail")
                } finally {
                    fos?.close()
                    bis?.close()
                    connection?.disconnect()
                }
                // download image
                val imageFlag = "dataimg=\""
                var imageIndex = content.indexOf(imageFlag)
                var page = 0;
                while (imageIndex != -1) {
                    page += 1
                    // find image target
                    content = content.substring(imageIndex + imageFlag.length)
                    val imageUrl = content.substring(0, content.indexOf("\""))
                    logger.info("${downloadTask.id} imageUrl $imageUrl")
                    // downloading
                    try {
                        val fileName = "$rootDirPath${downloadTask.name}/$page.png"
                        if (!File(fileName).exists()) {
                            connection = URL(imageUrl).openConnection() as HttpURLConnection
                            connection.connect()
                            bis = BufferedInputStream(connection.inputStream)
                            fos = FileOutputStream(fileName)
                            val buf = ByteArray(4096)
                            logger.info("${downloadTask.id} downloading $page.png")
                            var size = bis.read(buf)
                            while (size != -1) {
                                fos.write(buf, 0, size)
                                size = bis.read(buf)
                            }
                            logger.info("${downloadTask.id} download $page.png ok")
                        } else {
                            logger.info("${downloadTask.id} exist $page.png")
                        }
                    } catch (e: Exception) {
                        downloadTask.imageErr.add("$page:$imageUrl")
                        logger.warn("${downloadTask.id} download $page.png fail")
                    } finally {
                        fos?.close()
                        bis?.close()
                        connection?.disconnect()
                        downloadTask.imageSum += 1
                    }
                    // next image
                    imageIndex = content.indexOf(imageFlag)
                }
                // calc time
                val endTime = LocalDateTime.now()
                downloadTask.endTime = endTime.format(formatter)
                downloadTask.usedTime = ChronoUnit.SECONDS.between(startTime, endTime)
                // recode res
                downloadTaskList.add(downloadTask)
                // downloadTaskMap[downloadTask.name] = downloadTask
            }, executor))
        }
        // wait all done
        for (tf in taskFutures) {
            try {
                while (!tf.isDone);
            } catch (e: InterruptedException) {
                e.printStackTrace()
            } catch (e: ExecutionException) {
                e.printStackTrace()
            } finally {
                executor.shutdown()
            }
        }
        // build res
        val downloadTaskRes = DownloadTaskRes()
        // calc sum time
        val usedTimeSum = AtomicLong(0)
//        ForkJoinPool(4).submit {
        downloadTaskList.parallelStream().forEach { downloadTask: DownloadTask ->
            downloadTaskRes.usedTimeSum = usedTimeSum.addAndGet(downloadTask.usedTime)
        }
//        }
        // fill detail
        downloadTaskRes.detail = downloadTaskList.toList()
        logger.info("${downloadTasks.size} task end")
        return downloadTaskRes
    }
}
