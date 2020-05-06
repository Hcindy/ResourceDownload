interface CheckService {
    fun startCheckLostImgFromLocal(classNameForImgSum: HashMap<String, Int>): CheckLocalImgRes
}