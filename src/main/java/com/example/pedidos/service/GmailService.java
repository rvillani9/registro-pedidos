package com.example.pedidos.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.Paths;
import java.util.*;

@Service
@Slf4j
public class GmailService {

    private static final String APPLICATION_NAME = "Ingreso Pedidos";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final List<String> SCOPES = Arrays.asList(
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
    );
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private Gmail gmailService;

    /**
     * Obtiene las credenciales de Gmail
     */
    private Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        InputStream in = new FileInputStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Archivo de credenciales no encontrado: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }

    /**
     * Inicializa el servicio de Gmail
     */
    public Gmail getGmailService() throws Exception {
        if (gmailService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            gmailService = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return gmailService;
    }

    /**
     * Lee emails no leídos con asunto específico
     */
    public List<Message> leerEmailsPedidos(String query) throws Exception {
        Gmail service = getGmailService();
        List<Message> messages = new ArrayList<>();

        ListMessagesResponse response = service.users().messages()
                .list("me")
                .setQ(query)
                .execute();

        if (response.getMessages() != null) {
            for (Message message : response.getMessages()) {
                Message fullMessage = service.users().messages()
                        .get("me", message.getId())
                        .setFormat("full")
                        .execute();
                messages.add(fullMessage);
            }
        }

        return messages;
    }

    /**
     * Marca un email como leído
     */
    public void marcarComoLeido(String messageId) throws Exception {
        Gmail service = getGmailService();
        ModifyMessageRequest mods = new ModifyMessageRequest()
                .setRemoveLabelIds(Arrays.asList("UNREAD"));
        service.users().messages().modify("me", messageId, mods).execute();
        log.info("Email marcado como leído: {}", messageId);
    }

    /**
     * Envía un email
     */
    public void enviarEmail(String destinatario, String asunto, String cuerpo) throws Exception {
        Gmail service = getGmailService();

        Properties props = new Properties();
        javax.mail.Session session = javax.mail.Session.getDefaultInstance(props, null);

        javax.mail.internet.MimeMessage email = new javax.mail.internet.MimeMessage(session);
        email.setFrom(new javax.mail.internet.InternetAddress("me"));
        email.addRecipient(javax.mail.Message.RecipientType.TO,
                new javax.mail.internet.InternetAddress(destinatario));
        email.setSubject(asunto);
        email.setText(cuerpo);

        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        email.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.getUrlEncoder().encodeToString(bytes);

        Message message = new Message();
        message.setRaw(encodedEmail);

        service.users().messages().send("me", message).execute();
        log.info("Email enviado a: {}", destinatario);
    }

    /**
     * Extrae el cuerpo del email
     */
    public String extraerCuerpoEmail(Message message) {
        try {
            if (message.getPayload().getBody().getData() != null) {
                return new String(Base64.getUrlDecoder().decode(
                        message.getPayload().getBody().getData()));
            } else if (message.getPayload().getParts() != null) {
                for (MessagePart part : message.getPayload().getParts()) {
                    if (part.getMimeType().equals("text/plain") || part.getMimeType().equals("text/html")) {
                        return new String(Base64.getUrlDecoder().decode(part.getBody().getData()));
                    }
                }
            }
        } catch (Exception e) {
            log.error("Error extrayendo cuerpo del email", e);
        }
        return "";
    }

    /**
     * Extrae el asunto del email
     */
    public String extraerAsunto(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(header -> header.getName().equals("Subject"))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }

    /**
     * Extrae el remitente del email
     */
    public String extraerRemitente(Message message) {
        return message.getPayload().getHeaders().stream()
                .filter(header -> header.getName().equals("From"))
                .map(MessagePartHeader::getValue)
                .findFirst()
                .orElse("");
    }
}

