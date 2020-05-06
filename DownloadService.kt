interface DownloadService {
    fun startFromLocal(downloadTasks: List<String>): DownloadTaskRes
}