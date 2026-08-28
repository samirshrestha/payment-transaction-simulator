package host.tools

import host.domain.TransactionRequest
import host.domain.TransactionResponse
import host.domain.TransactionType
import host.transport.DevTls
import host.wire.RequestCodec
import host.wire.ResponseCodec
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.IOException
import java.net.SocketTimeoutException
import javax.net.ssl.SSLSocket

private const val DEFAULT_HOST = "localhost"
private const val DEFAULT_PORT = 8583

/**
 * Interactive CLI for exercising a running Host server without a Terminal -- see #39. Reuses
 * [RequestCodec]/[ResponseCodec] directly so it can never drift from the real wire format.
 */
internal sealed interface Command {
    data class Transaction(
        val type: TransactionType,
        val pan: String,
        val amount: Long,
        val stan: String,
        val advice: Boolean,
        val repeat: Boolean,
    ) : Command

    object Quit : Command
}

/**
 * Parses one REPL line: `TYPE PAN AMOUNT STAN [ADVICE] [REPEAT]` (amount in minor units/cents),
 * or `quit`. Both flags together sends a retried Financial Advice (see `host/CONTEXT.md`'s Repeat
 * Indicator entry -- valid because a Financial Advice retry sets both). Throws
 * [IllegalArgumentException] with a message meant to be shown directly to the operator.
 */
internal fun parseCommand(line: String): Command {
    val tokens = line.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
    require(tokens.isNotEmpty()) { "Empty command" }
    if (tokens.size == 1 && tokens[0].equals("quit", ignoreCase = true)) return Command.Quit

    require(tokens.size in 4..6) {
        "Expected 'TYPE PAN AMOUNT STAN [ADVICE] [REPEAT]' or 'quit', got '$line'"
    }

    val type = try {
        TransactionType.valueOf(tokens[0].uppercase())
    } catch (_: IllegalArgumentException) {
        throw IllegalArgumentException(
            "Unknown transaction type '${tokens[0]}' -- expected one of ${TransactionType.entries.joinToString()}"
        )
    }
    val pan = tokens[1]
    val amount = tokens[2].toLongOrNull()
        ?: throw IllegalArgumentException("Amount must be a whole number of minor units (cents), got '${tokens[2]}'")
    val stan = tokens[3]

    val flagTokens = tokens.drop(4).map { it.uppercase() }
    require(flagTokens.all { it == "ADVICE" || it == "REPEAT" }) {
        "Unknown flag(s) '${tokens.drop(4).joinToString(" ")}' -- expected ADVICE and/or REPEAT"
    }
    require(flagTokens.size == flagTokens.toSet().size) { "Duplicate flag in '$line'" }

    return Command.Transaction(
        type = type,
        pan = pan,
        amount = amount,
        stan = stan,
        advice = "ADVICE" in flagTokens,
        repeat = "REPEAT" in flagTokens,
    )
}

/** Decline takes priority: a wire-corrupted or misbehaving server could in principle set both. */
internal fun describe(response: TransactionResponse): String = when {
    response.declineReason != null -> "DECLINED (${response.declineReason})"
    response.advice -> "ACKNOWLEDGED (advice)"
    else -> "APPROVED"
}

fun main(args: Array<String>) {
    val host = args.getOrElse(0) { DEFAULT_HOST }
    val portArg = args.getOrElse(1) { DEFAULT_PORT.toString() }
    val port = portArg.toIntOrNull()
    if (port == null) {
        println("Port must be a whole number, got '$portArg'")
        return
    }

    println("Manual Test Client -- connecting to $host:$port ...")
    val socket = try {
        DevTls.clientSocketFactory().createSocket(host, port) as SSLSocket
    } catch (e: Exception) {
        println("Could not connect to $host:$port -- is the Host server running (./gradlew host:run)? (${e.message})")
        return
    }

    socket.use {
        val output = DataOutputStream(socket.getOutputStream())
        val input = DataInputStream(socket.getInputStream())

        println("Connected. Demo Accounts (see README): PAN 4111111111111111 ($1,000.00 balance / $500.00 limit),")
        println("                                        PAN 5500000000000004 ($5.00 balance / $5.00 limit).")
        println("Enter: TYPE PAN AMOUNT STAN [ADVICE] [REPEAT]   e.g. FINANCIAL 4111111111111111 10000 1")
        println("TYPE is one of ${TransactionType.entries.joinToString()}; AMOUNT is in cents. Type 'quit' to exit.")

        System.`in`.bufferedReader().use { reader ->
            while (true) {
                print("> ")
                val line = reader.readLine() ?: break
                if (line.isBlank()) continue

                val command = try {
                    parseCommand(line)
                } catch (e: IllegalArgumentException) {
                    println("Error: ${e.message}")
                    continue
                }

                when (command) {
                    is Command.Quit -> break
                    is Command.Transaction -> {
                        val request = TransactionRequest(
                            type = command.type,
                            stan = command.stan,
                            pan = command.pan,
                            amount = command.amount,
                            advice = command.advice,
                            repeat = command.repeat,
                        )
                        try {
                            val requestBytes = RequestCodec.encode(request)
                            output.writeShort(requestBytes.size)
                            output.write(requestBytes)
                            output.flush()

                            val responseLength = input.readUnsignedShort()
                            val responseBytes = ByteArray(responseLength)
                            input.readFully(responseBytes)
                            println(describe(ResponseCodec.decode(responseBytes)))
                        } catch (e: IllegalArgumentException) {
                            println("Error: ${e.message}")
                        } catch (e: SocketTimeoutException) {
                            println(
                                "Host closed the connection after its idle timeout -- restart the client to continue. (${e.message})"
                            )
                            break
                        } catch (e: IOException) {
                            println(
                                "Connection closed by Host -- this can happen after a request that violates its " +
                                    "recorded state (e.g. a Reversal referencing an unrecorded STAN, or a PAN/amount " +
                                    "mismatch); check the Host server's own console for the exact reason. Restart the " +
                                    "client to continue. (${e.message})"
                            )
                            break
                        }
                    }
                }
            }
        }
    }
    println("Disconnected.")
}
