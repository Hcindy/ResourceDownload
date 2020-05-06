data class DownloadTask(var id: String,
                        var name: String,
                        var url: String,
                        var startTime: String,
                        var endTime: String,
                        var usedTime: Long,
                        var video: Boolean,
                        var imageSum: Int,
                        var imageErr: ArrayList<String>) {
    constructor(id: String, url: String, startTime: String) : this(id, "", url,
            startTime, "", 0,
            false, 0, arrayListOf())
}