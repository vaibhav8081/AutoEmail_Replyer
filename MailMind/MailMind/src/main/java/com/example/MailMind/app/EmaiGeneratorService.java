package com.example.MailMind.app;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Map;

@Service
public class EmaiGeneratorService {

    private final WebClient webClient;

   // @Value("$(gemini.api.url)")
    private String geminiApiUrl="https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent?key=";

    //@Value("$(gemini.api.key)")
    private String geminiApiKey="write your own generated key";

    public EmaiGeneratorService(WebClient.Builder webClientBuilder) {
        this.webClient = webClientBuilder.build();
    }

    public String generateEmailReply(EmailRequest emailRequest){
        // Build the  prompt
        String prompt=buildPrompt(emailRequest);

        // Craft a Request
        Map<String,Object> requestBody=Map.of(
                "contents",new Object[]{
                        Map.of("parts",new Object[]{
                                Map.of("text",prompt)
                        })
                }
        );
        // Do Request and get response
        String response =webClient.post().
                uri(geminiApiUrl+geminiApiKey).
                header("Content-Type","application/json").
                bodyValue(requestBody).
                retrieve()
                .bodyToMono(String.class)
                .block();

        // Extract response and Return response
        return extractResponseContent(response);
    }

    private String extractResponseContent(String response) {
        try{
            ObjectMapper mapper=new ObjectMapper();
            JsonNode rootNode=mapper.readTree(response);
            return rootNode.path("candidates")
                    .get(0)
                    .path("content")
                    .path("parts")
                    .get(0)
                    .path("text")
                    .asText();
        }catch (Exception e){
            return "Error processing request "+e.getMessage();
        }
    }

    private String buildPrompt(EmailRequest emailRequest) {
        StringBuilder prompt=new StringBuilder();
        prompt.append("Generate a email reply for the following email content. Please don't generate a subject line only reply in output which we send in mail no else sentence ");
        if(emailRequest.getTone()!=null && !emailRequest.getTone().isEmpty()){
            prompt.append("Use a ").append(emailRequest.getTone()).append(" tone.");
        }
        prompt.append("\n Original email: \n").append(emailRequest.getEmailContent());
        return prompt.toString();
    }
}
