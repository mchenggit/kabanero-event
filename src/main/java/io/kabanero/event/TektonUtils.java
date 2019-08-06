package io.kabanero.event;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.kubernetes.client.ApiClient;
import io.kubernetes.client.ApiException;
import io.kubernetes.client.Configuration;
import io.kubernetes.client.apis.CoreV1Api;
import io.kubernetes.client.apis.CustomObjectsApi;
import io.kubernetes.client.util.Config;
import io.kubernetes.client.models.V1DeleteOptions;
import io.kubernetes.client.models.V1Secret;

public class TektonUtils {
    private static final Logger logger = LoggerFactory.getLogger(TektonUtils.class);

    public static void test(ApiClient apiClient) {
    try {
        // crate resources
       createModifyDockerImagePipelineResource(apiClient, "docker-image", "kabanero", "docker-registry.default.svc:5000/kabanero/java-microprofile");

        createModifyGitPipelineResource(apiClient, "git-source", "kabanero", "master", "https://github.com/dacleyra/appsody-hello-world/");

        // Create a new run
        runPipeline(apiClient, "appsody-manual-pipeline-run", "kabanero",
         "appsody-build-pipeline", "git-source", "docker-image", "appsody-sa", "1h0m0s", "manual");
     
    } catch(Exception ex) {
       ex.printStackTrace();
    }
    }


    // image pipeline resource
    private static String pipelineResourceImage = 
        "{" +
        "    \"apiVersion\": \"tekton.dev/v1alpha1\", " +
        "    \"kind\": \"PipelineResource\"," +
        "    \"metadata\": {" +
        "        \"name\": \"{{__NAME__}}\","+
        "        \"namespace\": \"{{__NAMESPACE__}}\"" +
        "    }," +
        "    \"spec\": {" +
        "        \"params\": ["+
        "            {" + 
        "                \"name\": \"url\","+
        "                \"value\": \"{{__IMAGE_LOCATION__}}\" " +
        "            }"+
        "        ], "+
        "        \"type\": \"image\""+
        "   }" +
        "}";

    public static void createModifyDockerImagePipelineResource(ApiClient apiClient, String name,
         String namespace, String imageLocation) throws Exception {
        logger.info("createModifyDockerImagePipelineResource name: {} namespace: {}, imageLocation: {}", name, namespace, imageLocation);

        String group = KubeUtils.TEKTON_GROUP;
        String version = KubeUtils.TEKTON_VERSION;
        String plural  = KubeUtils.TEKTON_PIPELINE_RESOURCE_PLURAL;
        Map resource = getExistingPipelineResource(apiClient, namespace, name);
        if (resource == null ) {
            // create it 
            String jsonBody = pipelineResourceImage.replace("{{__NAME__}}", name).
                         replace("{{__NAMESPACE__}}", namespace).
                         replace("{{__IMAGE_LOCATION__}}", imageLocation);
            logger.info("createModifyDockerImagePiprlineResource Creating resource with json body: {}", jsonBody);
            KubeUtils.createResource(apiClient, group, version, plural, namespace, jsonBody, name);
        } else {
            logger.info("createModifyDockerImagePipelineRessource: resource {}/{} alredy exists", namespace, name);
        }
    }


   private static String pipelineResourceGit =
    "{" +
    "    \"apiVersion\": \"tekton.dev/v1alpha1\","+
    "    \"kind\": \"PipelineResource\","+
    "    \"metadata\": {"+
    "        \"name\": \"{{__NAME__}}\","+
    "        \"namespace\": \"{{__NAMESPACE__}}\""+
    "    },"+
    "    \"spec\": {"+
    "        \"params\": ["+
    "            {"+
    "                \"name\": \"revision\","+
    "                \"value\": \"{{__BRANCH__}}\""+
    "            },"+
    "            {"+
    "                \"name\": \"url\","+
    "                \"value\": \"{{__REPO_LOCATION__}}\""+
    "            }"+
    "        ],"+
    "        \"type\": \"git\""+
    "    }"+
    "}";

    public static void createModifyGitPipelineResource(ApiClient apiClient, String name,
         String namespace, String branch, String repoLocation) throws Exception {
        logger.info("createModifyGitPipelineResource name: {} namespace: {}, branch: {}, repoLocation:{}", name, namespace, branch, repoLocation);

        String group = KubeUtils.TEKTON_GROUP;
        String version = KubeUtils.TEKTON_VERSION;
        String plural  = KubeUtils.TEKTON_PIPELINE_RESOURCE_PLURAL;
        Map resource = getExistingPipelineResource(apiClient, namespace, name);
        if (resource == null ) {

            String jsonBody = pipelineResourceGit.replace("{{__NAME__}}", name).
                         replace("{{__NAMESPACE__}}", namespace).
                         replace("{{__BRANCH__}}", branch).
                         replace("{{__REPO_LOCATION__}}", repoLocation);
            logger.info("createModifyGitPiprlineResource Creating resource with json body: {}", jsonBody);
            KubeUtils.createResource(apiClient, group, version, plural, namespace, jsonBody, name);
        } else {
            logger.info("createModifyGitPipelineRessource: resource {}/{} alredy exists", namespace, name);
        }
    }



