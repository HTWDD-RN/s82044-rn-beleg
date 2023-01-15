import java.io.File
import java.lang.Exception
import java.nio.ByteBuffer
import java.nio.file.Paths
import java.util.*
import java.util.zip.CRC32
import kotlin.random.Random
import kotlin.system.measureNanoTime
import kotlin.system.measureTimeMillis

fun longToBytes(x: ULong): ByteArray {
    val buffer: ByteBuffer = ByteBuffer.allocate(java.lang.Long.BYTES)
    buffer.putLong(x.toLong())
    return buffer.array()
}

// todo: replace numberToBuffer methods with ByteArray.setIntAt, ...
fun intToBytes(i: UInt): ByteArray =
    ByteBuffer.allocate(Int.SIZE_BYTES).putInt(i.toInt()).array()

fun shortToBytes(i: Short): ByteArray =
    ByteBuffer.allocate(Short.SIZE_BYTES).putShort(i).array()

fun bytesToInt(bytes: ByteArray): Int =
    ByteBuffer.wrap(bytes).int

fun bytesToShort(bytes: ByteArray): Short =
    ByteBuffer.wrap(bytes).short

fun bytesToLong(bytes: ByteArray): Long =
    ByteBuffer.wrap(bytes).long

class FileTransfer : FT {
    private val host: String
    private val socket: Socket
    private val fileName: String
    private val arq: String
    private val dir: String

    constructor(host: String, socket: Socket, fileName: String, arq: String) {
        this.host = host
        this.socket = socket
        this.fileName = fileName
        this.arq = arq

        if(this.arq.lowercase(Locale.getDefault()) != "sw") throw Exception("only sw protocol is supported")

        this.dir = ""
    }

    constructor(socket: Socket, dir: String) {
        this.socket = socket
        this.dir = dir

        this.host = ""
        this.fileName = ""
        this.arq =""
    }

    override fun file_req(): Boolean {
        //val logger = Logger.getAnonymousLogger()

        val fileContent = File(fileName).readBytes()

        // cast to short se we really have a short
        val sessionId = Random.nextBits(16).toShort()


        val client = SWClient(socket, sessionId.toInt())

        client.debug("connected to ${this.host}")

        val start = "Start"
        val fileSize: ULong = File("./$fileName").length().toULong()
        val filenameLength = fileName.length.toULong()

        val packageInfo =
            start.toByteArray() + longToBytes(fileSize) + shortToBytes(filenameLength.toShort()) + fileName.toByteArray()

        val crcChecksumCalc = CRC32()
        crcChecksumCalc.update(packageInfo)
        val crcchecksum = crcChecksumCalc.value.toUInt()



        // send first package and wait for ack
        client.debug("sending start package")
        client.data_req(packageInfo + intToBytes(crcchecksum), 0, false /* last pkg */)
        client.debug("start package sent. now sending data")


        // send file to server
        val amountOfPackages = (Math.ceil(fileSize.toDouble() / 1400.0)).toLong()
        //logger.info("sending file in ${amountOfPackages} packages")

        var avgBytesPerSecond = 0.0
        var avgBytesPerSecondCount = 0

        val sendTimeMs = measureTimeMillis /* measure time file sending takes */ {
            for (i in 0 until amountOfPackages) {
                val position = i * 1400
                val bytesToRead = Math.min(fileSize.toLong() - (position), 1400)
                val bytes = fileContent.copyOfRange(position.toInt(), position.toInt() + bytesToRead.toInt())

                // send file part package
                val elapsedNs = measureNanoTime {
                    if (!client.data_req(bytes, 0, false)) {
                        println("error sending packet (after 10 retries) -> aborting")
                        return false
                    }
                }

                // calculate speed if bytes.size != 0
                if (bytes.isNotEmpty()) {
                    val bytesPerSecond =
                        bytes.size.toDouble() / (elapsedNs.toDouble() / (1000 * 1000 * 1000).toDouble())

                    avgBytesPerSecond += bytesPerSecond
                    avgBytesPerSecondCount++

                    progressBar(
                        "",
                        "${(bytesPerSecond / 1000).toInt()} KB/s",
                        (i.toDouble() / (amountOfPackages - 1).toDouble())
                    )
                } else progressBar("", "", (i.toDouble() / (amountOfPackages - 1).toDouble()))
            }
        }

        println("Average speed: ${((avgBytesPerSecond / avgBytesPerSecondCount) / 1000).toInt()} KB/s")
        println("File transfer took: ${(Math.round(sendTimeMs / 10.0) / 100.0)}s")

        client.debug("calculating CRC for whole file")
        val crcChecksumCalc2 = CRC32()
        crcChecksumCalc2.update(fileContent)
        val fileChecksum = crcChecksumCalc2.value.toUInt()
        client.debug("CRC for file = ${fileChecksum}")

        // send last data package with crc to server
        client.data_req(intToBytes(fileChecksum), 0, true /* last pkg */)

        println("File successfully sent")

        return true
    }

    override fun file_init(): Boolean {
        val server = SWServer(socket)
        println("waiting for packets")
        try {
            server.data_ind_req()
        } catch (e: Exception) {
            e.printStackTrace()
            return false
        }

        var data = byteArrayOf()
        while (server.sizeToReceive > data.size.toULong()) {
            data += server.data_ind_req()

            println("sizeToReceive: ${server.sizeToReceive} | size: ${data.size}")
        }

        println("sending crc package")
        server.data_ind_req()

        println("data size received: ${data.size}")


        var file = Paths.get(server.fileName).toFile()

        var i = 0
        while (file.exists()) {
            i += 1
            file = Paths.get("${server.fileName}${i}").toFile()
        }

        if (!file.createNewFile()) {
            return false
        }

        println(file.absolutePath)

        file.writeBytes(data)

        println("got file and wrote it to disk")


        return true
    }

}