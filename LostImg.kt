data class LostImg(var className: String,
                   var lostImgs: ArrayList<Int>) {
    constructor() : this("", arrayListOf())
}