    private static String pipelineRun =
    "{"+
    "    \"apiVersion\": \"tekton.dev/v1alpha1\","+
    "    \"kind\": \"PipelineRun\","+
    "    \"metadata\": {"+
    "        \"labels\": {"+
    "            \"tekton.dev/pipeline\": \"appsody-build-pipeline\""+
    "        },"+
    "        \"name\": \"{{__NAME__}}\","+
    "        \"namespace\": \"{{__NAMESPACE__}}\""+
    "    },"+
    "    \"spec\": {"+
    "        \"pipelineRef\": {"+
    "            \"name\": \"{{__PIPELINE_REF__}}\""+
    "        },"+
    "        \"resources\": ["+
    "            {"+
    "                \"name\": \"git-source\","+
    "                \"resourceRef\": {"+
    "                    \"name\": \"{{__GIT_RESOURCE_REF__}}\""+
    "                }"+
    "            },"+
    "            {"+
    "                \"name\": \"docker-image\","+
    "                \"resourceRef\": {"+
    "                    \"name\": \"{{__DOCKER_IMAGE_REF__}}\""+
    "                }"+
    "            }"+
    "        ],"+
    "        \"serviceAccount\": \"{{__SERVICE_ACCOUNT__}}\","+
    "        \"timeout\": \"{{__TIMEOUT__}}\","+
    "        \"trigger\": {"+
    "            \"type\": \"{{__TRIGGER_TYPE__}}\""+
    "        }"+
    "    }"+
    "}";

    public static void runPipeline(ApiClient apiClient, String name, String namespace,
         String pipelineRef, String gitResourceRef, String dockerImageRef, String serviceAccount,
         String timeout, String triggerType) throws Exception {
        logger.info("runPipeline name: {} namespace: {}, pipelineRef: {}, gitResourceRef:{}, dockerImageRef: {}, serviceAccount: {}, timeout: {}, triggerType: {}", name, namespace, pipelineRef, gitResourceRef, dockerImageRef, serviceAccount, timeout, triggerType);
    
       
        String group = KubeUtils.TEKTON_GROUP;
        String version = KubeUtils.TEKTON_VERSION;
        String plural = KubeUtils.TEKTON_PIPELINE_RUN_PLURAL;
        Map map = getExistingPipelineRun(apiClient, namespace, name);
        if ( map != null ) {
            // delete the run
            logger.info("runPipeline  deleting existing run: {}/{}", namespace, name);
            KubeUtils.deleteKubeResource(apiClient, namespace, name, group, version, plural);
        }

        String jsonBody = pipelineRun.replace("{{__NAME__}}", name).
                      replace("{{__NAMESPACE__}}", namespace).
                      replace("{{__PIPELINE_REF__}}", pipelineRef).
                      replace("{{__GIT_RESOURCE_REF__}}", gitResourceRef).
                      replace("{{__DOCKER_IMAGE_REF__}}", dockerImageRef).
                      replace("{{__SERVICE_ACCOUNT__}}", serviceAccount).
                      replace("{{__TIMEOUT__}}", timeout).
                      replace("{{__TRIGGER_TYPE__}}", triggerType);

        logger.info("runPiepeline creating resource with json body: {}", jsonBody);
        KubeUtils.createResource(apiClient, group, version, plural, namespace, jsonBody, name);
    }
   

    public static Map getExistingPipelineResource(ApiClient apiClient, String namespace, String name) throws Exception {
        String group = KubeUtils.TEKTON_GROUP;
        String version = KubeUtils.TEKTON_VERSION;
        String plural = KubeUtils.TEKTON_PIPELINE_RESOURCE_PLURAL;
        Map ret = null;
        CustomObjectsApi customApi = new CustomObjectsApi(apiClient);
        try {
            Object obj = customApi.getNamespacedCustomObject(group, version, namespace, plural, name);
            ret = (Map) obj;
        } catch(ApiException ex) {
            logger.error("can't get existing tekton resource {}/{}", namespace, name, ex);
            int code = ex.getCode();
            if ( code != 404) {
                // some other issue than app does not exist
                throw ex;
            }
        }
        return ret;
    }

    public static Map getExistingPipelineRun(ApiClient apiClient, String namespace, String name) throws Exception {
        String group = KubeUtils.TEKTON_GROUP;
        String version = KubeUtils.TEKTON_VERSION;
        String plural = KubeUtils.TEKTON_PIPELINE_RUN_PLURAL;
        Map ret = null;
        CustomObjectsApi customApi = new CustomObjectsApi(apiClient);
        try {
            Object obj = customApi.getNamespacedCustomObject(group, version, namespace, plural, name);
            ret = (Map) obj;
        } catch(ApiException ex) {
            logger.error("can't get existing tekton pipeline run {}/{}", namespace, name, ex);
            int code = ex.getCode();
            if ( code != 404) {
                // some other issue than app does not exist
                throw ex;
            }
        }
        return ret;
    }
 
}
