import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import java.io.File
import java.io.FileFilter
import java.io.FileWriter
import java.util.*

const val rootDirPath = "D:/下载/"

@Service
class CheckServiceImpl : CheckService {
    val logger: Logger = LoggerFactory.getLogger(javaClass)

    override fun startCheckLostImgFromLocal(classNameForImgSum: HashMap<String, Int>): CheckLocalImgRes {
        val rootDir = File(rootDirPath)
        if (!rootDir.exists()) {
            rootDir.mkdirs()
        }

        val checkLocalImgRes = CheckLocalImgRes()

        val allClassDir = rootDir.listFiles()
        for (classDir in allClassDir) {
            if (classDir.isDirectory) {
                val lostImg = LostImg()
                lostImg.className = classDir.name

                if (classNameForImgSum.containsKey(lostImg.className)) {
                    val imgSum = classNameForImgSum[lostImg.className]
                    for (i in 1..imgSum!!) {
                        if (!File("${classDir.absolutePath}/$i.png").exists()) {
                            lostImg.lostImgs.add(i)
                        }
                    }
                }
                if (!lostImg.lostImgs.isEmpty()) {
                    checkLocalImgRes.add(lostImg)
                }
            }
        }

        checkLocalImgRes.sortWith(Comparator { o1: LostImg, o2: LostImg ->
            getIndexFromClassName(o1.className) - getIndexFromClassName(o2.className)
        })

        val lostImgOutputFile = File("D:/下载失败的图片.txt")
        if (lostImgOutputFile.exists()) {
            lostImgOutputFile.delete()
        }
        lostImgOutputFile.createNewFile()
        val fileWriter = FileWriter(lostImgOutputFile, true)
        val errStrBuilder = StringBuilder()
        for (one in checkLocalImgRes) {
            errStrBuilder.append(one.className)
                    .append(" : ")
                    .append(one.lostImgs.toString())
                    .append("\r\n")
        }
        fileWriter.write(errStrBuilder.toString())
        try {
            fileWriter.close()
        } catch (e: Exception) {
            logger.warn("close fileWriter fail:${e.message}")
        }

        return checkLocalImgRes
    }

    fun getIndexFromClassName(className: String): Int {
        val length = className.length
        for (i in length downTo 1) {
            className.substring(0, i).toIntOrNull()?.let {
                return@getIndexFromClassName it
            }
        }
        return -1
    }
}