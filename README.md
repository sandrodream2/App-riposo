# App Riposo

Applicazione Android (Kotlin) con interfaccia moderna a fondo nero (AMOLED) che consente di programmare il blocco di applicazioni scelte dall'utente in una fascia oraria definita, per favorire il riposo dal troppo utilizzo dello smartphone.

## Funzionalità

- **Tema nero AMOLED** con card scure, pulsanti arrotondati e accenti viola/corallo.
- Selezione delle app da bloccare con **ricerca**, conteggio app selezionate e azioni **blocca tutte / deseleziona tutto**.
- Programmazione della fascia oraria (es. 22:00 – 07:00), con supporto per fasce a cavallo di mezzanotte.
- Attivazione/disattivazione del blocco programmato con un solo pulsante.
- **Blocco immediato**: blocca le app selezionate per 15 minuti, 30 minuti o 1 ora.
- **Modalità telefono semplice (dumbphone)**: durante la fascia oraria trasforma lo smartphone in un semplice telefono, lasciando disponibili solo telefono, messaggi, orologio e impostazioni.
- **Schermata di blocco** con messaggio, orario di fine blocco e **countdown** in tempo reale.
- Servizio di accessibilità interno all'app (nessun permesso privacy-richiesto oltre alla visibilità delle finestre).

## Come funziona

L'app usa un `AccessibilityService` (`AppBlockService`) che ascolta gli eventi `TYPE_WINDOW_STATE_CHANGED`: quando l'utente apre un'app bloccata durante la fascia oraria programmata (o durante un blocco immediato, o in modalità telefono semplice), viene lanciata `BlockActivity` a schermo intero con countdown. Quando la fascia oraria termina, il blocco si interrompe automaticamente.

In **modalità telefono semplice** tutte le app vengono bloccate tranne telefono, messaggi, orologio, impostazioni e App Riposo stessa.

## Installazione

1. Aprire il progetto in Android Studio (minimum SDK 24, target SDK 34).
2. Compilare con `./gradlew assembleDebug`.
3. Installare l'APK sullo smartphone.
4. Al primo avvio, attivare il servizio **Blocco app Riposo** in *Impostazioni → Accessibilità*.

## Permessi

- **Servizio di accessibilità**: necessario per rilevare quale app viene aperta e mostrare la schermata di blocco. I dati restano sul dispositivo.
- `QUERY_ALL_PACKAGES`: usato solo per elencare le app installate selezionabili dall'utente.
- `POST_NOTIFICATIONS`: riservato per notifiche future.
