package org.azbuilder.executor.plugin.tfoutput.configuration;

import com.amazonaws.auth.AWSCredentials;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.regions.Regions;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import org.azbuilder.executor.plugin.tfoutput.TerraformOutput;
import org.azbuilder.executor.plugin.tfoutput.TerraformOutputPathService;
import org.azbuilder.executor.plugin.tfoutput.aws.AwsTerraformOutputImpl;
import org.azbuilder.executor.plugin.tfoutput.aws.AwsTerraformOutputProperties;
import org.azbuilder.executor.plugin.tfoutput.azure.AzureTerraformOutputImpl;
import org.azbuilder.executor.plugin.tfoutput.azure.AzureTerraformOutputProperties;
import org.azbuilder.executor.plugin.tfoutput.local.LocalTerraformOutputImpl;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({
        AzureTerraformOutputProperties.class
})
@ConditionalOnMissingBean(TerraformOutput.class)
public class TerraformOutputAutoConfiguration {

    @Bean
    public TerraformOutput terraformOutput(TerraformOutputProperties terraformOutputProperties, AzureTerraformOutputProperties azureTerraformOutputProperties, AwsTerraformOutputProperties awsTerraformOutputProperties, TerraformOutputPathService terraformOutputPathService) {
        TerraformOutput terraformOutput = null;

        if (terraformOutputProperties != null)
            switch (terraformOutputProperties.getType()) {
                case AzureTerraformOutputImpl:
                    BlobServiceClient blobServiceClient = new BlobServiceClientBuilder()
                            .connectionString(
                                    String.format("DefaultEndpointsProtocol=https;AccountName=%s;AccountKey=%s;EndpointSuffix=core.windows.net",
                                            azureTerraformOutputProperties.getAccountName(),
                                            azureTerraformOutputProperties.getAccountKey())
                            ).buildClient();

                    terraformOutput = AzureTerraformOutputImpl.builder()
                            .blobServiceClient(blobServiceClient)
                            .terraformOutputPathService(terraformOutputPathService)
                            .build();
                    break;
                case AwsTerraformOutputImpl:
                    AWSCredentials credentials = new BasicAWSCredentials(
                            awsTerraformOutputProperties.getAccessKey(),
                            awsTerraformOutputProperties.getSecretKey()
                    );

                    AmazonS3 s3client = AmazonS3ClientBuilder
                            .standard()
                            .withCredentials(new AWSStaticCredentialsProvider(credentials))
                            .withRegion(Regions.fromName(awsTerraformOutputProperties.getRegion()))
                            .build();

                    terraformOutput = AwsTerraformOutputImpl.builder()
                            .s3client(s3client)
                            .bucketName(awsTerraformOutputProperties.getBucketName())
                            .terraformOutputPathService(terraformOutputPathService)
                            .build();
                    break;
                default:
                    terraformOutput = new LocalTerraformOutputImpl();
            }
        else
            terraformOutput = new LocalTerraformOutputImpl();
        return terraformOutput;
    }
}
