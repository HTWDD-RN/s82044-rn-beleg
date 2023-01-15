import java.lang.StringBuilder
import java.lang.Thread.sleep

const val PROGRESS_BAR_WIDTH = 25

fun progressBar(
    preText: String,
    postText: String,
    progress: Double
) {
    val emptyChar = '-'
    val takenChar  = "="
    val leftBorderChar = "["
    val rightBorderChar = "]"
    val remainingProgressChars = progress * PROGRESS_BAR_WIDTH

    val progbar =  StringBuilder(leftBorderChar)
    for (i in 0..(PROGRESS_BAR_WIDTH - 1)) if (remainingProgressChars > i) {
        progbar.append(takenChar)
    } else {
        progbar.append(emptyChar)
    }
    progbar.append(rightBorderChar)

    val percent = (progress * 100).toInt()

    print("\r${if(preText.isNotBlank()) "$preText " else ""}$progbar ${(progress * 100).toInt()}%${if(postText.isNotBlank()) " $postText" else ""}")
    if(percent >= 100) print("\n")
}


fun main (){
    val max = 20
    for (i in 0..max){
        //progressBar("Uploading","", i, max)
        sleep(500)
    }
}