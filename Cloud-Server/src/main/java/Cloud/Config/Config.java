package Cloud.Config;

import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class Config {

    @Bean
    public OpenApiCustomizer fileUploadCustomizer() {
        return openApi -> {
            if (openApi.getPaths() != null) {
                openApi.getPaths().forEach((path, pathItem) -> {
                    if (path.equals("/api/files/upload")) {
                        pathItem.readOperations().forEach(operation -> {
                            operation.requestBody(new io.swagger.v3.oas.models.parameters.RequestBody()
                                .content(new Content().addMediaType("multipart/form-data",
                                    new MediaType().schema(new Schema<>().type("object")
                                        .addProperties("username", new StringSchema())
                                        .addProperties("file", new Schema<>().type("string").format("binary"))
                                    )))
                                .required(true));
                        });
                    }
                });
            }
        };
    }
}
