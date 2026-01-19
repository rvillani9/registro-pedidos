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
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.Calendar;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.EventReminder;
import com.google.api.services.gmail.GmailScopes;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.*;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Arrays;
import java.util.List;

@Service
@Slf4j
public class GoogleCalendarService {

    private static final String APPLICATION_NAME = "Ingreso Pedidos Calendar";
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    // Incluir TODOS los scopes necesarios (Gmail + Calendar)
    private static final List<String> SCOPES = Arrays.asList(
            CalendarScopes.CALENDAR,
            GmailScopes.GMAIL_READONLY,
            GmailScopes.GMAIL_SEND,
            GmailScopes.GMAIL_MODIFY
    );
    private static final String CREDENTIALS_FILE_PATH = "credentials.json";

    private Calendar calendarService;

    /**
     * Obtiene las credenciales de Google Calendar
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
     * Inicializa el servicio de Google Calendar
     */
    public Calendar getCalendarService() throws Exception {
        if (calendarService == null) {
            final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            calendarService = new Calendar.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                    .setApplicationName(APPLICATION_NAME)
                    .build();
        }
        return calendarService;
    }

    /**
     * Crea un evento de entrega en Google Calendar
     */
    public String crearEventoEntrega(String numeroPedido, LocalDate fechaEntrega, String lugarEntrega, String detalles) throws Exception {
        Calendar service = getCalendarService();

        Event event = new Event()
                .setSummary("Entrega Pedido: " + numeroPedido)
                .setLocation(lugarEntrega)
                .setDescription(detalles);

        // Fecha y hora del evento
        DateTime startDateTime = new DateTime(
                java.util.Date.from(fechaEntrega.atStartOfDay(ZoneId.systemDefault()).toInstant())
        );
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("America/Argentina/Buenos_Aires");
        event.setStart(start);

        DateTime endDateTime = new DateTime(
                java.util.Date.from(fechaEntrega.atTime(23, 59).atZone(ZoneId.systemDefault()).toInstant())
        );
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("America/Argentina/Buenos_Aires");
        event.setEnd(end);

        // Recordatorio 48 horas antes
        EventReminder[] reminderOverrides = new EventReminder[] {
                new EventReminder().setMethod("email").setMinutes(48 * 60), // 48 horas
                new EventReminder().setMethod("popup").setMinutes(24 * 60)  // 24 horas
        };
        Event.Reminders reminders = new Event.Reminders()
                .setUseDefault(false)
                .setOverrides(Arrays.asList(reminderOverrides));
        event.setReminders(reminders);

        String calendarId = "primary";
        event = service.events().insert(calendarId, event).execute();

        log.info("Evento creado en Calendar: {}", event.getHtmlLink());
        return event.getId();
    }

    /**
     * Actualiza un evento existente
     */
    public void actualizarEvento(String eventId, String nuevaDescripcion) throws Exception {
        Calendar service = getCalendarService();
        Event event = service.events().get("primary", eventId).execute();

        String descripcionActual = event.getDescription() != null ? event.getDescription() : "";
        event.setDescription(descripcionActual + "\n" + nuevaDescripcion);

        service.events().update("primary", eventId, event).execute();
        log.info("Evento actualizado: {}", eventId);
    }

    /**
     * Elimina un evento
     */
    public void eliminarEvento(String eventId) throws Exception {
        Calendar service = getCalendarService();
        service.events().delete("primary", eventId).execute();
        log.info("Evento eliminado: {}", eventId);
    }
}

