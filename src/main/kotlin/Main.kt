import java.io.*
import java.util.*
import kotlin.concurrent.thread
import kotlin.system.exitProcess

fun String.encodeBase64() = String(Base64.getEncoder().encode(this.toByteArray())!!, Charsets.UTF_8)

/*
How to do a full oauth login (doesn't work right now):

1. Request auth CODE: Open the following line in the browser (and sign into studodev tenant):
 https://login.microsoftonline.com/common/oauth2/v2.0/authorize?client_id=4db28b70-a5eb-43de-b1f7-4796a3f93875&response_type=code&redirect_uri=https://login.microsoftonline.com/common/oauth2/nativeclient&response_mode=query&scope=offline_access%20https%3A%2F%2Foutlook.office.com%2FIMAP.AccessAsUser.All%20https%3A%2F%2Foutlook.office.com%2FSMTP.Send

2. Complete auth in browser and copy query parameter `code` from url

3. Request token with code (replace xxxxxxxxxxxxxxxxxxxxx with `code` from above)
curl --location --request POST 'https://login.microsoftonline.com/225b1136-6ecb-46ed-8ef1-2a6ba3ada1a9/oauth2/v2.0/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'client_id=4db28b70-a5eb-43de-b1f7-4796a3f93875' \
--data-urlencode 'scope=https://graph.microsoft.com/.default' \
--data-urlencode 'redirect_uri=https://login.microsoftonline.com/common/oauth2/nativeclient' \
--data-urlencode 'grant_type=authorization_code' \
--data-urlencode 'code=xxxxxxxxxxxxxxxxxxxxx'

Example response:
{
    "token_type": "Bearer",
    "scope": "profile openid email https://outlook.office.com/IMAP.AccessAsUser.All https://outlook.office.com/SMTP.Send https://graph.microsoft.com/.default",
    "expires_in": 4369,
    "ext_expires_in": 4369,
    "access_token": "eyxxxxxxx"
}

4. paste eyxxxxxxx from response into this file line 50. Replace also imap username on line 49 if needed.

5. run main()
 */

fun main() {
    val process = ProcessBuilder("openssl", "s_client", "-connect", "outlook.office365.com:993", "-crlf")
        .directory(File("."))
        .redirectErrorStream(true) // Get stderr too
        .redirectOutput(ProcessBuilder.Redirect.PIPE)
        .redirectError(ProcessBuilder.Redirect.PIPE)
        .start()

    val outputWriter = process.outputWriter()

    val imapUsername = "ffactory@studodev.onmicrosoft.com"
    val accessToken = "eyxxxxxxx"

    fun write(str: String) {
        println(">> $str")
        outputWriter.write(str)
        outputWriter.flush()
    }

    val waitThread = thread {
        Thread.sleep(1000)
        write("C01 CAPABILITY\n")
        Thread.sleep(1000)
        val encoded = "user=${imapUsername}\u0001auth=Bearer $accessToken\u0001\u0001".encodeBase64().replace("\n", "")
        write("A01 AUTHENTICATE XOAUTH2 $encoded\n")
        Thread.sleep(3000)
    }

    val reader = BufferedReader(InputStreamReader(process.inputStream))

    reader.use {
        reader.lines().forEachOrdered { line ->
            println(line)
        }
    }

    process.waitFor() // Wait until the process exited successfully or was destroyed by the waitThread
    waitThread.join()

    exitProcess(0)
}