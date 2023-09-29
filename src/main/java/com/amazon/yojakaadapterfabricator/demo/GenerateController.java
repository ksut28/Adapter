package com.amazon.yojakaadapterfabricator.demo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mashape.unirest.http.HttpResponse;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@CrossOrigin
@RestController
@RequestMapping("/api/generate")
public class GenerateController {

    private static final String KEY = "$2a$10$BaPEk.47Lz5.Xykt3Mc66OfeWmqSO1x04S8EGfy6j6HDO0EirSpTa";

    @PostMapping
    public String generateRepository(@RequestParam("swaggerFile") MultipartFile swaggerFile) throws IOException, UnirestException {
        String uploadDir = "/Users/kusshara/Downloads/demo/src/main/java/com/amazon/yojakaadapterfabricator/demo/";

        createSwaggerJson(swaggerFile, uploadDir);
        String content = new String(Files.readAllBytes(Paths.get(uploadDir + "swagger.yml")));

        // Upload json to server to create a url
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("https://api.jsonbin.io/v3/b")
                .header("Content-Type", "application/json")
                .header("X-Master-key", KEY)
                .header("X-Bin-Private", "false")
                .body(content)
                .asString();

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode jsonNode = objectMapper.readTree(response.getBody());

        // Extract the "id" value from the "metadata" object
        String id = jsonNode.get("metadata").get("id").asText();

        return returnUrlOfSwaggerRepo(id);
    }

    private void createSwaggerJson(final MultipartFile swaggerFile, final String uploadDir) throws IOException {
        // Create the directory if it doesn't exist
        File directory = new File(uploadDir);
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Construct the file path where the uploaded JSON file will be saved
        String filePath = uploadDir + "swagger.yml";

        // Copy the file to the specified location
        Path destination = new File(filePath).toPath();
        Files.copy(swaggerFile.getInputStream(), destination, StandardCopyOption.REPLACE_EXISTING);
    }

    private String returnUrlOfSwaggerRepo(final String id) throws UnirestException {
        Unirest.setTimeouts(0, 0);
        HttpResponse<String> response = Unirest.post("http://api.openapi-generator.tech/api/gen/clients/spring")
                .header("content-type", "application/json")
                .body("{\n" +
                        "    \"openAPIUrl\": \"https://api.jsonbin.io/v3/b/" + id + "?meta=false\",\n" +
                        "    \"options\": {\n" +
                        "        \"packageName\": \"pet_store\",\n" +
                        "        \"modelPackage\": \"com.amazon.yojakaadapterfabricator.model\",\n" +
                        "        \"apiPackage\": \"com.amazon.yojakaadapterfabricator.api\",\n" +
                        "        \"groupId\": \"com.amazon.yojaka-adapter-fabricator\",\n" +
                        "        \"artifactId\": \"demo-adapter\"\n" +
                        "    }\n" +
                        "}")
                .asString();

        return response.getBody();
    }
}