# App Riposo

Applicazione Android (Kotlin) che consente di programmare il blocco di applicazioni scelte dall'utente in una fascia oraria definita, per favorire il riposo dal troppo utilizzo dello smartphone.

## Funzionalità

- Selezione delle app da bloccare (lista app installate con checkbox).
- Programmazione della fascia oraria (es. 22:00 – 07:00), con supporto per fasce a cavallo di mezzanotte.
- Attivazione/disattivazione del blocco programmato con un solo pulsante.
- Quando la fascia oraria è attiva, aprendo un'app bloccata viene mostrata una schermata di blocco che invita al riposo.
- Servizio di accessibilità interno all'app (nessun permesso privacy-richiesto oltre alla visibilità delle finestre).

## Come funziona

L'app usa un `AccessibilityService` (`AppBlockService`) che ascolta gli eventi `TYPE_WINDOW_STATE_CHANGED`: quando l'utente apre un'app nella lista bloccati durante la fascia oraria programmata, viene lanciata `BlockActivity` a schermo intero. Quando la fascia oraria termina, il blocco si interrompe automaticamente.

## Installazione

1. Aprire il progetto in Android Studio (minimum SDK 24, target SDK 34).
2. Compilare con `./gradlew assembleDebug`.
3. Installare l'APK sullo smartphone.
4. Al primo avvio, attivare il servizio **Blocco app Riposo** in *Impostazioni → Accessibilità*.

## Permessi

- **Servizio di accessibilità**: necessario per rilevare quale app viene aperta e mostrare la schermata di blocco. I dati restano sul dispositivo.
- `QUERY_ALL_PACKAGES`: usato solo per elencare le app installate selezionabili dall'utente.
- `POST_NOTIFICATIONS`: riservato per notifiche future.
