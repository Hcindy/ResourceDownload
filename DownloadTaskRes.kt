data class DownloadTaskRes(var usedTimeSum: Long,
                           var detail: List<DownloadTask>?) {
    constructor() : this(0, null)
}