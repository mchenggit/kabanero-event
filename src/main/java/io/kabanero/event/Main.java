package io.kabanero.event;

import io.kabanero.event.model.AppsodyStack;
import io.kabanero.event.model.Commit;
import io.kabanero.event.model.Content;
import io.kubernetes.client.ApiClient;

import javax.json.bind.Jsonb;
import javax.json.bind.JsonbBuilder;
import javax.net.ssl.HttpsURLConnection;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Base64;

public class Main {

    private static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String args[] ) {
        Consumer<String, String> consumer = ConsumerCreator.createConsumer();

        try {
            logger.info("Starting io.kabanero.event");


            ApiClient apiClient = KubeUtils.getApiClient();

            // disable host name verification for HTTSURLConnection calls
            HttpsURLConnection.setDefaultHostnameVerifier((host, session) -> true);

           while (true) {
               ConsumerRecords<String, String> records = consumer.poll(1000);

               if (records.count() <= 0)
                   continue;

               records.forEach(record -> {
                   logger.info("Record key: " + record.key());
                   logger.info("Record value: " + record.value());
                   logger.info("Record partition: " + record.partition());
                   logger.info("Record offset: " + record.offset());

                   Jsonb jsonb = JsonbBuilder.create();
                   Commit commit = jsonb.fromJson(record.value(), Commit.class);
                   String body = GHUtils.getFile(commit.repository.url, ".appsody-config.yaml");
                   logger.info("Body:\n" + body);

                   Content content = jsonb.fromJson(body, Content.class);
                   String contentStr = new String(Base64.getUrlDecoder().decode(content.getContent()));
                   contentStr = contentStr.substring("stack: ".length());
                   logger.info("Content is " + contentStr);

                   AppsodyStack stack = new AppsodyStack(contentStr);
                   logger.info("user {}, project {}, version {}", stack.getUser(), stack.getProject(), stack.getVersion());

                   // TODO: Validate stack in Jane's repo against the stacks in the collection

                   // Create the pipeline resources and create a new run
                   String namespace = "kabanero";
                   String imageLocation = "docker-registry.default.svc:5000/kabanero/" + commit.repository.name;
                   logger.info("Creating Docker Image Pipeline Resource: name {}, namespace {}, imageLocation {}",
                           "docker-image",
                           namespace,
                           imageLocation);

                   String branch = commit.ref.substring(commit.ref.lastIndexOf("/") + 1);
                   logger.info("Creating Git pipeline resource: name {}, namespace {}, branch {}, repoLocation: {}",
                           "git-source",
                           namespace,
                           branch,
                           commit.repository.gitUrl);

                   logger.info("Running pipeline: name {}, namespace {}, pipelineRef {}, gitResourceRef {}, dockerImageRef {}, serviceAccount {}, timeout {}, triggerType {}",
                           "appsody-manual-pipeline-run",
                           "kabanero",
                           "appsody-build-pipeline",
                           "git-source",
                           "docker-image",
                           "appsody-sa",
                           "1h0m0s",
                           "manual");

                   try {
                       TektonUtils.createModifyDockerImagePipelineResource(apiClient,
                               "docker-image",
                               namespace,
                               imageLocation);
                       TektonUtils.createModifyGitPipelineResource(apiClient,
                               "git-source",
                               namespace,
                               branch,
                               commit.repository.gitUrl);
                       TektonUtils.runPipeline(apiClient,
                               "appsody-manual-pipeline-run",
                               "kabanero",
                               "appsody-build-pipeline",
                               "git-source",
                               "docker-image",
                               "appsody-sa",
                               "1h0m0s",
                               "manual");
                   } catch (Exception e) {
                       e.printStackTrace();
                   }
               });

               consumer.commitAsync();
           }
        }  catch(Throwable t) {
            logger.error("Error in WASController", t);
        }
        // If we get here, something is wrong. Should loop forever
        System.exit(1);
    }
}
