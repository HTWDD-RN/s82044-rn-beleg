import java.net.DatagramPacket
import java.util.zip.CRC32
import kotlin.text.Charsets.UTF_8


class SWServer(socket: Socket?) : ARQAbst(socket) {
    private var transmissionTries = 0
    public var sizeToReceive: ULong = (0).toULong()
    public var dataReceived: ByteArray = byteArrayOf()
    public var fileName = ""
    private var shouldValidateCrc = false

    public var lastAddedAmount = 0


    /**
     * Sender: Request the transmission of a data packet
     * @param hlData DataPacket from Higher Layer
     * @param hlSize Size of the DataPacket
     * @return true implies a correct ACK is received
     */
    override fun data_req(hlData: ByteArray?, hlSize: Int, lastTransmission: Boolean): Boolean {
        throw NotImplementedError("")
    }

    /**
     * Sender: Wait until RTO for the ACK-packet, checks sessionID and pNr
     * @param packetNr ACK-Number we are waiting for
     * @return true if correct ACK is received in time
     */
    override fun waitForAck(packetNr: Int): Boolean {
        throw NotImplementedError("")
    }

    override fun getSessionID(packet: DatagramPacket?): Int {
        val sessionId = bytesToShort(packet!!.data.copyOfRange(0, 2))

        return sessionId.toInt()
    }

    override fun getPacketNr(packet: DatagramPacket?): Int {
        return packet!!.data[2].toUByte().toInt()
    }


    /**
     * Sender: Adds protocol header: sessionID, pNr
     * @param sendData HL-data
     * @param dataSize uses size of HL-data array
     * @return SW-packet
     */
    override fun generateDataPacket(sendData: ByteArray?, dataSize: Int): ByteArray {
        throw NotImplementedError("")
    }

    /**
     * Receiver/Server: Request to receive a data packet (first packet is start)
     * @param values Optional data for the Sender/Client
     * @return DataPacket without SW-Header
     * @throws TimeoutException Timeout of Socket
     */
    override fun data_ind_req(vararg values: Int): ByteArray {
        val pkg = this.socket.receivePacket()
        val sessionId = this.getSessionID(pkg)
        val packetNr = this.getPacketNr(pkg)
        logger.info("got pkg with sessionId=$sessionId packetNr=$packetNr")

        if (this.checkStart(pkg)) {
            this.sessionID = sessionId
            this.sizeToReceive = bytesToLong(pkg.data.copyOfRange(8, 16)).toULong()

            // send acknowledgement packet for start packet so client starts sending file data
            this.sendAck(packetNr)
            return byteArrayOf()
        }

        if (this.sessionID == 0) throw Exception("Start package was not sent by client.")

        if (this.sessionID != sessionId) {
            throw java.lang.Exception("sessionId mismatch got $sessionId but should have been ${this.sessionID}")
        }

        var data: ByteArray
        if ((this.dataReceived.size + (3 + this.MTU)).toULong() < this.sizeToReceive) {
            data = pkg.data.copyOfRange(3, (3 + this.MTU))
            println("copying 1400 bytes")
        } else {
            data = pkg.data.copyOfRange(3, 3 + this.sizeToReceive.toInt() - this.dataReceived.size)
            println("copying ${this.sizeToReceive.toInt() - this.dataReceived.size}bytes")
        }

        println("received already: ${this.dataReceived.size}bytes")
        if ((this.dataReceived.size) >= this.sizeToReceive.toInt()) {
            println("setting crc")
            data = pkg.data.copyOfRange(3, 3 + 4)
            this.backData = bytesToInt(data)
            this.shouldValidateCrc = true

            println("set this.backData to ${this.backData}")

            this.sendAck(packetNr)
            return byteArrayOf()
        }

        //val data = pkg.data.copyOfRange(16, (16 + 1400 / 8))
        this.sendAck(packetNr)

        // skip if we already have this
        if (this.pNr == packetNr) return byteArrayOf()

        this.dataReceived += data


        this.pNr = packetNr

        lastAddedAmount = data.size

        return data
    }

    /**
     * Receiver: Collects the gbn and backdata from the ACK packet -> member variables
     * @param packet
     */
    override fun getAckData(packet: DatagramPacket?) {
        TODO("Not yet implemented")
    }

