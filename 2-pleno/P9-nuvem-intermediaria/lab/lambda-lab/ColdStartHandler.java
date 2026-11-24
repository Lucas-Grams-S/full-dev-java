import java.net.URI;
import java.time.Instant;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.http.urlconnection.UrlConnectionHttpClient;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.sqs.SqsClient;

/**
 * Local model of a Java Lambda: "init" (static initialization: build the AWS client) + "invoke" (first call).
 * It is NOT Lambda: it measures the JVM + SDK cost on THIS machine, to see what a cold start is made of
 * and what class-data sharing / AOT changes. Usage: java ... ColdStartHandler <endpoint> [warmup-calls]
 */
public class ColdStartHandler {

    // "Init phase": runs once per execution environment, before the first invocation.
    static final long T_JVM_START = java.lang.management.ManagementFactory.getRuntimeMXBean().getStartTime();

    public static void main(String[] args) throws Exception {
        long mainStart = System.currentTimeMillis();
        String endpoint = args[0];
        int warm = args.length > 1 ? Integer.parseInt(args[1]) : 0;

        long t0 = System.nanoTime();
        SqsClient sqs = SqsClient.builder()
                .endpointOverride(URI.create(endpoint))
                .region(Region.US_EAST_1)
                .credentialsProvider(StaticCredentialsProvider.create(AwsBasicCredentials.create("test", "test")))
                .httpClientBuilder(UrlConnectionHttpClient.builder())
                .build();
        long initMs = (System.nanoTime() - t0) / 1_000_000;

        t0 = System.nanoTime();
        sqs.listQueues();                       // "first invocation": TLS-less HTTP call + serialization + first use of every class
        long firstMs = (System.nanoTime() - t0) / 1_000_000;

        long warmUs = 0;
        for (int i = 0; i < warm; i++) {
            t0 = System.nanoTime();
            sqs.listQueues();
            warmUs = (System.nanoTime() - t0) / 1_000;
        }
        long jvmBootMs = mainStart - T_JVM_START;
        System.out.printf("jvm-boot=%dms  sdk-client-init=%dms  first-call=%dms  total-to-first-response=%dms%s%n",
                jvmBootMs, initMs, firstMs, jvmBootMs + initMs + firstMs,
                warm > 0 ? "  warm-call=" + warmUs + "us" : "");
    }
}
