import java.net.DatagramPacket

class SWClient(socket: Socket?, sessionId: Int) : ARQAbst(socket, sessionId) {
    private var transmissionTries = 0

    public var logDebugOutput = false

    public fun debug(message: String) {
        if(!logDebugOutput) return
        logger.info(message)
    }

    /**
     * Sender: Request the transmission of a data packet
     * @param hlData DataPacket from Higher Layer
     * @param hlData DataPacket from Higher Layer
     * @param hlSize Size of the DataPacket
     * @return true implies a correct ACK is received
     */
    override fun data_req(hlData: ByteArray, hlSize: Int, lastTransmission: Boolean): Boolean {
        this.debug("sending packet ${this.pNr}")

        val generatedPacket = this.generateDataPacket(hlData, hlSize)

        this.debug("this.sessionId: ${this.sessionID}")

        this.socket.sendPacket(generatedPacket)
        if (!this.waitForAck(this.pNr)) {
            if (this.transmissionTries >= 10) return false
            this.transmissionTries = this.transmissionTries + 1

            // retry
            return data_req(hlData, hlSize, lastTransmission)
        }
        this.pNr = (this.pNr + 1) % 256
        this.transmissionTries = 0

        this.debug("----------------[ package sent ]----------------")

        return true
    }

    /**
     * Sender: Wait until RTO for the ACK-packet, checks sessionID and pNr
     * @param packetNr ACK-Number we are waiting for
     * @return true if correct ACK is received in time
     */
    override fun waitForAck(packetNr: Int): Boolean {
        try {
            // resend timeout
            this.socket.setTimeout(500)
            val pkg = this.socket.receivePacket()

            this.debug("got answer for $packetNr")

            val sessionId = this.getSessionID(pkg)
            val nr = this.getPacketNr(pkg)

            if (this.sessionID != sessionId || packetNr != nr) {
                this.debug("doesn't match sessionId ${this.sessionID} got $sessionId  | packetNr: $packetNr got $nr")
                return false
            }
            return true
        } catch (e: java.lang.Exception) {
            this.debug("failed to confirm if package was received")
            this.debug(e.toString())
            return false
        }
    }


    override fun getSessionID(packet: DatagramPacket?): Int {
        return bytesToShort(packet!!.data.copyOfRange(0, 2)).toInt()
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
    override fun generateDataPacket(sendData: ByteArray, dataSize: Int): ByteArray {
        return shortToBytes(this.sessionID.toShort()) + byteArrayOf(this.pNr.toUByte().toByte()) + sendData
    }

    /**
     * Receiver/Server: Request to receive a data packet (first packet is start)
     * @param values Optional data for the Sender/Client
     * @return DataPacket without SW-Header
     * @throws TimeoutException Timeout of Socket
     */
    override fun data_ind_req(vararg values: Int): ByteArray {
        TODO("Not yet implemented")
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
        TODO("Not yet implemented")
    }

    /**
     * Receiver: generates and sends ACK accordingly to protocol definition Session ID + PacketNr + (CRC)
     * @param nr
     */
    override fun sendAck(nr: Int) {
        TODO("Not yet implemented")
    }

    /**
     * Receiver: Checks for start packet
     * @param packet UDP
     * @return true, if packet includes phrase "Start"
     */
    override fun checkStart(packet: DatagramPacket?): Boolean {
        TODO("Not yet implemented")
    }

     override fun closeConnection() {
        TODO("Not yet implemented")
    }
}