    /**
     * Receiver: generates ACK accordingly to protocol definition Session ID + PacketNr.
     * @param packetNr to Acknowledge
     * @return the ACK packet
     */
    override fun generateAckPacket(packetNr: Int): ByteArray {
        // is start package
        if (shouldValidateCrc) {
            println("received: ${this.dataReceived.size}")

            val crcChecksumCalc = CRC32()
            crcChecksumCalc.update(this.dataReceived)
            val crcchecksum = crcChecksumCalc.value.toUInt()

//            println("calc crc for: ${this.dataReceived.toString(UTF_8)}")

            println("crcChecksum: ${this.dataReceived} crcFromPackage: ${this.backData}")



            if (crcchecksum != this.backData.toUInt())
                throw java.lang.Exception("file corrupted")


            println("sending crc to client to confirm file sent")

            return shortToBytes(this.sessionID.toShort()) + byteArrayOf((packetNr).toUByte().toByte()) + byteArrayOf((1).toByte()) + intToBytes(
                crcchecksum
            )
        }

        return shortToBytes(this.sessionID.toShort()) + byteArrayOf((packetNr).toUByte().toByte()) + byteArrayOf((1).toByte())
    }

    /**
     * Receiver: generates and sends ACK accordingly to protocol definition Session ID + PacketNr + (CRC)
     * @param nr
     */
    override fun sendAck(nr: Int) {
        this.socket.setTimeout(this.receiveTimeout)
        val pkg = this.generateAckPacket(nr)
        this.socket.sendPacket(pkg)

        logger.info("----------------[ acknowledgement sent ]----------------")
    }

    /**
     * Receiver: Checks for start packet
     * @param packet UDP
     * @return true, if packet includes phrase "Start"
     */
    override fun checkStart(packet: DatagramPacket?): Boolean {
        val packetNr = packet!!.data.copyOfRange(2, 3)[0].toInt()

        println("packetNr $packetNr")

        if (packetNr != 0) return false

        val bytes = packet!!.data.copyOfRange(3, 3 + 5)
        val checkString = bytes.toString(UTF_8)
        println("checkString = $checkString")

        if (checkString != "Start") return false

        println("got start package now checking for crc")

        val start = (16 + 8 + 5 * 8 + 64) / 8
        val fileNameLength = bytesToShort(packet.data.copyOfRange(start, start + 2)).toUShort()

        println("fileNameLength: $fileNameLength")

        val fileName = packet.data.copyOfRange(start + 2, start + 2 + fileNameLength.toInt()).toString(UTF_8)

        this.fileName = fileName

        println("fileName: $fileName")

        val dPWoHeader = packet.data.copyOfRange((16 + 8) / 8, (16 + 8 + 5 * 8 + 64 + 16) / 8 + fileNameLength.toInt())

        val crcChecksumCalc = CRC32()
        crcChecksumCalc.update(dPWoHeader)
        val crcChecksum = crcChecksumCalc.value.toUInt()

        //Startpacket
        //16-Bit Sessionnummer (Wahl per Zufallsgenerator)
        //8-Bit Paketnummer (immer 0)
        //5-Byte Kennung „Start“ als ASCII-Zeichen
        //64-Bit Dateilänge (unsigned integer) (für Dateien > 4 GB)
        //16-Bit (unsigned integer) Länge des Dateinamens (1-255)
        //0-255 Byte Dateiname als String mit Codierung UTF-8 (erlaubte Zeichen: [a-zA-ZäöüßÄÖÜ0-9_\-\.])
        //32-Bit CRC32 über alle Daten des Startpaketes ab der Startkennung (Sessionnummer und Paketnummer werden nicht einbezogen)
        //println("packet size ${packet.data.size}")

        val packageCrcStart = (16 + 8 + 5 * 8 + 64 + 16) / 8 + fileNameLength.toInt()
        // check if crc of package is correct
        val packageCrc = bytesToInt(
            packet.data.copyOfRange(
                packageCrcStart,
                packageCrcStart + 32 / 8
            )
        ).toUInt()

        println("packageCrc: $packageCrc | crcChecksum: $crcChecksum")

        return crcChecksum == packageCrc
    }

    override fun closeConnection() {
        // sadly there is no close function in this.socket
    }
}